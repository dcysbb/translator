package com.poozh.translator

import com.poozh.translator.data.DeepSeekClient
import com.poozh.translator.data.SettingsSnapshot
import com.poozh.translator.model.AnalysisResult
import com.poozh.translator.model.TextLanguage
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for the streaming translation client using MockWebServer.
 * Covers: SSE streaming with incremental progress, plain-JSON (server ignores
 * stream=true), and stream-unsupported fallback to one non-streaming request.
 */
class DeepSeekStreamingTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DeepSeekClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = DeepSeekClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun snapshot(): SettingsSnapshot = SettingsSnapshot(
        providerId = "test",
        apiKey = "sk-test",
        baseUrl = server.url("/v1/chat/completions").toString(),
        model = "test-model",
        intervalMs = 1000L,
        wifiOnly = false
    )

    @Test
    fun sseStreamEmitsIncrementalTranslationThenCompletes() {
        // Build the SSE body programmatically to avoid escape hell. Each data
        // line carries a delta.content fragment; concatenated they form valid JSON.
        fun deltaJson(content: String): String {
            val obj = org.json.JSONObject()
            val choices = org.json.JSONArray()
            val choice = org.json.JSONObject()
            val delta = org.json.JSONObject().put("content", content)
            choice.put("delta", delta)
            choices.put(choice)
            obj.put("choices", choices)
            return obj.toString()
        }
        val sse = buildString {
            append("data: ").append(deltaJson("""{"translation":"你好""")).append("\n\n")
            append("data: ").append(deltaJson("""世界","language":"ja"}""")).append("\n\n")
            append("data: [DONE]\n\n")
        }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sse)
        )

        val progressCount = AtomicInteger(0)
        val lastPartial = AtomicReference<String>("")
        val failRef = AtomicReference<String?>(null)
        val resultRef = AtomicReference<AnalysisResult?>(null)
        val latch = CountDownLatch(1)
        client.analyze("hello", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onTranslationProgress(partial: String) { progressCount.incrementAndGet(); lastPartial.set(partial) }
            override fun onSuccess(result: AnalysisResult) { resultRef.set(result); latch.countDown() }
            override fun onFailure(message: String) { failRef.set(message); latch.countDown() }
        })
        assertTrue("onSuccess not called (fail=${failRef.get()})", latch.await(5, TimeUnit.SECONDS))
        val result = resultRef.get()
        assertTrue("result should contain translation (fail=${failRef.get()})", result != null)
        assertTrue("translation should contain 你好世界 (was '${result!!.translation}')",
            result.translation.contains("你好世界"))
        assertTrue("should have emitted progress", progressCount.get() >= 1)
        // Exactly one HTTP request for the normal streaming path.
        assertEquals(1, server.requestCount)
    }

    @Test
    fun plainJsonBodyIsHandledInOneRequest() {
        // Server ignores stream=true and returns a normal chat completion.
        val body = """{"choices":[{"message":{"content":"{\"translation\":\"你好\",\"language\":\"en\"}"}}]}"""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        val resultRef = AtomicReference<AnalysisResult?>(null)
        val latch = CountDownLatch(1)
        client.analyze("hi", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onSuccess(result: AnalysisResult) { resultRef.set(result); latch.countDown() }
            override fun onFailure(message: String) { latch.countDown() }
        })
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("你好", resultRef.get()?.translation)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun mislabeledSseBodyStillStreamsInOneRequest() {
        fun delta(content: String): String {
            val choice = org.json.JSONObject().put(
                "delta",
                org.json.JSONObject().put("content", content)
            )
            return org.json.JSONObject()
                .put("choices", org.json.JSONArray().put(choice))
                .toString()
        }
        val sse = buildString {
            append("data: ").append(delta("""{"translation":"流""")).append("\n\n")
            append("data: ").append(delta("""式","language":"zh"}""")).append("\n\n")
            append("data: [DONE]\n\n")
        }
        // Some compatible gateways stream SSE while incorrectly declaring JSON.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(sse)
        )

        val partial = AtomicReference("")
        val result = AtomicReference<AnalysisResult?>(null)
        val latch = CountDownLatch(1)
        client.analyze("stream", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onTranslationProgress(partialTranslation: String) { partial.set(partialTranslation) }
            override fun onSuccess(value: AnalysisResult) { result.set(value); latch.countDown() }
            override fun onFailure(message: String) { latch.countDown() }
        })

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("流式", partial.get())
        assertEquals("流式", result.get()?.translation)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun streamUnsupportedFallsBackToSingleNonStreamingRequest() {
        // First: 400 with a stream-related error body.
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"message":"stream not supported"}}""")
        )
        // Then: a normal non-streaming JSON success.
        val body = """{"choices":[{"message":{"content":"{\"translation\":\"hi\",\"language\":\"en\"}"}}]}"""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        val resultRef = AtomicReference<AnalysisResult?>(null)
        val latch = CountDownLatch(1)
        client.analyze("hi", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onSuccess(result: AnalysisResult) { resultRef.set(result); latch.countDown() }
            override fun onFailure(message: String) { latch.countDown() }
        })
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("hi", resultRef.get()?.translation)
        // Exactly two requests: the streaming attempt + one fallback.
        assertEquals(2, server.requestCount)
    }

    @Test
    fun other400ErrorsDoNotFallback() {
        // A 400 that is NOT about streaming (e.g. invalid model) → fail, no retry.
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"message":"model not found"}}""")
        )

        val failRef = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        client.analyze("hi", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onSuccess(result: AnalysisResult) { latch.countDown() }
            override fun onFailure(message: String) { failRef.set(message); latch.countDown() }
        })
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue("should report failure", failRef.get() != null)
        // Only the one request; no fallback.
        assertEquals(1, server.requestCount)
    }

    @Test
    fun timeoutOrServerErrorDoesNotAutoRetry() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("busy"))
        val failRef = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        client.analyze("hi", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onSuccess(result: AnalysisResult) { latch.countDown() }
            override fun onFailure(message: String) { failRef.set(message); latch.countDown() }
        })
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(failRef.get()?.contains("503") == true)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cancellingOuterHandleAlsoCancelsFallbackCall() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"stream not supported"}}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"choices":[{"message":{"content":"{\"translation\":\"late\"}"}}]}""")
                .setBodyDelay(2, TimeUnit.SECONDS)
        )

        val terminal = CountDownLatch(1)
        val handle = client.analyze("cancel", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onSuccess(result: AnalysisResult) { terminal.countDown() }
            override fun onFailure(message: String) { terminal.countDown() }
        })
        assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)
        assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)
        handle.cancel()

        assertFalse("cancelled fallback must not call a terminal callback", terminal.await(500, TimeUnit.MILLISECONDS))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun thinkingIsDisabledByDefault() {
        val body = """{"choices":[{"message":{"content":"{\"translation\":\"ok\"}"}}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val latch = CountDownLatch(1)
        client.analyze("fast", TextLanguage.ENGLISH, snapshot(), object : DeepSeekClient.ResultCallback {
            override fun onSuccess(result: AnalysisResult) { latch.countDown() }
            override fun onFailure(message: String) { latch.countDown() }
        })

        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertTrue(request != null)
        val requestJson = org.json.JSONObject(request!!.body.readUtf8())
        assertEquals("disabled", requestJson.getJSONObject("thinking").getString("type"))
        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }
}
