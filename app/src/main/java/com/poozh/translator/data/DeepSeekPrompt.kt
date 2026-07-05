package com.poozh.translator.data

import com.poozh.translator.model.TextLanguage

object DeepSeekPrompt {
    fun systemPrompt(language: TextLanguage): String {
        return when (language) {
            TextLanguage.JAPANESE -> """
                你是日语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并解析日语语法。
                必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。
                JSON 字段：sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar。
                vocabulary/particles/conjugations/fixedExpressions 是数组，每项包含 surface, reading, meaning, note。
                grammar 是字符串数组。若某字段无内容，使用空字符串或空数组。
            """.trimIndent()

            TextLanguage.ENGLISH -> """
                你是英语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并给出简短词句说明。
                必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。
                JSON 字段：sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar。
                英语不需要助词和活用解析，相关数组可为空。
            """.trimIndent()

            TextLanguage.UNKNOWN -> """
                你是屏幕文本翻译助手。请识别文本主要语言，翻译为自然中文，并在可能时给出简短解析。
                必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。
                JSON 字段：sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar。
            """.trimIndent()
        }
    }

    fun userPrompt(text: String, language: TextLanguage): String {
        return """
            语言提示：${language.apiName}
            OCR 文本：
            $text
        """.trimIndent()
    }
}

