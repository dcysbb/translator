package com.poozh.translator

import com.poozh.translator.data.AnalysisJsonParser
import com.poozh.translator.model.TextLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisJsonParserTest {
    @Test
    fun parsesJsonWrappedInMarkdownFence() {
        val raw = """
            ```json
            {
              "sourceText": "雨が降っています",
              "translation": "正在下雨。",
              "language": "ja",
              "words": [
                {"surface":"雨","reading":"あめ","meaning":"雨","note":"名词"},
                {"surface":"が","reading":"","meaning":"主格助词","note":"标记主语"},
                {"surface":"降っています","reading":"ふっています","meaning":"正在下","note":"ている"}
              ],
              "grammar": ["N が Vています"]
            }
            ```
        """.trimIndent()

        val result = AnalysisJsonParser.parse(raw, "fallback")

        assertEquals(TextLanguage.JAPANESE, result.language)
        assertEquals("正在下雨。", result.translation)
        assertEquals(3, result.words.size)
        assertEquals("雨", result.words[0].surface)
        assertEquals("あめ", result.words[0].reading)
        assertEquals("N が Vています", result.grammar.first())
    }

    @Test
    fun backwardCompatibleWithLegacyVocabularyField() {
        val raw = """{"translation":"hi","sourceText":"hello","language":"en","vocabulary":[{"surface":"hello","meaning":"你好"}]}"""
        val result = AnalysisJsonParser.parse(raw, "hello")
        assertEquals(1, result.words.size)
        assertEquals("hello", result.words[0].surface)
    }

    @Test
    fun toDisplayTextHasEmphasisedHeaders() {
        val raw = """{"translation":"你好","sourceText":"hello","language":"en","words":[{"surface":"hello","reading":"","meaning":"你好","note":""}],"grammar":["主谓结构"]}"""
        val result = AnalysisJsonParser.parse(raw, "hello")
        val text = result.toDisplayText()
        assertTrue(text.contains("━━ 原文 ━━"))
        assertTrue(text.contains("━━ 译文 ━━"))
        assertTrue(text.contains("━━ 单词释义 ━━"))
        assertTrue(text.contains("━━ 语法解析 ━━"))
    }
}
