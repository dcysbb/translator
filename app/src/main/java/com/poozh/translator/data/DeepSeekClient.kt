package com.poozh.translator.data

import com.poozh.translator.model.AnalysisResult
import com.poozh.translator.model.TextLanguage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class DeepSeekClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    interface ResultCallback {
        fun onSuccess(result: AnalysisResult)
        fun onFailure(message: String)
    }

    /** Optional: surfaced when a transient failure triggers an automatic retry,
     *  so the UI can show "正在重试…" instead of silently re-sending. */
    var retryProgress: ((String) -> Unit)? = null

    fun analyze(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        callback: ResultCallback
    ): Call? {
        val provider = ModelProviders.byId(settings.providerId)
        if (settings.apiKey.isBlank() && provider.requiresApiKey) {
            callback.onFailure("请先在主界面填写 ${provider.name} API Key")
            return null
        }
        if (text.isBlank()) {
            callback.onFailure("没有可翻译的文本")
            return null
        }

        val body = buildRequestJson(text, language, settings.model, provider.supportsJsonMode).toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(settings.baseUrl)
            .header("Content-Type", "application/json")
            .post(body)
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${settings.apiKey}")
                }
            }
            .build()

        // Execute with up to MAX_RETRIES retries for transient failures
        // (timeouts, 429, 5xx, network IO). Auth errors (401/403) and bad
        // requests (400/404) are not retried — they won't fix themselves.
        return executeWithRetry(request, provider, text, callback, attempt = 0)
    }

    private fun executeWithRetry(
        request: Request,
        provider: ModelProviderPreset,
        text: String,
        callback: ResultCallback,
        attempt: Int
    ): Call {
        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Network/timeout errors are transient — retry.
                if (attempt < MAX_RETRIES && !call.isCanceled()) {
                    scheduleRetry(request, provider, text, callback, attempt,
                        "${provider.name} 请求失败（第 ${attempt + 1} 次），正在重试…")
                } else {
                    callback.onFailure(friendlyNetworkMessage(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string().orEmpty()
                    val code = it.code
                    // Retry transient server errors and rate limiting.
                    if (shouldRetryStatus(code) && attempt < MAX_RETRIES && !call.isCanceled()) {
                        scheduleRetry(request, provider, text, callback, attempt,
                            "${provider.name} HTTP $code，正在重试…")
                        return
                    }
                    if (!it.isSuccessful) {
                        callback.onFailure("${provider.name} HTTP $code: ${responseBody.take(240)}")
                        return
                    }
                    runCatching {
                        val content = JSONObject(responseBody)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        AnalysisJsonParser.parse(content, text)
                    }.onSuccess(callback::onSuccess)
                        .onFailure { error ->
                            callback.onFailure(error.message ?: "模型服务返回解析失败")
                        }
                }
            }
        })
        return call
    }

    private fun scheduleRetry(
        request: Request,
        provider: ModelProviderPreset,
        text: String,
        callback: ResultCallback,
        attempt: Int,
        statusMessage: String
    ) {
        // Backoff: 1s, then 2s. The statusMessage is surfaced via a dedicated
        // progress callback if the caller supplied one (see [ProgressCallback]).
        retryProgress?.invoke(statusMessage)
        val delayMs = RETRY_BASE_DELAY_MS * (1L shl attempt)
        Thread {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                return@Thread
            }
            executeWithRetry(request, provider, text, callback, attempt + 1)
        }.apply { isDaemon = true; name = "translate-retry-${attempt}" }.start()
    }

    /** Map raw OkHttp exceptions to user-readable Chinese hints. */
    private fun friendlyNetworkMessage(e: IOException): String {
        val msg = e.message.orEmpty()
        return when {
            "timeout" in msg.lowercase() -> "请求超时，请检查网络或稍后重试"
            "failed to connect" in msg.lowercase() || "unable" in msg.lowercase() ->
                "无法连接模型服务，请检查网络或 Base URL"
            else -> msg.ifBlank { "模型服务请求失败" }
        }
    }

    private fun shouldRetryStatus(code: Int): Boolean = code == 429 || code in 500..599

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAX_RETRIES = 2
        private const val RETRY_BASE_DELAY_MS = 1000L
    }

    private fun buildRequestJson(
        text: String,
        language: TextLanguage,
        model: String,
        supportsJsonMode: Boolean
    ): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", DeepSeekPrompt.systemPrompt(language)))
            .put(JSONObject().put("role", "user").put("content", DeepSeekPrompt.userPrompt(text, language)))

        val json = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("stream", false)
        // Only enforce JSON mode when the provider supports it; otherwise rely
        // on the prompt + tolerant parsing (some endpoints, e.g. local Ollama,
        // reject response_format with a 400).
        if (supportsJsonMode) {
            json.put("response_format", JSONObject().put("type", "json_object"))
        }
        return json
    }
}
