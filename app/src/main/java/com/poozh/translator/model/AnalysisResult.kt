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
    val summary: String = "",
    val vocabulary: List<TermNote> = emptyList(),
    val particles: List<TermNote> = emptyList(),
    val conjugations: List<TermNote> = emptyList(),
    val fixedExpressions: List<TermNote> = emptyList(),
    val tone: String = "",
    val grammar: List<String> = emptyList()
) {
    fun toDisplayText(): String {
        val builder = StringBuilder()
        builder.appendLine("原文")
        builder.appendLine(sourceText.ifBlank { "未识别到文本" })
        builder.appendLine()
        builder.appendLine("中文")
        builder.appendLine(translation.ifBlank { "暂无翻译" })

        if (summary.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("说明")
            builder.appendLine(summary)
        }

        appendNotes(builder, "词汇", vocabulary)
        appendNotes(builder, "助词", particles)
        appendNotes(builder, "活用", conjugations)
        appendNotes(builder, "固定表达", fixedExpressions)

        if (tone.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("语气")
            builder.appendLine(tone)
        }

        if (grammar.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("语法")
            grammar.forEach { builder.appendLine("• $it") }
        }

        return builder.toString().trim()
    }

    private fun appendNotes(builder: StringBuilder, title: String, notes: List<TermNote>) {
        if (notes.isEmpty()) return
        builder.appendLine()
        builder.appendLine(title)
        notes.forEach { note ->
            val parts = listOf(note.reading, note.meaning, note.note).filter { it.isNotBlank() }
            builder.append("• ").append(note.surface)
            if (parts.isNotEmpty()) builder.append("：").append(parts.joinToString("；"))
            builder.appendLine()
        }
    }
}

