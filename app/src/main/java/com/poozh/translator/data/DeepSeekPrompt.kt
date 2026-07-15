package com.poozh.translator.data

import com.poozh.translator.model.TextLanguage

object DeepSeekPrompt {
    /**
     * Returns the system prompt for the given language, or the user's custom
     * prompt if one is set. When [customPrompt] is non-blank it fully replaces
     * the built-in template.
     */
    fun systemPrompt(language: TextLanguage, customPrompt: String = ""): String {
        if (customPrompt.isNotBlank()) return customPrompt.trim()
        return builtInSystemPrompt(language)
    }

    fun builtInSystemPrompt(language: TextLanguage): String {
        val fields = "translation, sourceText, language, words, grammar"
        val wordsNote = "words 是数组，按原句中出现的顺序排列每个词（不按词性分类），每项含 surface（原文词形）, reading（读音/假名，无则留空）, meaning（中文释义）, note（语法/用法简注，无则留空）。"
        val grammarNote = "grammar 是字符串数组，每项是一条语法解析要点。"
        val jsonRule = "必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。JSON 字段顺序必须为：$fields。重要：第一个字段必须是 translation（中文译文），其余字段随后。"

        return when (language) {
            TextLanguage.JAPANESE -> """
                你是日语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并逐词解析。
                $jsonRule
                $wordsNote
                $grammarNote
                若某字段无内容，使用空字符串或空数组。
            """.trimIndent()

            TextLanguage.ENGLISH -> """
                你是英语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并逐词解析。
                $jsonRule
                $wordsNote
                $grammarNote
                若某字段无内容，使用空字符串或空数组。
            """.trimIndent()

            TextLanguage.UNKNOWN -> """
                你是屏幕文本翻译助手。请识别文本主要语言，翻译为自然中文，并在可能时逐词解析。
                $jsonRule
                $wordsNote
                $grammarNote
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
