package com.poozh.translator.data

import com.poozh.translator.model.AnalysisResult
import com.poozh.translator.model.AnalysisProgress
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
import java.util.concurrent.atomic.AtomicReference

class DeepSeekClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // A model may spend more than 90 seconds generating a long analysis.
        // The request is explicitly user-cancellable from the floating window,
        // so a read timeout would only discard useful partial output.
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Streaming translation callback. All methods are invoked from a background
     * OkHttp thread; callers must marshal onto the UI thread themselves.
     */
    interface ResultCallback {
        /** A new streaming analysis snapshot is available. */
        fun onAnalysisProgress(progress: AnalysisProgress) {
            // Keep source compatibility for callers that only consumed the
            // original translation-only callback.
            onTranslationProgress(progress.translation)
        }
        /** A partial translation has been extracted from the stream so far. */
        fun onTranslationProgress(partialTranslation: String) {}
        /** The full analysis (translation + grammar notes) is ready. */
        fun onSuccess(result: AnalysisResult)
        /** The request failed terminally. Retry is left to the user ("重译"). */
        fun onFailure(message: String)
        /** The connection ended after useful content had already arrived. */
        fun onPartialFailure(progress: AnalysisProgress, message: String) {
            onFailure(message)
        }
    }

    /** A cancellable handle for the single streaming translation request. */
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

        val session = RequestSession(callback)
        android.util.Log.d(TAG, "translation request started provider=${provider.id} model=${settings.model}")
        startStreaming(text, language, settings, provider, session)
        return session
    }

    /**
     * Fire the single streaming request. A response may still be a normal JSON
     * body when a gateway ignores stream=true; it is parsed from the same call.
     */
    private fun startStreaming(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        provider: ModelProviderPreset,
        session: RequestSession
    ) {
        val request = buildRequest(text, language, settings, provider, stream = true)
        val call = httpClient.newCall(request)
        if (!session.activate(call)) return
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!session.isRunning) return
                session.fail(friendlyNetworkMessage(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    if (!it.isSuccessful) {
                        if (!session.isRunning) return
                        val body = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
                        session.fail("${provider.name} HTTP $code: ${body.take(240)}")
                        return
                    }
                    consumeResponseBody(it, text, language, session)
                }
            }
        })
    }

    /**
     * Decide from the first meaningful response line instead of trusting the
     * Content-Type header. Several OpenAI-compatible gateways send real SSE
     * using application/json, which otherwise turns streaming into a blocking
     * body.string() call.
     */
    private fun consumeResponseBody(
        response: Response,
        text: String,
        language: TextLanguage,
        session: RequestSession
    ) {
        val body = response.body ?: run {
            session.fail("模型服务返回为空")
            return
        }
        try {
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var firstLine: String? = null
                while (session.isRunning && firstLine == null) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) firstLine = line.trimStart('\uFEFF')
                }
                if (!session.isRunning) return
                val first = firstLine ?: run {
                    session.fail("模型服务返回为空")
                    return
                }
                session.markFirstData()
                if (looksLikeSse(first)) {
                    consumeSseStream(reader, first, text, language, session)
                } else {
                    val plain = buildString {
                        append(first)
                        var line = reader.readLine()
                        while (line != null) {
                            append('\n').append(line)
                            line = reader.readLine()
                        }
                    }
                    consumePlainText(plain, text, language, session)
                }
            }
        } catch (e: IOException) {
            if (session.isRunning) session.fail(friendlyNetworkMessage(e))
        }
    }

    private fun looksLikeSse(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("data:") || trimmed.startsWith("event:") || trimmed.startsWith(":")
    }

    /** Read an SSE event stream, accumulating delta.content and emitting progress. */
    private fun consumeSseStream(
        reader: BufferedReader,
        firstLine: String,
        text: String,
        language: TextLanguage,
        session: RequestSession
    ) {
        val extractor = StreamingAnalysisExtractor()
        val full = StringBuilder()
        var reasoningNotified = false
        try {
            var line: String? = firstLine
            while (session.isRunning && line != null) {
                val currentLine = line ?: break
                line = reader.readLine()
                run {
                    if (currentLine.isEmpty() || currentLine.startsWith(":")) return@run
                    if (!currentLine.startsWith("data:")) return@run
                    val data = currentLine.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        line = null
                        return@run
                    }
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
                            session.progress(extractor.snapshot(text, language, isThinking = true))
                        }
                    }
                    if (content.isNotEmpty()) {
                        full.append(content)
                        extractor.append(content)
                        session.progress(extractor.snapshot(text, language, isThinking = false))
                    }
                }
            }
        } catch (e: IOException) {
            if (session.isRunning) {
                session.fail(
                    friendlyNetworkMessage(e),
                    extractor.snapshot(text, language, isThinking = false)
                )
                return
            }
        }
        if (!session.isRunning) return
        finishWithContent(full.toString(), text, language, extractor, session)
    }

    /** Server ignored stream=true and returned a normal JSON body in one shot. */
    private fun consumePlainText(
        body: String,
        text: String,
        language: TextLanguage,
        session: RequestSession
    ) {
        if (!session.isRunning) return
        val content = runCatching {
            JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }.getOrDefault(body) // if not chat-completions shape, treat body as raw content
        val extractor = StreamingAnalysisExtractor().apply { append(content) }
        session.progress(extractor.snapshot(text, language))
        finishWithContent(content, text, language, extractor, session)
    }

    /** Parse the full accumulated model content into an AnalysisResult. */
    private fun finishWithContent(
        content: String,
        text: String,
        language: TextLanguage,
        extractor: StreamingAnalysisExtractor,
        session: RequestSession
    ) {
        val parsed = runCatching { AnalysisJsonParser.parse(content, text) }
        parsed.onSuccess(session::succeed)
            .onFailure {
                val partial = extractor.snapshot(text, language)
                if (partial.translation.isNotBlank() || partial.words.isNotEmpty() || partial.grammar.isNotEmpty()) {
                    session.fail("解析不完整：${it.message ?: "模型服务返回内容不完整"}", partial)
                } else {
                    session.fail(it.message ?: "模型服务返回解析失败")
                }
            }
    }

    private fun buildRequest(
        text: String,
        language: TextLanguage,
        settings: SettingsSnapshot,
        provider: ModelProviderPreset,
        stream: Boolean
    ): Request {
        android.util.Log.d("DeepSeekClient", "buildRequest: thinkingEnabled=${settings.thinkingEnabled} stream=$stream model=${settings.model} customPrompt=${settings.customSystemPrompt.isNotBlank()}")
        val body = buildRequestJson(text, language, settings.model, stream, settings.thinkingEnabled, settings.customSystemPrompt)
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
        stream: Boolean,
        thinkingEnabled: Boolean,
        customPrompt: String = ""
    ): JSONObject {
        val messages = org.json.JSONArray()
            .put(JSONObject().put("role", "system").put("content", DeepSeekPrompt.systemPrompt(language, customPrompt)))
            .put(JSONObject().put("role", "user").put("content", DeepSeekPrompt.userPrompt(text, language)))

        val json = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .put("stream", stream)
        // Reasoning models (DeepSeek-R1, V4-Flash) accept a `thinking` object to
        // control the chain-of-thought phase. When disabled the model answers
        // directly — much faster, at a small quality cost on complex grammar.
        if (!thinkingEnabled) {
            json.put("thinking", JSONObject().put("type", "disabled"))
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

    /** Owns the active Call and prevents stale callbacks after cancellation. */
    private class RequestSession(
        private val callback: ResultCallback
    ) : TranslationHandle {
        private val cancelled = AtomicBoolean(false)
        private val terminal = AtomicBoolean(false)
        private val activeCall = AtomicReference<Call?>(null)
        private val startedAtNanos = System.nanoTime()
        private val firstDataLogged = AtomicBoolean(false)
        private val firstTranslationLogged = AtomicBoolean(false)

        val isRunning: Boolean
            get() = !cancelled.get() && !terminal.get()

        fun activate(call: Call): Boolean {
            if (!isRunning) {
                call.cancel()
                return false
            }
            activeCall.set(call)
            if (!isRunning && activeCall.compareAndSet(call, null)) {
                call.cancel()
                return false
            }
            return true
        }

        fun markFirstData() {
            if (firstDataLogged.compareAndSet(false, true)) {
                android.util.Log.d(TAG, "first response data after ${elapsedMs()}ms")
            }
        }

        fun progress(progress: AnalysisProgress) {
            if (!isRunning) return
            if (progress.translation.isNotBlank() && firstTranslationLogged.compareAndSet(false, true)) {
                android.util.Log.d(TAG, "first translation text after ${elapsedMs()}ms")
            }
            callback.onAnalysisProgress(progress)
        }

        fun succeed(result: AnalysisResult) {
            if (!terminal.compareAndSet(false, true) || cancelled.get()) return
            activeCall.set(null)
            android.util.Log.d(TAG, "translation completed after ${elapsedMs()}ms")
            callback.onSuccess(result)
        }

        fun fail(message: String, partial: AnalysisProgress? = null) {
            if (!terminal.compareAndSet(false, true) || cancelled.get()) return
            activeCall.set(null)
            android.util.Log.w(TAG, "translation failed after ${elapsedMs()}ms")
            if (partial != null) callback.onPartialFailure(partial, message) else callback.onFailure(message)
        }

        override fun cancel() {
            cancelled.set(true)
            terminal.set(true)
            activeCall.getAndSet(null)?.cancel()
        }

        private fun elapsedMs(): Long = (System.nanoTime() - startedAtNanos) / 1_000_000L
    }

    private object NoopHandle : TranslationHandle { override fun cancel() {} }

    companion object {
        private const val TAG = "TranslationTrace"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
