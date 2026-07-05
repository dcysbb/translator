package com.poozh.translator.data

import com.poozh.translator.model.AnalysisResult
import com.poozh.translator.model.LanguageDetector
import com.poozh.translator.model.TermNote
import com.poozh.translator.model.TextLanguage
import org.json.JSONArray
import org.json.JSONObject

object AnalysisJsonParser {
    fun parse(raw: String, fallbackSource: String): AnalysisResult {
        val jsonText = extractJsonObject(raw)
        val json = JSONObject(jsonText)
        val source = json.optString("sourceText").ifBlank { fallbackSource }
        val language = parseLanguage(json.optString("language"), source)

        return AnalysisResult(
            sourceText = source,
            translation = json.optString("translation"),
            language = language,
            summary = json.optString("summary"),
            vocabulary = json.optJSONArray("vocabulary").toTermNotes(),
            particles = json.optJSONArray("particles").toTermNotes(),
            conjugations = json.optJSONArray("conjugations").toTermNotes(),
            fixedExpressions = json.optJSONArray("fixedExpressions").toTermNotes(),
            tone = json.optString("tone"),
            grammar = json.optJSONArray("grammar").toStringList()
        )
    }

    fun fallback(source: String, message: String): AnalysisResult {
        return AnalysisResult(
            sourceText = source,
            translation = "",
            language = LanguageDetector.detect(source),
            summary = message
        )
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        require(start >= 0 && end > start) { "模型服务返回内容不是 JSON 对象" }
        return trimmed.substring(start, end + 1)
    }

    private fun parseLanguage(value: String, source: String): TextLanguage {
        return when (value.lowercase()) {
            "ja", "jp", "japanese", "日语", "日本語" -> TextLanguage.JAPANESE
            "en", "english", "英语" -> TextLanguage.ENGLISH
            else -> LanguageDetector.detect(source)
        }
    }

    private fun JSONArray?.toTermNotes(): List<TermNote> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val surface = item.optString("surface")
                if (surface.isBlank()) continue
                add(
                    TermNote(
                        surface = surface,
                        reading = item.optString("reading"),
                        meaning = item.optString("meaning"),
                        note = item.optString("note")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optString(index)
                if (item.isNotBlank()) add(item)
            }
        }
    }
}
