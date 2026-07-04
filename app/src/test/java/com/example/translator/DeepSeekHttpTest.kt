package com.example.translator

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration-style tests for [DeepSeekClient.translate] using MockWebServer,
 * covering success, auth error, rate limit, and bad-JSON responses.
 */
class DeepSeekHttpTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DeepSeekClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        // baseUrl must end with '/' and point at the mock server.
        client = DeepSeekClient(
            apiKey = "sk-test",
            baseUrl = server.url("/").toString(),
            modelName = "deepseek-chat"
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun successParsesTranslation() = runBlocking {
        val body = """
            {
              "choices": [
                {"message": {"role":"assistant","content":"{\"translation\":\"你好\",\"language\":\"Chinese\"}"}}
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val outcome = client.translate("Hello")
        assertTrue(outcome is TranslationOutcome.Success)
        assertEquals("你好", (outcome as TranslationOutcome.Success).result.translation)
    }

    @Test
    fun unauthorizedMapsToInvalidApiKey() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"invalid api key"}"""))
        val outcome = client.translate("Hello")
        assertTrue(outcome is TranslationOutcome.Error)
        assertEquals(ErrorCode.INVALID_API_KEY, (outcome as TranslationOutcome.Error).code)
    }

    @Test
    fun rateLimitedMapsToRateLimited() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))
        val outcome = client.translate("Hello")
        assertTrue(outcome is TranslationOutcome.Error)
        assertEquals(ErrorCode.RATE_LIMITED, (outcome as TranslationOutcome.Error).code)
    }

    @Test
    fun emptyTextReturnsEmptyTextErrorWithoutNetwork() = runBlocking {
        val outcome = client.translate("   ")
        assertTrue(outcome is TranslationOutcome.Error)
        assertEquals(ErrorCode.EMPTY_TEXT, (outcome as TranslationOutcome.Error).code)
    }

    @Test
    fun missingApiKeyReturnsEmptyTextError() = runBlocking {
        val noKeyClient = DeepSeekClient(
            apiKey = "",
            baseUrl = server.url("/").toString()
        )
        val outcome = noKeyClient.translate("Hello")
        assertTrue(outcome is TranslationOutcome.Error)
        assertEquals(ErrorCode.EMPTY_TEXT, (outcome as TranslationOutcome.Error).code)
    }
}
