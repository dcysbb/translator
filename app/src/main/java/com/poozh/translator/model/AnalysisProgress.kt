package com.poozh.translator.model

/**
 * The part of an analysis that has arrived from the single streaming request.
 * The lists only contain complete JSON array items, so the UI never renders a
 * half-written word or grammar sentence.
 */
data class AnalysisProgress(
    val sourceText: String,
    val translation: String = "",
    val words: List<TermNote> = emptyList(),
    val grammar: List<String> = emptyList(),
    val isThinking: Boolean = false
) {
    fun toResult(language: TextLanguage = LanguageDetector.detect(sourceText)): AnalysisResult {
        return AnalysisResult(
            sourceText = sourceText,
            translation = translation,
            language = language,
            words = words,
            grammar = grammar
        )
    }
}
