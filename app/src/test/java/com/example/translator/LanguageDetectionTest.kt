package com.example.translator

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectionTest {

    @Test
    fun detectsJapaneseKana() {
        assertEquals(TextLanguage.JAPANESE, detectLanguage("これはペンです"))
    }

    @Test
    fun detectsJapaneseKanjiOnly() {
        assertEquals(TextLanguage.JAPANESE, detectLanguage("日本語の文章"))
    }

    @Test
    fun detectsEnglish() {
        assertEquals(TextLanguage.ENGLISH, detectLanguage("The quick brown fox"))
    }

    @Test
    fun mixedJapaneseAndEnglishClassifiedAsJapanese() {
        // Kana/kanji presence dominates -> Japanese (full grammar analysis).
        assertEquals(TextLanguage.JAPANESE, detectLanguage("I love 日本語"))
    }

    @Test
    fun emptyOrWhitespaceIsUnknown() {
        assertEquals(TextLanguage.UNKNOWN, detectLanguage(""))
        assertEquals(TextLanguage.UNKNOWN, detectLanguage("   \n\t "))
    }

    @Test
    fun onlyDigitsAndPunctuationIsUnknown() {
        assertEquals(TextLanguage.UNKNOWN, detectLanguage("1234 + 567 !!!"))
    }
}
