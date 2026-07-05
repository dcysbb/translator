package com.example.translator

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ---- API transport models ----

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class Message(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: Message?
)

// ---- Parsed translation result ----
// Reflects the JSON schema we prompt DeepSeek to return. Every field is
// nullable so partial / English-mode responses parse without throwing.

data class VocabularyItem(
    val word: String? = null,
    val reading: String? = null,
    val meaning: String? = null
)

data class SentenceAnalysis(
    val sentence: String? = null,
    val translation: String? = null,
    val note: String? = null
)

data class TranslationResult(
    @SerializedName("originalText") val originalText: String? = null,
    @SerializedName("translation") val translation: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("vocabulary") val vocabulary: List<VocabularyItem>? = null,
    @SerializedName("particles") val particles: List<String>? = null,
    @SerializedName("conjugation") val conjugation: String? = null,
    @SerializedName("fixedExpressions") val fixedExpressions: List<String>? = null,
    @SerializedName("grammar") val grammar: String? = null,
    @SerializedName("pragmaticNotes") val pragmaticNotes: String? = null,
    @SerializedName("sentenceAnalysis") val sentenceAnalysis: List<SentenceAnalysis>? = null
)

/** Outcome of a translation request. */
sealed class TranslationOutcome {
    data class Success(val result: TranslationResult) : TranslationOutcome()
    /** The API replied with a recoverable error (auth, quota, rate limit, bad JSON). */
    data class Error(val message: String, val code: ErrorCode) : TranslationOutcome()
}

enum class ErrorCode {
    EMPTY_TEXT,
    INVALID_API_KEY,
    INSUFFICIENT_BALANCE,
    RATE_LIMITED,
    SERVER_ERROR,
    BAD_RESPONSE,
    NETWORK
}

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

/**
 * Minimal, dependency-free language detection used to pick the prompt template.
 * Japanese Kana / Kanji presence => full grammar analysis; otherwise English.
 */
enum class TextLanguage { JAPANESE, ENGLISH, UNKNOWN }

fun detectLanguage(text: String): TextLanguage {
    if (text.isBlank()) return TextLanguage.UNKNOWN
    var hasAsciiLetter = false
    var hasKanaOrKanji = false
    for (ch in text) {
        val c = ch.code
        when {
            // Hiragana / Katakana ranges
            c in 0x3040..0x30FF || c in 0x31F0..0x31FF -> hasKanaOrKanji = true
            // CJK Unified Ideographs (Kanji) + extensions A
            c in 0x4E00..0x9FFF || c in 0x3400..0x4DBF -> hasKanaOrKanji = true
            // Basic Latin letters
            c in 0x41..0x5A || c in 0x61..0x7A -> hasAsciiLetter = true
        }
    }
    return when {
        hasKanaOrKanji -> TextLanguage.JAPANESE
        hasAsciiLetter -> TextLanguage.ENGLISH
        else -> TextLanguage.UNKNOWN
    }
}

/**
 * OpenAI-compatible chat client. Builds a request using a language-specific
 * prompt template and parses the returned JSON into a [TranslationResult].
 * Works against any provider that speaks the `/chat/completions` protocol
 * (DeepSeek, OpenAI, OpenRouter, Qwen, Zhipu, Moonshot, Ollama, ...).
 *
 * Constructed per capture session with the user's stored preferences.
 *
 * @param supportsJsonMode when true, `response_format: json_object` is sent to
 *   enforce strict JSON. Some endpoints (notably local Ollama models) reject
 *   this, so it can be turned off — the tolerant parser then handles a
 *   free-form JSON reply.
 */
class DeepSeekClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com/",
    private val modelName: String = DEFAULT_MODEL,
    private val supportsJsonMode: Boolean = true
) {
    private val api: DeepSeekApi
    private val gson = Gson()

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }

    /** Translate and analyze [text]. Returns a structured outcome. */
    suspend fun translate(text: String): TranslationOutcome {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            return TranslationOutcome.Error("No text to translate", ErrorCode.EMPTY_TEXT)
        }
        // Local servers (needsApiKey=false) may run without a key.
        if (apiKey.isBlank() && supportsJsonMode) {
            return TranslationOutcome.Error("未配置 API Key", ErrorCode.EMPTY_TEXT)
        }

        val language = detectLanguage(cleaned)
        val systemPrompt = systemPromptFor(language)

        val request = ChatRequest(
            model = modelName,
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", cleaned)
            ),
            // Only enforce JSON mode when the provider supports it; otherwise
            // rely on the prompt + tolerant parsing.
            responseFormat = if (supportsJsonMode) ResponseFormat("json_object") else null
        )

        return try {
            val response = api.chat("Bearer $apiKey", request)
            if (!response.isSuccessful) {
                return TranslationOutcome.Error(
                    httpErrorMessage(response.code(), response.errorBody()?.string()),
                    mapHttpCode(response.code())
                )
            }
            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return TranslationOutcome.Error("服务器返回为空", ErrorCode.BAD_RESPONSE)
            parseContent(content, cleaned, language)
        } catch (e: CancellationException) {
            // Coroutine cancellation is cooperative control flow, not an error.
            // It must propagate (e.g. when a new refresh cancels the previous
            // request) instead of being reported to the user.
            throw e
        } catch (e: HttpException) {
            TranslationOutcome.Error(
                httpErrorMessage(e.code(), null),
                mapHttpCode(e.code())
            )
        } catch (e: Exception) {
            TranslationOutcome.Error(e.message ?: "网络错误", ErrorCode.NETWORK)
        }
    }

    /**
     * Parse the model's content into a [TranslationResult]. Tolerates JSON that
     * is either a bare object or wrapped in ```json fences.
     */
    fun parseContent(
        content: String,
        originalText: String,
        language: TextLanguage
    ): TranslationOutcome {
        val json = extractJsonObject(content) ?: return TranslationOutcome.Error(
            "Could not parse JSON response", ErrorCode.BAD_RESPONSE
        )
        return try {
            val result = gson.fromJson(json, TranslationResult::class.java)
                ?.copy(originalText = originalText, language = languageLabel(language))
                ?: TranslationResult(originalText = originalText, translation = json)
            TranslationOutcome.Success(result)
        } catch (e: Exception) {
            TranslationOutcome.Error("Invalid JSON: ${e.message}", ErrorCode.BAD_RESPONSE)
        }
    }

    private fun extractJsonObject(content: String): String? {
        val trimmed = content.trim()
        // Direct JSON object.
        if (trimmed.startsWith("{")) {
            val valid = try {
                JsonParser.parseString(trimmed).isJsonObject
            } catch (_: Exception) {
                false
            }
            if (valid) return trimmed
        }
        // JSON wrapped in ```json ... ``` fences.
        val fenceMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(trimmed)
        if (fenceMatch != null) {
            val inner = fenceMatch.groupValues[1].trim()
            val valid = try {
                JsonParser.parseString(inner).isJsonObject
            } catch (_: Exception) {
                false
            }
            if (valid) return inner
        }
        // Last resort: first {...} substring.
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start in 0 until end) {
            val sub = trimmed.substring(start, end + 1)
            val valid = try {
                JsonParser.parseString(sub).isJsonObject
            } catch (_: Exception) {
                false
            }
            if (valid) return sub
        }
        return null
    }

    private fun languageLabel(language: TextLanguage) = when (language) {
        TextLanguage.JAPANESE -> "Japanese"
        TextLanguage.ENGLISH -> "English"
        TextLanguage.UNKNOWN -> "Unknown"
    }

    private fun mapHttpCode(code: Int): ErrorCode = when (code) {
        401, 403 -> ErrorCode.INVALID_API_KEY
        402 -> ErrorCode.INSUFFICIENT_BALANCE
        429 -> ErrorCode.RATE_LIMITED
        in 500..599 -> ErrorCode.SERVER_ERROR
        else -> ErrorCode.BAD_RESPONSE
    }

    private fun httpErrorMessage(code: Int, body: String?): String {
        val suffix = body?.take(200)?.let { " ($it)" } ?: ""
        return when (code) {
            401, 403 -> "API Key 无效或无权限 (HTTP $code)$suffix"
            402 -> "余额不足 (HTTP 402)$suffix"
            429 -> "请求过于频繁，请稍候 (HTTP 429)$suffix"
            in 500..599 -> "服务器错误 (HTTP $code)$suffix"
            else -> "请求失败 (HTTP $code)$suffix"
        }
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    companion object {
        const val DEFAULT_MODEL = "deepseek-chat"

        // Japanese: full grammar analysis (vocabulary, particles, conjugation,
        // fixed expressions, tone, sentence-by-sentence breakdown).
        internal val JAPANESE_PROMPT = """
            你是一位精通日语的翻译与语法解析助手。请将用户的日语翻译成简体中文，并做完整语法解析。
            只输出原始 JSON 对象，不要输出 markdown、解释或多余文字。严格遵循如下 schema：
            {
              "originalText": "用户给出的原文",
              "translation": "流畅的简体中文翻译",
              "language": "Japanese",
              "vocabulary": [{"word":"词或短语","reading":"假名读法或留空","meaning":"中文释义"}],
              "particles": ["重要助词及其作用说明"],
              "conjugation": "动词/形容词活用与变形说明，无则留空字符串",
              "fixedExpressions": ["固定表达、惯用语、句型"],
              "pragmaticNotes": "语气、语用、敬体/常体、文化背景说明",
              "sentenceAnalysis": [{"sentence":"单句原文","translation":"该句翻译","note":"该句语法要点"}]
            }
            字段可酌情留空数组或空字符串，但必须保持结构合法。
        """.trimIndent()

        // English: brief translation + light word/phrase notes (no full grammar).
        internal val ENGLISH_PROMPT = """
            你是一位精通英语的翻译助手。请将用户的英文翻译成简体中文，并给出简要词句说明。
            只输出原始 JSON 对象，不要输出 markdown 或多余文字。严格遵循如下 schema：
            {
              "originalText": "用户给出的原文",
              "translation": "流畅的简体中文翻译",
              "language": "English",
              "vocabulary": [{"word":"单词或短语","reading":"","meaning":"中文释义"}],
              "pragmaticNotes": "语气、语境或用法简述，可留空字符串"
            }
            其它字段可省略或留空，但必须保持结构合法。
        """.trimIndent()

        internal val UNKNOWN_PROMPT = """
            你是一位翻译助手。请将用户文本翻译成简体中文，并简要说明要点。
            只输出原始 JSON 对象，不要输出 markdown。schema：
            {
              "originalText": "原文",
              "translation": "简体中文翻译",
              "language": "检测到的语言",
              "vocabulary": [{"word":"","reading":"","meaning":""}],
              "pragmaticNotes": ""
            }
        """.trimIndent()

        fun systemPromptFor(language: TextLanguage): String = when (language) {
            TextLanguage.JAPANESE -> JAPANESE_PROMPT
            TextLanguage.ENGLISH -> ENGLISH_PROMPT
            TextLanguage.UNKNOWN -> UNKNOWN_PROMPT
        }
    }
}
