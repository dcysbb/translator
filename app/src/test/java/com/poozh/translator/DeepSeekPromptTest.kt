package com.poozh.translator

import com.poozh.translator.data.DeepSeekPrompt
import com.poozh.translator.model.TextLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekPromptTest {
    @Test
    fun japanesePromptRequestsGrammarFields() {
        val prompt = DeepSeekPrompt.systemPrompt(TextLanguage.JAPANESE)
        assertTrue(prompt.contains("particles"))
        assertTrue(prompt.contains("conjugations"))
        assertTrue(prompt.contains("fixedExpressions"))
    }

    @Test
    fun userPromptCarriesOriginalText() {
        val prompt = DeepSeekPrompt.userPrompt("雨が降っています", TextLanguage.JAPANESE)
        assertTrue(prompt.contains("雨が降っています"))
        assertTrue(prompt.contains("ja"))
    }
}
