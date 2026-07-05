package com.poozh.translator.model

enum class RefreshAction {
    REQUEST_TRANSLATION,
    REUSE_CACHED_RESULT,
    IGNORE_EMPTY
}

object TranslationRefreshPolicy {
    fun decide(
        currentText: String,
        lastText: String,
        hasCachedResult: Boolean,
        forceTranslate: Boolean
    ): RefreshAction {
        if (currentText.isBlank()) return RefreshAction.IGNORE_EMPTY
        if (!forceTranslate && hasCachedResult && currentText == lastText) {
            return RefreshAction.REUSE_CACHED_RESULT
        }
        return RefreshAction.REQUEST_TRANSLATION
    }
}

