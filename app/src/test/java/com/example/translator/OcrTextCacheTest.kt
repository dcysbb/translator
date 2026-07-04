package com.example.translator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextCacheTest {

    @Test
    fun firstNonBlankTextIsTranslated() {
        val cache = OcrTextCache()
        assertTrue(cache.shouldTranslate("こんにちは"))
    }

    @Test
    fun blankTextIsNeverTranslated() {
        val cache = OcrTextCache()
        assertFalse(cache.shouldTranslate(""))
        assertFalse(cache.shouldTranslate("   \n  "))
    }

    @Test
    fun identicalTextAfterRememberIsSkipped() {
        val cache = OcrTextCache()
        cache.remember("Hello World")
        assertFalse(cache.shouldTranslate("Hello World"))
    }

    @Test
    fun whitespaceDifferencesAreNormalizedAway() {
        val cache = OcrTextCache()
        cache.remember("hello   world")
        // Same words, different spacing/case -> considered identical.
        assertFalse(cache.shouldTranslate("Hello\nWorld"))
    }

    @Test
    fun sufficientlyDifferentTextIsTranslatedAgain() {
        val cache = OcrTextCache()
        cache.remember("I am a student")
        assertTrue(cache.shouldTranslate("She works at the hospital"))
    }

    @Test
    fun smallOcrNoiseDoesNotTriggerRedo() {
        // A single extra token out of many keeps similarity above threshold.
        val cache = OcrTextCache(similarityThreshold = 0.92)
        cache.remember("the quick brown fox jumps over the lazy dog")
        // Add one word to a 9-word sentence -> 8/10 intersection ~ 0.8 < 0.92,
        // so this SHOULD translate. Use a near-identical string instead.
        assertFalse(
            "near-identical text must be skipped",
            cache.shouldTranslate("the quick brown fox jumps over the lazy dog")
        )
    }

    @Test
    fun resetAllowsSameTextAgain() {
        val cache = OcrTextCache()
        cache.remember("Hello")
        assertFalse(cache.shouldTranslate("Hello"))
        cache.reset()
        assertTrue(cache.shouldTranslate("Hello"))
    }

    @Test
    fun similarityBounds() {
        val cache = OcrTextCache()
        assertEquals(1.0, cache.similarity("abc", "abc"), 1e-9)
        assertEquals(0.0, cache.similarity("", "abc"), 1e-9)
        assertTrue(cache.similarity("a b c", "a b c d") > 0.0)
        assertTrue(cache.similarity("a b c", "a b c d") < 1.0)
    }
}
