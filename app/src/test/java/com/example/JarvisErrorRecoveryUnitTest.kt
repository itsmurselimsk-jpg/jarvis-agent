package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.Tool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.brain.ToolResult
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.recovery.ErrorCategory
import com.example.jarvis.recovery.ErrorClassifier
import com.example.jarvis.recovery.ResponseRepair
import com.example.jarvis.recovery.RetryPolicy
import com.example.jarvis.recovery.ToolRecovery
import com.example.jarvis.recovery.executeWithRetry
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisErrorRecoveryUnitTest {

    @Test
    fun testSuccessfulOperationWithoutRetry() = runBlocking {
        var attempts = 0
        val policy = RetryPolicy(maxAttempts = 3, initialBackoffMs = 10L, maxBackoffMs = 50L)

        val result = executeWithRetry(policy = policy, operationName = "TestSuccess") {
            attempts++
            "JARVIS Core Nominal"
        }

        assertTrue(result.isSuccess)
        assertEquals("JARVIS Core Nominal", result.getOrNull())
        assertEquals(1, attempts)
    }

    @Test
    fun testTransientNetworkFailureThenSuccess() = runBlocking {
        var attempts = 0
        val policy = RetryPolicy(maxAttempts = 3, initialBackoffMs = 10L, maxBackoffMs = 50L)

        val result = executeWithRetry(policy = policy, operationName = "TestNetworkRetry") {
            attempts++
            if (attempts == 1) {
                throw UnknownHostException("Unable to resolve host generativelanguage.googleapis.com")
            }
            "Telemetry Link Restored"
        }

        assertTrue(result.isSuccess)
        assertEquals("Telemetry Link Restored", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun testTimeoutThenSuccess() = runBlocking {
        var attempts = 0
        val policy = RetryPolicy(maxAttempts = 3, initialBackoffMs = 10L, maxBackoffMs = 50L)

        val result = executeWithRetry(policy = policy, operationName = "TestTimeoutRetry") {
            attempts++
            if (attempts == 1) {
                throw SocketTimeoutException("Read timed out on socket 443")
            }
            "Payload Delivered"
        }

        assertTrue(result.isSuccess)
        assertEquals("Payload Delivered", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun testProviderFailureAfterMaxRetries() = runBlocking {
        var attempts = 0
        val max = 3
        val policy = RetryPolicy(maxAttempts = max, initialBackoffMs = 10L, maxBackoffMs = 50L)

        val result = executeWithRetry(policy = policy, operationName = "TestExhaustion") {
            attempts++
            throw IOException("503 Service Temporarily Overloaded")
        }

        assertTrue(result.isFailure)
        assertEquals(max, attempts)
    }

    @Test
    fun testRateLimitHandlingDoesNotRetry() = runBlocking {
        var attempts = 0
        val policy = RetryPolicy(maxAttempts = 3, initialBackoffMs = 10L, maxBackoffMs = 50L)

        val result = executeWithRetry(policy = policy, operationName = "TestRateLimit") {
            attempts++
            throw Exception("HTTP 429: RESOURCE_EXHAUSTED. Quota exceeded for quota metric.")
        }

        assertTrue(result.isFailure)
        // Must abort immediately on attempt 1 to prevent hammering the provider
        assertEquals(1, attempts)

        val classified = ErrorClassifier.classify(result.exceptionOrNull()!!)
        assertEquals(ErrorCategory.RATE_LIMIT, classified.category)
        assertFalse(classified.isRetryable)
        assertTrue(classified.isRecoverable)
    }

    @Test
    fun testPermanentErrorWithoutRetry() = runBlocking {
        var attempts = 0
        val policy = RetryPolicy(maxAttempts = 3, initialBackoffMs = 10L, maxBackoffMs = 50L)

        val result = executeWithRetry(policy = policy, operationName = "TestAuthError") {
            attempts++
            throw Exception("HTTP 401 Unauthorized: Invalid API key provided.")
        }

        assertTrue(result.isFailure)
        assertEquals(1, attempts)

        val classified = ErrorClassifier.classify(result.exceptionOrNull()!!)
        assertEquals(ErrorCategory.AUTH, classified.category)
        assertFalse(classified.isRetryable)
        assertFalse(classified.isRecoverable)
    }

    @Test
    fun testCancellationDuringRetry() = runBlocking {
        var attempts = 0
        val policy = RetryPolicy(maxAttempts = 5, initialBackoffMs = 100L, maxBackoffMs = 500L)

        val job = launch {
            executeWithRetry(policy = policy, operationName = "TestCancellation") {
                attempts++
                throw IOException("Network dropped")
            }
        }

        // Allow attempt 1 to execute and enter delay
        kotlinx.coroutines.delay(20)
        job.cancel()
        job.join()

        // Cancellation must have stopped retries immediately
        assertTrue(attempts < 5)
    }

    @Test
    fun testMalformedAiResponseRepair() {
        // 1. Markdown code fences ```json ... ```
        val markdownWrapped = """
            ```json
            {
                "useTool": true,
                "toolName": "Battery",
                "toolInput": "check battery",
                "reasoning": "User requested battery charge status"
            }
            ```
        """.trimIndent()

        val parsedMarkdown = ResponseRepair.parseToolDecisionWithRepair(markdownWrapped, "check battery")
        assertNotNull(parsedMarkdown)
        assertTrue(parsedMarkdown!!.useTool)
        assertEquals("Battery", parsedMarkdown.toolName)

        // 2. Conversational preamble
        val conversationalPreamble = """
            Certainly, Sir. Here is the selected command:
            {"useTool": true, "toolName": "Flashlight", "toolInput": "turn on flashlight", "reasoning": "toggle torch"}
        """.trimIndent()

        val parsedPreamble = ResponseRepair.parseToolDecisionWithRepair(conversationalPreamble, "turn on flashlight")
        assertNotNull(parsedPreamble)
        assertTrue(parsedPreamble!!.useTool)
        assertEquals("Flashlight", parsedPreamble.toolName)

        // 3. Completely unparseable garbage returns null safely without throwing
        val broken = "Sorry, I encountered an internal provider hallucination."
        val parsedBroken = ResponseRepair.parseToolDecisionWithRepair(broken, "hello")
        assertNull(parsedBroken)
    }

    @Test
    fun testReadOnlyToolRecovery() = runBlocking {
        var executionCount = 0

        val readOnlyTool = object : Tool {
            override val name = "Battery"
            override val description = "Checks battery percentage"
            override val riskLevel = RiskLevel.SAFE
            override val permissions: List<String> = emptyList()

            override suspend fun execute(input: String, context: ToolContext): ToolResult {
                executionCount++
                if (executionCount == 1) {
                    throw IOException("Hardware sensor bus busy")
                }
                return ToolResult(success = true, output = "Battery at 85%", verified = true)
            }

            override suspend fun verify(result: ToolResult, context: ToolContext): Boolean = true
        }

        assertTrue(ToolRecovery.isReadOnlyOrIdempotent(readOnlyTool, "check battery"))

        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val dummyContext = ToolContext(
            repository = JarvisRepository(appContext),
            bridge = AndroidBridge(appContext)
        )

        val result = ToolRecovery.executeSafely(readOnlyTool, "check battery", dummyContext)
        assertTrue(result.success)
        assertEquals("Battery at 85%", result.output)
        assertEquals(2, executionCount)
    }

    @Test
    fun testStateChangingToolNotDuplicated() = runBlocking {
        var executionCount = 0

        val destructiveTool = object : Tool {
            override val name = "PhoneCall"
            override val description = "Dials phone call"
            override val riskLevel = RiskLevel.CONFIRMATION
            override val permissions: List<String> = emptyList()

            override suspend fun execute(input: String, context: ToolContext): ToolResult {
                executionCount++
                throw SecurityException("Telephony permission interrupted")
            }

            override suspend fun verify(result: ToolResult, context: ToolContext): Boolean = false
        }

        assertFalse(ToolRecovery.isReadOnlyOrIdempotent(destructiveTool, "call John"))

        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val dummyContext = ToolContext(
            repository = JarvisRepository(appContext),
            bridge = AndroidBridge(appContext)
        )

        val result = ToolRecovery.executeSafely(destructiveTool, "call John", dummyContext)
        // Must NOT succeed
        assertFalse(result.success)
        // Must execute EXACTLY once, never retried or duplicated
        assertEquals(1, executionCount)
    }

    @Test
    fun testUserSafeErrorMessage() {
        val rawNpe = NullPointerException("Null reference in internal com.example.jarvis.bridge.SecretCore")
        val classified = ErrorClassifier.classify(rawNpe)

        // Must not expose raw stack or internal class names in user safe text
        assertFalse(classified.userSafeMessage.contains("NullPointerException"))
        assertFalse(classified.userSafeMessage.contains("SecretCore"))
        assertTrue(classified.userSafeMessage.isNotBlank())
    }

    @Test
    fun testNoSensitiveDataLeakageInErrorsAndLogging() {
        val sensitiveMsg = "Error at https://generativelanguage.googleapis.com?key=AIzaSyD-1234567890abcdefghijklmnopqr with secret Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz"
        val ex = IOException(sensitiveMsg)
        val classified = ErrorClassifier.classify(ex)

        // API Key must be redacted
        assertFalse(classified.technicalMessage.contains("AIzaSyD-1234567890abcdefghijklmnopqr"))
        // Bearer token must be redacted
        assertFalse(classified.technicalMessage.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        // User safe message must not contain any of it
        assertFalse(classified.userSafeMessage.contains("AIzaSyD"))
    }
}
