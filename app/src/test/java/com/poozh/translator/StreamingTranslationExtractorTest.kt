package com.poozh.translator

import com.poozh.translator.data.StreamingTranslationExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingTranslationExtractorTest {

    @Test
    fun extractsTranslationBeforeFullJson() {
        val ex = StreamingTranslationExtractor()
        ex.append("""{"translation":"你好""")
        assertEquals("你好", ex.currentTranslation)
        assertFalse(ex.isComplete)
        ex.append("""","language":"ja"}""")
        assertTrue(ex.isComplete)
        assertEquals("你好", ex.currentTranslation)
    }

    @Test
    fun handlesCharByCharChunking() {
        val ex = StreamingTranslationExtractor()
        val full = """{"translation":"Hello world"}"""
        var last = ""
        for (c in full) {
            ex.append(c.toString())
            // Translation only grows after we're inside the value.
            if (ex.currentTranslation.isNotEmpty()) {
                assertTrue(ex.currentTranslation.length >= last.length)
                last = ex.currentTranslation
            }
        }
        assertEquals("Hello world", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }

    @Test
    fun unescapesQuotesAndBackslashes() {
        val ex = StreamingTranslationExtractor()
        // "He said \"hi\"\n done"
        ex.append("""{"translation":"He said \"hi\"""")
        assertEquals("He said \"hi\"", ex.currentTranslation)
        ex.append("""\n done"}""")
        assertEquals("He said \"hi\"\n done", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }

    @Test
    fun unescapesUnicode() {
        val ex = StreamingTranslationExtractor()
        // \u0041 is 'A'
        ex.append("""{"translation":"\u0041BC"}""")
        assertEquals("ABC", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }

    @Test
    fun unicodeSplitAcrossChunks() {
        val ex = StreamingTranslationExtractor()
        ex.append("""{"translation":"\u00""")
        assertEquals("", ex.currentTranslation) // not enough digits yet
        ex.append("""41B"}""")
        assertEquals("AB", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }

    @Test
    fun skipsOtherKeysBeforeTranslation() {
        val ex = StreamingTranslationExtractor()
        // Another field appears first; translation still extracted.
        ex.append("""{"sourceText":"原", "translation":"译""")
        assertEquals("译", ex.currentTranslation)
        ex.append("""文"}""")
        assertEquals("译文", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }

    @Test
    fun ignoresContentAfterValueClosed() {
        val ex = StreamingTranslationExtractor()
        ex.append("""{"translation":"done","vocabulary":[{"surface":"x"}]}""")
        assertEquals("done", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }

    @Test
    fun handlesKeyEscapeInOtherField() {
        val ex = StreamingTranslationExtractor()
        // A key with an escaped quote inside; translation comes after.
        ex.append("""{"foo\"bar":1,"translation":"ok"}""")
        assertEquals("ok", ex.currentTranslation)
        assertTrue(ex.isComplete)
    }
}
