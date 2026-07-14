package com.poozh.translator

import com.poozh.translator.model.AnalysisResult
import com.poozh.translator.model.TextLanguage
import com.poozh.translator.model.TranslationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationUiStateTest {
    @Test
    fun inProgressStateRetainsLatestPartialWithoutAView() {
        val state: TranslationUiState = TranslationUiState.InProgress(
            sourceText = "hello",
            partialTranslation = "你好"
        )

        assertTrue(state.displayText().contains("你好"))
        assertEquals("正在翻译", state.statusText())
    }

    @Test
    fun completedStateCanBeRenderedAfterPanelIsRecreated() {
        val result = AnalysisResult(
            sourceText = "hello",
            translation = "你好",
            language = TextLanguage.ENGLISH
        )
        val state: TranslationUiState = TranslationUiState.Complete("hello", result)

        assertTrue(state.displayText().contains("中文\n你好"))
        assertEquals("翻译完成", state.statusText())
    }

    @Test
    fun thinkingAndFailureHaveExplicitStatus() {
        val thinking: TranslationUiState = TranslationUiState.InProgress("考える", isThinking = true)
        val failed: TranslationUiState = TranslationUiState.Failed("hello", "timeout")

        assertTrue(thinking.displayText().contains("模型正在思考"))
        assertEquals("模型思考中", thinking.statusText())
        assertTrue(failed.displayText().contains("timeout"))
        assertEquals("翻译失败", failed.statusText())
    }
}
