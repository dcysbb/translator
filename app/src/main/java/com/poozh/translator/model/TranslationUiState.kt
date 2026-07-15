package com.poozh.translator.model

/**
 * Translation state kept by the service independently from overlay Views.
 *
 * The floating panel is destroyed when it is collapsed. Keeping the current
 * request state here lets streaming continue in the background and lets a
 * newly-created panel render the latest partial or final result immediately.
 */
sealed interface TranslationUiState {
    data object Idle : TranslationUiState

    data class InProgress(
        val sourceText: String,
        val partialTranslation: String = "",
        val isThinking: Boolean = false,
        val words: List<TermNote> = emptyList(),
        val grammar: List<String> = emptyList()
    ) : TranslationUiState

    data class Complete(
        val sourceText: String,
        val result: AnalysisResult
    ) : TranslationUiState

    data class Failed(
        val sourceText: String,
        val message: String
    ) : TranslationUiState

    data class Interrupted(
        val sourceText: String,
        val progress: AnalysisProgress,
        val message: String
    ) : TranslationUiState

    fun displayText(): String = when (this) {
        Idle -> "选择屏幕区域后，点“刷新”识别当前画面。\n\n这里会显示原文、中文翻译和语言解析。"
        is InProgress -> buildString {
            val progress = AnalysisProgress(
                sourceText = sourceText,
                translation = partialTranslation,
                words = words,
                grammar = grammar,
                isThinking = isThinking
            )
            if (partialTranslation.isNotBlank() || words.isNotEmpty() || grammar.isNotEmpty()) {
                append(progress.toResult().toDisplayText())
                append("\n\n").append(if (grammar.isNotEmpty()) "正在收尾解析…" else "正在解析详细内容…")
            } else {
                append("原文\n").append(sourceText)
                append(if (isThinking) "\n\n模型正在思考…" else "\n\n正在连接模型服务…")
            }
        }
        is Complete -> result.toDisplayText()
        is Failed -> "原文\n$sourceText\n\n❌ $message\n\n点底部“重译”可重试"
        is Interrupted -> buildString {
            append(progress.toResult().toDisplayText())
            append("\n\n⚠ ").append(message)
            append("\n\n点“刷新”可重新请求")
        }
    }

    fun statusText(): String = when (this) {
        Idle -> "等待刷新"
        is InProgress -> when {
            isThinking && partialTranslation.isBlank() -> "模型思考中"
            grammar.isNotEmpty() -> "解析语法"
            words.isNotEmpty() -> "解析词汇"
            else -> "正在翻译"
        }
        is Complete -> "翻译完成"
        is Failed -> "翻译失败"
        is Interrupted -> "解析不完整"
    }
}
