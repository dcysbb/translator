package com.poozh.translator.data

import com.poozh.translator.model.AnalysisProgress
import com.poozh.translator.model.TermNote
import com.poozh.translator.model.TextLanguage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tolerant incremental reader for the model's JSON content. Translation is
 * exposed character-by-character by [StreamingTranslationExtractor], while
 * words and grammar are published as soon as a complete array item arrives.
 */
class StreamingAnalysisExtractor {
    private val raw = StringBuilder()
    private val translationExtractor = StreamingTranslationExtractor()

    fun append(chunk: String) {
        if (chunk.isEmpty()) return
        raw.append(chunk)
        translationExtractor.append(chunk)
    }

    fun snapshot(
        sourceText: String,
        language: TextLanguage,
        isThinking: Boolean = false
    ): AnalysisProgress {
        return AnalysisProgress(
            sourceText = sourceText,
            translation = translationExtractor.currentTranslation,
            words = scanWords(),
            grammar = scanGrammar(),
            isThinking = isThinking
        )
    }

    val currentTranslation: String
        get() = translationExtractor.currentTranslation

    private fun scanWords(): List<TermNote> {
        val values = completedArrayValues("words")
        if (values.isEmpty()) {
            // Older prompts called this field vocabulary.
            return completedArrayValues("vocabulary").mapNotNull(::toTermNote)
        }
        return values.mapNotNull(::toTermNote)
    }

    private fun scanGrammar(): List<String> {
        return completedArrayValues("grammar").mapNotNull { value ->
            runCatching { JSONArray("[$value]").optString(0) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun toTermNote(value: String): TermNote? {
        return runCatching {
            val item = JSONObject(value)
            val surface = item.optString("surface").trim()
            if (surface.isBlank()) null else TermNote(
                surface = surface,
                reading = item.optString("reading"),
                meaning = item.optString("meaning"),
                note = item.optString("note")
            )
        }.getOrNull()
    }

    /** Return complete JSON values from the first matching root array. */
    private fun completedArrayValues(field: String): List<String> {
        val marker = "\"$field\""
        var searchFrom = 0
        var open = -1
        while (searchFrom < raw.length) {
            val keyStart = raw.indexOf(marker, searchFrom)
            if (keyStart < 0) break
            val colon = raw.indexOf(':', keyStart + marker.length)
            if (colon < 0) break
            var candidate = colon + 1
            while (candidate < raw.length && raw[candidate].isWhitespace()) candidate++
            if (candidate < raw.length && raw[candidate] == '[') {
                open = candidate
                break
            }
            searchFrom = keyStart + marker.length
        }
        if (open < 0) return emptyList()

        val values = mutableListOf<String>()
        var cursor = open + 1
        while (cursor < raw.length) {
            while (cursor < raw.length && (raw[cursor].isWhitespace() || raw[cursor] == ',')) cursor++
            if (cursor >= raw.length || raw[cursor] == ']') break
            val end = completeValueEnd(cursor) ?: break
            values += raw.substring(cursor, end)
            cursor = end
        }
        return values
    }

    /** End index (exclusive) of a complete JSON value, or null if incomplete. */
    private fun completeValueEnd(start: Int): Int? {
        return when (raw[start]) {
            '"' -> {
                var index = start + 1
                while (index < raw.length) {
                    when (raw[index]) {
                        '\\' -> {
                            if (index + 1 >= raw.length) return null
                            index += 2
                        }
                        '"' -> return index + 1
                        else -> index++
                    }
                }
                null
            }
            '{', '[' -> balancedValueEnd(start)
            else -> {
                var index = start
                while (index < raw.length && raw[index] != ',' && raw[index] != ']') index++
                if (index == raw.length) null else index
            }
        }
    }

    private fun balancedValueEnd(start: Int): Int? {
        val stack = ArrayDeque<Char>()
        var inString = false
        var escaped = false
        var index = start
        while (index < raw.length) {
            val c = raw[index]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
            } else {
                when (c) {
                    '"' -> inString = true
                    '{', '[' -> stack.addLast(c)
                    '}' -> {
                        if (stack.removeLastOrNull() != '{') return null
                    }
                    ']' -> {
                        if (stack.removeLastOrNull() != '[') return null
                    }
                }
                if (stack.isEmpty()) return index + 1
            }
            index++
        }
        return null
    }
}
