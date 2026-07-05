package com.poozh.translator

import com.poozh.translator.data.ModelProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelProvidersTest {
    @Test
    fun resolvesKnownProviderFromBaseUrl() {
        val kimi = "https://api.moonshot.cn/v1/chat/completions/"

        assertEquals("Moonshot Kimi", ModelProviders.displayName(kimi))
        assertTrue(ModelProviders.requiresApiKey(kimi))
    }

    @Test
    fun allowsLocalOllamaWithoutApiKey() {
        val ollama = "http://127.0.0.1:11434/v1/chat/completions"

        assertEquals("Ollama 本地", ModelProviders.displayName(ollama))
        assertFalse(ModelProviders.requiresApiKey(ollama))
    }

    @Test
    fun labelsUnknownEndpointAsCustomService() {
        assertEquals("自定义模型服务", ModelProviders.displayName("https://example.com/v1/chat/completions"))
    }
}
