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
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    interface ResultCallback {
        fun onSuccess(result: AnalysisResult)
        fun onFailure(message: String)
    }

    fun analyze(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        callback: ResultCallback
    ): Call? {
        if (settings.apiKey.isBlank() && ModelProviders.requiresApiKey(settings.baseUrl)) {
            callback.onFailure("请先在主界面填写 ${ModelProviders.displayName(settings.baseUrl)} API Key")
            return null
        }
        if (text.isBlank()) {
            callback.onFailure("没有可翻译的文本")
            return null
        }

        val body = buildRequestJson(text, language, settings.model).toString()
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

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e.message ?: "模型服务请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        callback.onFailure("${ModelProviders.displayName(settings.baseUrl)} HTTP ${it.code}: ${responseBody.take(240)}")
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

    private fun buildRequestJson(text: String, language: TextLanguage, model: String): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", DeepSeekPrompt.systemPrompt(language)))
            .put(JSONObject().put("role", "user").put("content", DeepSeekPrompt.userPrompt(text, language)))

        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("stream", false)
            .put("response_format", JSONObject().put("type", "json_object"))
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
