package com.poozh.translator.data

import com.poozh.translator.model.TextLanguage

object DeepSeekPrompt {
    /**
     * Field order matters for streaming: the client incrementally extracts the
     * `"translation"` value as it arrives, so `translation` MUST be the first
     * field in the JSON object so users see a translation ASAP. The remaining
     * fields (language, summary, vocabulary, ...) follow.
     */
    fun systemPrompt(language: TextLanguage): String {
        // The ordered field list is shared; only the lead-in differs per language.
        val fields = "translation, sourceText, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar"
        val arrayNote = "vocabulary/particles/conjugations/fixedExpressions 是数组（每项含 surface, reading, meaning, note），grammar 是字符串数组；无内容用空字符串或空数组。"
        return when (language) {
            TextLanguage.JAPANESE -> """
                你是日语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并解析日语语法。
                必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。
                JSON 字段顺序必须为：$fields。
                重要：第一个字段必须是 translation（中文译文），其余字段随后。
                $arrayNote
            """.trimIndent()

            TextLanguage.ENGLISH -> """
                你是英语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并给出简短词句说明。
                必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。
                JSON 字段顺序必须为：$fields。
                重要：第一个字段必须是 translation（中文译文），其余字段随后。
                英语不需要助词和活用解析，相关数组可为空。
            """.trimIndent()

            TextLanguage.UNKNOWN -> """
                你是屏幕文本翻译助手。请识别文本主要语言，翻译为自然中文，并在可能时给出简短解析。
                必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。
                JSON 字段顺序必须为：$fields。
                重要：第一个字段必须是 translation（中文译文），其余字段随后。
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
