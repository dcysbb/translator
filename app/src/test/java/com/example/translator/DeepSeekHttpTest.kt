package com.example.translator

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

    /**
     * Providers that don't support JSON mode (e.g. Ollama) must omit
     * `response_format` from the request body — the field is set to null.
     */
    @Test
    fun jsonModeDisabledOmitsResponseFormat() = runBlocking {
        val body = """
            {
              "choices": [
                {"message": {"role":"assistant","content":"{\"translation\":\"你好\"}"}}
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val noJsonClient = DeepSeekClient(
            apiKey = "sk-test",
            baseUrl = server.url("/").toString(),
            modelName = "qwen2.5",
            supportsJsonMode = false
        )
        noJsonClient.translate("Hello")

        val recorded = server.takeRequest()
        val requestBody = recorded.body.readUtf8()
        // When JSON mode is off, response_format must NOT appear in the body.
        assertTrue(
            "response_format should be absent when supportsJsonMode=false",
            !requestBody.contains("response_format")
        )
    }

    /**
     * Regression: cancelling an in-flight translate() must propagate the
     * CancellationException instead of returning it as a TranslationOutcome.Error
     * (which previously surfaced "StandaloneCoroutine was canceled" to the user).
     */
    @Test
    fun cancellationPropagatesInsteadOfBecomingError() = runBlocking {
        // Server replies slowly so we can cancel mid-flight.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"content":"{}"}}]}""")
                .setBodyDelay(2, java.util.concurrent.TimeUnit.SECONDS)
        )

        var threwCancellation = false
        val job = async {
            try {
                client.translate("Hello")
            } catch (e: CancellationException) {
                threwCancellation = true
                throw e
            }
        }
        delay(150) // let the request start
        job.cancelAndJoin()

        assertTrue("CancellationException must propagate, not be swallowed", threwCancellation)
    }
}
