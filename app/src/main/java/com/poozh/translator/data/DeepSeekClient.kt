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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DeepSeekClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Streaming translation callback. All methods are invoked from a background
     * OkHttp thread; callers must marshal onto the UI thread themselves.
     */
    interface ResultCallback {
        /** A partial translation has been extracted from the stream so far. */
        fun onTranslationProgress(partialTranslation: String) {}
        /** The full analysis (translation + grammar notes) is ready. */
        fun onSuccess(result: AnalysisResult)
        /** The request failed terminally. Retry is left to the user ("重译"). */
        fun onFailure(message: String)
    }

    /** A cancellable handle for a translation request (stream + fallback). */
    interface TranslationHandle {
        fun cancel()
    }

    fun analyze(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        callback: ResultCallback
    ): TranslationHandle {
        val provider = ModelProviders.byId(settings.providerId)
        if (settings.apiKey.isBlank() && provider.requiresApiKey) {
            callback.onFailure("请先在主界面填写 ${provider.name} API Key")
            return NoopHandle
        }
        if (text.isBlank()) {
            callback.onFailure("没有可翻译的文本")
            return NoopHandle
        }

        val cancelled = AtomicBoolean(false)
        return startStreaming(text, language, settings, provider, callback, cancelled, allowFallback = true)
    }

    /**
     * Fire the streaming request. [allowFallback] gates whether a clearly-
     * unsupported-stream error triggers a single non-streaming retry.
     */
    private fun startStreaming(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        provider: ModelProviderPreset,
        callback: ResultCallback,
        cancelled: AtomicBoolean,
        allowFallback: Boolean
    ): TranslationHandle {
        val request = buildRequest(text, language, settings, provider, stream = true)
        var currentCall: Call? = null
        val handle = object : TranslationHandle {
            override fun cancel() {
                cancelled.set(true)
                currentCall?.cancel()
            }
        }

        currentCall = httpClient.newCall(request)
        currentCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cancelled.get()) return
                callback.onFailure(friendlyNetworkMessage(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    // 400/415/422 with a "stream"/"response_format" complaint →
                    // the server can't stream; fall back once to non-streaming.
                    if (allowFallback && isStreamUnsupported(code, it)) {
                        if (cancelled.get()) return
                        val fbCall = startNonStreaming(text, language, settings, provider, callback, cancelled)
                        // Wire cancellation of the outer handle to the fallback call.
                        // (currentCall already completed; the new call owns the socket.)
                        currentCall = null
                        return
                    }
                    if (!it.isSuccessful) {
                        if (cancelled.get()) return
                        val body = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
                        callback.onFailure("${provider.name} HTTP $code: ${body.take(240)}")
                        return
                    }
                    // 200 OK. Detect whether the server actually streamed (SSE)
                    // or ignored stream=true and returned a plain JSON body.
                    val contentType = it.header("Content-Type").orEmpty().lowercase()
                    val isSse = contentType.contains("text/event-stream") || contentType.contains("stream")
                    if (isSse) {
                        consumeSseStream(it, text, callback, cancelled)
                    } else {
                        consumePlainBody(it, text, callback, cancelled)
                    }
                }
            }
        })
        return handle
    }

    /** One-shot non-streaming fallback for servers that reject stream=true. */
    private fun startNonStreaming(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        provider: ModelProviderPreset,
        callback: ResultCallback,
        cancelled: AtomicBoolean
    ): TranslationHandle {
        val request = buildRequest(text, language, settings, provider, stream = false)
        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cancelled.get()) return
                callback.onFailure(friendlyNetworkMessage(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (cancelled.get()) return
                    if (!it.isSuccessful) {
                        val body = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
                        callback.onFailure("${provider.name} HTTP ${it.code}: ${body.take(240)}")
                        return
                    }
                    consumePlainBody(it, text, callback, cancelled)
                }
            }
        })
        return object : TranslationHandle { override fun cancel() { cancelled.set(true); call.cancel() } }
    }

    /** Read an SSE event stream, accumulating delta.content and emitting progress. */
    private fun consumeSseStream(
        response: Response,
        text: String,
        callback: ResultCallback,
        cancelled: AtomicBoolean
    ) {
        val extractor = StreamingTranslationExtractor()
        val full = StringBuilder()
        var reasoningNotified = false
        try {
            BufferedReader(InputStreamReader(response.body?.byteStream() ?: return)).use { reader ->
                while (!cancelled.get()) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty() || line.startsWith(":")) continue  // keep-alive / comment
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val (content, reasoning) = runCatching {
                        val delta = JSONObject(data)
                            .optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("delta")
                        (delta?.optString("content") ?: "") to (delta?.optString("reasoning_content") ?: "")
                    }.getOrDefault("" to "")
                    // Reasoning models (DeepSeek-R1, V4-Flash) emit reasoning_content
                    // first — potentially for many seconds — before any content. Give
                    // the user a visible "thinking" indicator during that phase so the
                    // stream doesn't look dead.
                    if (reasoning.isNotEmpty() && content.isEmpty()) {
                        if (!reasoningNotified) {
                            reasoningNotified = true
                            callback.onTranslationProgress("")
                        }
                    }
                    if (content.isNotEmpty()) {
                        full.append(content)
                        extractor.append(content)
                        val partial = extractor.currentTranslation
                        if (partial.isNotEmpty()) callback.onTranslationProgress(partial)
                    }
                }
            }
        } catch (e: IOException) {
            if (!cancelled.get()) { callback.onFailure(friendlyNetworkMessage(e)); return }
        }
        if (cancelled.get()) return
        finishWithContent(full.toString(), text, extractor, callback)
    }

    /** Server ignored stream=true and returned a normal JSON body in one shot. */
    private fun consumePlainBody(
        response: Response,
        text: String,
        callback: ResultCallback,
        cancelled: AtomicBoolean
    ) {
        val body = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
        if (cancelled.get()) return
        val content = runCatching {
            JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }.getOrDefault(body) // if not chat-completions shape, treat body as raw content
        finishWithContent(content, text, StreamingTranslationExtractor().apply { append(content) }, callback)
    }

    /** Parse the full accumulated model content into an AnalysisResult. */
    private fun finishWithContent(
        content: String,
        text: String,
        extractor: StreamingTranslationExtractor,
        callback: ResultCallback
    ) {
        val parsed = runCatching { AnalysisJsonParser.parse(content, text) }
        parsed.onSuccess(callback::onSuccess)
            .onFailure {
                // The full JSON didn't parse, but we may have already streamed a
                // usable translation. Keep it so the user isn't left empty.
                val quick = extractor.currentTranslation
                if (quick.isNotBlank()) {
                    callback.onSuccess(
                        AnalysisResult(
                            sourceText = text,
                            translation = quick,
                            language = com.poozh.translator.model.LanguageDetector.detect(text),
                            summary = "（详细解析不可用）"
                        )
                    )
                } else {
                    callback.onFailure(it.message ?: "模型服务返回解析失败")
                }
            }
    }

    /** True when the error response indicates the server can't handle streaming. */
    private fun isStreamUnsupported(code: Int, response: Response): Boolean {
        if (code !in setOf(400, 415, 422)) return false
        val body = runCatching { response.peekBody(2048L).string().lowercase() }.getOrDefault("")
        return body.contains("stream") || body.contains("response_format")
    }

    private fun buildRequest(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        provider: ModelProviderPreset,
        stream: Boolean
    ): Request {
        val body = buildRequestJson(text, language, settings.model, provider.supportsJsonMode, stream)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        return Request.Builder()
            .url(settings.baseUrl)
            .header("Content-Type", "application/json")
            .header("Accept", if (stream) "text/event-stream" else "application/json")
            .post(body)
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${settings.apiKey}")
                }
            }
            .build()
    }

    private fun buildRequestJson(
        text: String,
        language: TextLanguage,
        model: String,
        supportsJsonMode: Boolean,
        stream: Boolean
    ): JSONObject {
        val messages = org.json.JSONArray()
            .put(JSONObject().put("role", "system").put("content", DeepSeekPrompt.systemPrompt(language)))
            .put(JSONObject().put("role", "user").put("content", DeepSeekPrompt.userPrompt(text, language)))

        val json = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("stream", stream)
        if (supportsJsonMode && !stream) {
            // response_format is meaningless under streaming and some servers
            // reject it; only send it for the non-streaming fallback.
            json.put("response_format", JSONObject().put("type", "json_object"))
        }
        return json
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

    private object NoopHandle : TranslationHandle { override fun cancel() {} }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
