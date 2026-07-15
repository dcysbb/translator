package com.poozh.translator

import com.poozh.translator.data.StreamingAnalysisExtractor
import com.poozh.translator.model.TextLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingAnalysisExtractorTest {
    @Test
    fun publishesCompleteWordsAndGrammarAcrossChunks() {
        val extractor = StreamingAnalysisExtractor()
        extractor.append("""{"translation":"你好","words":[{"surface":"你","meaning":"你"}""")

        var progress = extractor.snapshot("hello", TextLanguage.ENGLISH)
        assertEquals("你好", progress.translation)
        assertEquals(listOf("你"), progress.words.map { it.surface })
        assertTrue(progress.grammar.isEmpty())

        extractor.append(""" ,{"surface":"好","meaning":"好"}],""")
        progress = extractor.snapshot("hello", TextLanguage.ENGLISH)
        assertEquals(listOf("你", "好"), progress.words.map { it.surface })
        assertTrue(progress.grammar.isEmpty())

        extractor.append(""""grammar":["问候句"]}""")
        progress = extractor.snapshot("hello", TextLanguage.ENGLISH)
        assertEquals(listOf("问候句"), progress.grammar)
    }

    @Test
    fun handlesEscapedContentAndLegacyVocabulary() {
        val extractor = StreamingAnalysisExtractor()
        extractor.append("{\"translation\":\"他说\\\"好\\\"\",\"vocabulary\":[{\"surface\":\"好\",\"note\":\"\\\"礼貌\\\"\"}]}")
        val progress = extractor.snapshot("source", TextLanguage.UNKNOWN)
        assertEquals("他说\"好\"", progress.translation)
        assertEquals("好", progress.words.single().surface)
        assertEquals("\"礼貌\"", progress.words.single().note)
    }
}
