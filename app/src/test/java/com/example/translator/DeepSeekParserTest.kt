package com.example.translator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [DeepSeekClient.parseContent]: bare JSON, fenced JSON, partial
 * schema, empty/garbage, and template selection by language.
 *
 * These construct a [DeepSeekClient] but only exercise the pure [parseContent]
 * and prompt-selection logic, so no network or Android classes are touched.
 */
class DeepSeekParserTest {

    private val client = DeepSeekClient(apiKey = "test-key")

    @Test
    fun parsesBareJsonObject_japanese() {
        val json = """
            {
              "translation": "私は学生です。",
              "vocabulary": [
                {"word":"私","reading":"わたし","meaning":"我"},
                {"word":"学生","reading":"がくせい","meaning":"学生"}
              ],
              "particles": ["は：主题助词"],
              "conjugation": "です：断定助动词，敬体",
              "sentenceAnalysis": [
                {"sentence":"私は学生です。","translation":"我是学生。","note":"A是B句型"}
              ]
            }
        """.trimIndent()

        val outcome = client.parseContent(json, "私は学生です。", TextLanguage.JAPANESE)

        assertTrue(outcome is TranslationOutcome.Success)
        val r = (outcome as TranslationOutcome.Success).result
        assertEquals("私は学生です。", r.originalText)
        assertEquals("Japanese", r.language)
        assertEquals("私は学生です。", r.translation)
        assertEquals(2, r.vocabulary?.size)
        assertEquals("私", r.vocabulary!![0].word)
        assertEquals(1, r.sentenceAnalysis?.size)
    }

    @Test
    fun parsesJsonFencedInMarkdown() {
        val content = """
            Sure, here is the result:
            ```json
            {"translation":"Hello","language":"English","vocabulary":[]}
            ```
        """.trimIndent()

        val outcome = client.parseContent(content, "Hello", TextLanguage.ENGLISH)
        assertTrue(outcome is TranslationOutcome.Success)
        assertEquals("Hello", (outcome as TranslationOutcome.Success).result.translation)
    }

    @Test
    fun parsesPartialEnglishSchema() {
        val json = """{"translation":"Good morning","vocabulary":[{"word":"morning","meaning":"早晨"}]}"""
        val outcome = client.parseContent(json, "Good morning", TextLanguage.ENGLISH)
        assertTrue(outcome is TranslationOutcome.Success)
        val r = (outcome as TranslationOutcome.Success).result
        assertEquals("English", r.language)
        // Fields not in the payload default to null, no exception.
        assertEquals(null, r.particles)
        assertEquals(null, r.conjugation)
    }

    @Test
    fun returnsErrorForEmptyContent() {
        val outcome = client.parseContent("", "anything", TextLanguage.UNKNOWN)
        assertTrue(outcome is TranslationOutcome.Error)
        assertEquals(ErrorCode.BAD_RESPONSE, (outcome as TranslationOutcome.Error).code)
    }

    @Test
    fun returnsErrorForGarbageNonJson() {
        val outcome = client.parseContent("the quick brown fox", "x", TextLanguage.UNKNOWN)
        assertTrue(outcome is TranslationOutcome.Error)
    }

    @Test
    fun templateSelectionJapaneseUsesFullAnalysis() {
        val prompt = DeepSeekClient.systemPromptFor(TextLanguage.JAPANESE)
        // Japanese template asks for particles, conjugation, sentence analysis.
        assertTrue(prompt.contains("particles"))
        assertTrue(prompt.contains("conjugation"))
        assertTrue(prompt.contains("sentenceAnalysis"))
    }

    @Test
    fun templateSelectionEnglishIsBrief() {
        val prompt = DeepSeekClient.systemPromptFor(TextLanguage.ENGLISH)
        // English template must NOT ask for full grammar breakdown.
        assertTrue(!prompt.contains("sentenceAnalysis"))
        assertTrue(prompt.contains("translation"))
    }
}
