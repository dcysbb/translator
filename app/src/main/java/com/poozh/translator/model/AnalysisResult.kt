package com.poozh.translator.model

data class TermNote(
    val surface: String,
    val reading: String = "",
    val meaning: String = "",
    val note: String = ""
)

data class AnalysisResult(
    val sourceText: String,
    val translation: String,
    val language: TextLanguage,
    /** Words in original sentence order (not grouped by part-of-speech). */
    val words: List<TermNote> = emptyList(),
    val grammar: List<String> = emptyList()
) {
    /**
     * Renders the result as structured text with emphasised section headers.
     * Word entries are returned as a flat list so the caller can wrap each
     * `surface` in a ClickableSpan for the favourites feature.
     */
    fun toDisplayText(): String = buildString {
        appendSectionHeader("原文")
        appendLine(sourceText.ifBlank { "未识别到文本" })

        appendSectionHeader("译文")
        appendLine(translation.ifBlank { "暂无翻译" })

        if (words.isNotEmpty()) {
            appendSectionHeader("单词释义")
            words.forEach { w ->
                val readingPart = w.reading.takeIf { it.isNotBlank() }?.let { "【$it】" } ?: ""
                val meaningPart = w.meaning.takeIf { it.isNotBlank() } ?: ""
                val notePart = w.note.takeIf { it.isNotBlank() }?.let { "（$it）" } ?: ""
                appendLine("• ${w.surface}$readingPart　$meaningPart$notePart")
            }
        }

        if (grammar.isNotEmpty()) {
            appendSectionHeader("语法解析")
            grammar.forEach { appendLine("• $it") }
        }
    }.trim()

    private fun StringBuilder.appendSectionHeader(title: String) {
        if (isNotEmpty()) appendLine()
        appendLine("━━ $title ━━")
    }
}
