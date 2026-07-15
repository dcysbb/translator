package com.poozh.translator

import com.poozh.translator.data.DeepSeekPrompt
import com.poozh.translator.model.TextLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekPromptTest {
    @Test
    fun japanesePromptRequestsWordsAndGrammar() {
        val prompt = DeepSeekPrompt.systemPrompt(TextLanguage.JAPANESE)
        assertTrue(prompt.contains("words"))
        assertTrue(prompt.contains("grammar"))
    }

    @Test
    fun translationIsRequestedAsFirstFieldForStreaming() {
        for (lang in TextLanguage.values()) {
            val prompt = DeepSeekPrompt.systemPrompt(lang)
            // The response starts with fields that can be rendered while the
            // single SSE request is still in flight.
            val translationIdx = prompt.indexOf("translation")
            val wordsIdx = prompt.indexOf("words")
            val grammarIdx = prompt.indexOf("grammar")
            assertTrue("lang=$lang translation should be mentioned", translationIdx >= 0)
            assertTrue("lang=$lang translation should precede words", translationIdx < wordsIdx)
            assertTrue("lang=$lang words should precede grammar", wordsIdx < grammarIdx)
        }
    }

    @Test
    fun userPromptCarriesOriginalText() {
        val prompt = DeepSeekPrompt.userPrompt("雨が降っています", TextLanguage.JAPANESE)
        assertTrue(prompt.contains("雨が降っています"))
        assertTrue(prompt.contains("ja"))
    }
}
