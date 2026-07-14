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
        val isThinking: Boolean = false
    ) : TranslationUiState

    data class Complete(
        val sourceText: String,
        val result: AnalysisResult
    ) : TranslationUiState

    data class Failed(
        val sourceText: String,
        val message: String
    ) : TranslationUiState

    fun displayText(): String = when (this) {
        Idle -> "选择屏幕区域后，点“刷新”识别当前画面。\n\n这里会显示原文、中文翻译和语言解析。"
        is InProgress -> buildString {
            append("原文\n").append(sourceText)
            when {
                partialTranslation.isNotBlank() -> {
                    append("\n\n中文\n").append(partialTranslation)
                    append("\n\n正在生成详细解析…")
                }
                isThinking -> append("\n\n模型正在思考…")
                else -> append("\n\n正在连接模型服务…")
            }
        }
        is Complete -> result.toDisplayText()
        is Failed -> "原文\n$sourceText\n\n❌ $message\n\n点底部“重译”可重试"
    }

    fun statusText(): String = when (this) {
        Idle -> "等待刷新"
        is InProgress -> if (isThinking && partialTranslation.isBlank()) "模型思考中" else "正在翻译"
        is Complete -> "翻译完成"
        is Failed -> "翻译失败"
    }
}
