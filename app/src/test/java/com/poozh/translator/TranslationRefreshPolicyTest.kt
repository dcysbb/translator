package com.poozh.translator

import com.poozh.translator.model.RefreshAction
import com.poozh.translator.model.TranslationRefreshPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationRefreshPolicyTest {
    @Test
    fun reusesCachedResultForSameText() {
        val action = TranslationRefreshPolicy.decide(
            currentText = "雨が降っています",
            lastText = "雨が降っています",
            hasCachedResult = true,
            forceTranslate = false
        )

        assertEquals(RefreshAction.REUSE_CACHED_RESULT, action)
    }

    @Test
    fun forceTranslateBypassesCache() {
        val action = TranslationRefreshPolicy.decide(
            currentText = "雨が降っています",
            lastText = "雨が降っています",
            hasCachedResult = true,
            forceTranslate = true
        )

        assertEquals(RefreshAction.REQUEST_TRANSLATION, action)
    }

    @Test
    fun emptyTextIsIgnored() {
        val action = TranslationRefreshPolicy.decide(
            currentText = " ",
            lastText = "previous",
            hasCachedResult = true,
            forceTranslate = true
        )

        assertEquals(RefreshAction.IGNORE_EMPTY, action)
    }
}

