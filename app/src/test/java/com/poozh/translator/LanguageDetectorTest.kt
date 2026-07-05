package com.poozh.translator

import com.poozh.translator.model.LanguageDetector
import com.poozh.translator.model.TextLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {
    @Test
    fun detectsJapaneseKana() {
        assertEquals(TextLanguage.JAPANESE, LanguageDetector.detect("これはテストです"))
    }

    @Test
    fun detectsEnglish() {
        assertEquals(TextLanguage.ENGLISH, LanguageDetector.detect("This is a test."))
    }

    @Test
    fun treatsKanjiOnlyAsJapaneseForSupportedScope() {
        assertEquals(TextLanguage.JAPANESE, LanguageDetector.detect("日本語解析"))
    }
}

