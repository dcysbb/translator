package com.poozh.translator

import com.poozh.translator.data.AnalysisJsonParser
import com.poozh.translator.model.TextLanguage
import org.junit.Assert.assertEquals
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
              "summary": "ています 表示正在进行。",
              "vocabulary": [{"surface":"雨","reading":"あめ","meaning":"雨","note":"名词"}],
              "particles": [{"surface":"が","reading":"","meaning":"主格助词","note":"标记主语"}],
              "conjugations": [{"surface":"降っています","reading":"ふっています","meaning":"正在下","note":"ている"}],
              "fixedExpressions": [],
              "tone": "陈述",
              "grammar": ["N が Vています"]
            }
            ```
        """.trimIndent()

        val result = AnalysisJsonParser.parse(raw, "fallback")

        assertEquals(TextLanguage.JAPANESE, result.language)
        assertEquals("正在下雨。", result.translation)
        assertEquals(1, result.vocabulary.size)
        assertEquals("が", result.particles.first().surface)
        assertEquals("N が Vています", result.grammar.first())
    }
}

