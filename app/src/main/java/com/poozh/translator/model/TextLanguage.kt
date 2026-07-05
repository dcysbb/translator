package com.poozh.translator.model

enum class TextLanguage(val apiName: String) {
    JAPANESE("ja"),
    ENGLISH("en"),
    UNKNOWN("unknown")
}

object LanguageDetector {
    private val japaneseKana = Regex("[\\u3040-\\u30ff\\u31f0-\\u31ff]")
    private val cjk = Regex("[\\u3400-\\u9fff]")
    private val latin = Regex("[A-Za-z]")

    fun detect(text: String): TextLanguage {
        val normalized = text.trim()
        if (normalized.isEmpty()) return TextLanguage.UNKNOWN

        if (japaneseKana.containsMatchIn(normalized)) return TextLanguage.JAPANESE

        val cjkCount = cjk.findAll(normalized).count()
        val latinCount = latin.findAll(normalized).count()

        return when {
            cjkCount > 0 && latinCount < cjkCount -> TextLanguage.JAPANESE
            latinCount > 0 -> TextLanguage.ENGLISH
            else -> TextLanguage.UNKNOWN
        }
    }
}

