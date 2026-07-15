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
            // "translation" must appear before "sourceText" so the streaming
            // extractor can surface it first.
            val translationIdx = prompt.indexOf("translation")
            val sourceTextIdx = prompt.indexOf("sourceText")
            assertTrue("lang=$lang translation should be mentioned", translationIdx >= 0)
            assertTrue("lang=$lang translation should precede sourceText", translationIdx < sourceTextIdx)
        }
    }

    @Test
    fun userPromptCarriesOriginalText() {
        val prompt = DeepSeekPrompt.userPrompt("雨が降っています", TextLanguage.JAPANESE)
        assertTrue(prompt.contains("雨が降っています"))
        assertTrue(prompt.contains("ja"))
    }
}
