package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.brain.TranslationTool
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.provider.AIProvider
import com.example.jarvis.provider.ToolDecision
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.translation.FormattingPreserver
import com.example.jarvis.translation.OfflineTranslationFallback
import com.example.jarvis.translation.SupportedLanguage
import com.example.jarvis.translation.TranslationEngine
import com.example.jarvis.translation.TranslationRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisTranslationUnitTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        bridge = AndroidBridge(context)
    }

    private class MockAIProvider(
        var responseToReturn: String = "",
        var shouldThrow: Boolean = false
    ) : AIProvider {
        var lastPrompt: String = ""
        var lastSystemInstruction: String = ""

        override suspend fun generateResponse(
            prompt: String,
            systemInstruction: String,
            onChunkReceived: (String) -> Unit
        ): String {
            lastPrompt = prompt
            lastSystemInstruction = systemInstruction
            if (shouldThrow) throw RuntimeException("AI Provider temporary quota or network failure")
            return responseToReturn
        }

        override suspend fun decideTool(
            userInput: String,
            availableTools: List<Pair<String, String>>,
            contextHistory: String
        ): ToolDecision {
            return com.example.jarvis.provider.LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
        }

        override suspend fun analyzeImage(prompt: String, bitmap: android.graphics.Bitmap): String {
            return "Image analysis"
        }
    }

    // 1. ENGLISH -> BENGALI TRANSLATION
    @Test
    fun testEnglishToBengaliTranslation() = runBlocking {
        val mock = MockAIProvider(responseToReturn = "হ্যালো বিশ্ব, শুভ সকাল!")
        val request = TranslationRequest(
            sourceText = "Hello world, good morning!",
            sourceLanguage = "en",
            targetLanguage = "bn"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertEquals("bn", result.targetLanguage)
        assertEquals("হ্যালো বিশ্ব, শুভ সকাল!", result.translatedText)
        assertTrue(mock.lastPrompt.contains("Hello world, good morning!"))
    }

    // 2. BENGALI -> ENGLISH TRANSLATION
    @Test
    fun testBengaliToEnglishTranslation() = runBlocking {
        val mock = MockAIProvider(responseToReturn = "I love learning new technologies.")
        val request = TranslationRequest(
            sourceText = "আমি নতুন প্রযুক্তি শিখতে ভালোবাসি।",
            sourceLanguage = "bn",
            targetLanguage = "en"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertEquals("en", result.targetLanguage)
        assertEquals("I love learning new technologies.", result.translatedText)
    }

    // 3. HINDI -> ENGLISH TRANSLATION
    @Test
    fun testHindiToEnglishTranslation() = runBlocking {
        val mock = MockAIProvider(responseToReturn = "What is the capital of India?")
        val request = TranslationRequest(
            sourceText = "भारत की राजधानी क्या है?",
            sourceLanguage = "hi",
            targetLanguage = "en"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertEquals("en", result.targetLanguage)
        assertEquals("What is the capital of India?", result.translatedText)
    }

    // 4. AUTOMATIC SOURCE LANGUAGE DETECTION
    @Test
    fun testAutomaticSourceLanguageDetection() = runBlocking {
        assertEquals("bn", TranslationEngine.detectSourceLanguageCode("আমার সোনার বাংলা"))
        assertEquals("hi", TranslationEngine.detectSourceLanguageCode("आप कैसे हैं?"))
        assertEquals("ur", TranslationEngine.detectSourceLanguageCode("آپ کا نام کیا ہے؟"))
        assertEquals("as", TranslationEngine.detectSourceLanguageCode("অসমীয়া ভাষা ৰ ৱ"))
        assertEquals("en", TranslationEngine.detectSourceLanguageCode("Welcome to JARVIS assistant."))
    }

    // 5. EXPLICIT SOURCE AND TARGET LANGUAGE
    @Test
    fun testExplicitSourceAndTargetLanguage() = runBlocking {
        val mock = MockAIProvider(responseToReturn = "নমস্কাৰ বন্ধু")
        val request = TranslationRequest(
            sourceText = "Hello friend",
            sourceLanguage = "en",
            targetLanguage = "as"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertEquals("as", result.targetLanguage)
        assertEquals("en", result.sourceLanguage)
        assertEquals("নমস্কাৰ বন্ধু", result.translatedText)
    }

    // 6. MARKDOWN PRESERVATION
    @Test
    fun testMarkdownPreservation() = runBlocking {
        val mock = MockAIProvider(responseToReturn = "## শিরোনাম\n\nএটি একটি **গুরুত্বপূর্ণ** নোট।")
        val request = TranslationRequest(
            sourceText = "## Heading\n\nThis is an **important** note.",
            sourceLanguage = "en",
            targetLanguage = "bn"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertTrue(result.translatedText.startsWith("## "))
        assertTrue(result.translatedText.contains("**গুরুত্বপূর্ণ**"))
    }

    // 7. URL AND EMAIL PRESERVATION
    @Test
    fun testUrlAndEmailPreservation() = runBlocking {
        val raw = "Please visit https://example.com/docs or email support@domain.org for info."
        val masked = FormattingPreserver.maskFormatters(raw)

        assertTrue(masked.maskedText.contains("__URL_"))
        assertTrue(masked.maskedText.contains("__EMAIL_"))

        val mock = MockAIProvider(
            responseToReturn = "অনুগ্রহ করে __URL_0__ পরিদর্শন করুন অথবা তথ্যের জন্য __EMAIL_1__ এ ইমেল করুন।"
        )
        val request = TranslationRequest(
            sourceText = raw,
            sourceLanguage = "en",
            targetLanguage = "bn"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertTrue("URL must be preserved verbatim", result.translatedText.contains("https://example.com/docs"))
        assertTrue("Email must be preserved verbatim", result.translatedText.contains("support@domain.org"))
    }

    // 8. CODE-BLOCK AND PLACEHOLDER PRESERVATION
    @Test
    fun testCodeBlockAndPlaceholderPreservation() = runBlocking {
        val codeSnippet = "```kotlin\nval count = 42\nprintln(\"Total: \${count}\")\n```"
        val raw = "Here is the implementation:\n$codeSnippet\nHello {userName}, you have %d messages."

        val masked = FormattingPreserver.maskFormatters(raw)
        assertTrue(masked.maskedText.contains("__CODE_BLOCK_"))
        assertTrue(masked.maskedText.contains("__PLACEHOLDER_"))

        val mock = MockAIProvider(
            responseToReturn = "এখানে বাস্তবায়ন দেওয়া হলো:\n__CODE_BLOCK_0__\nহ্যালো __PLACEHOLDER_1__, আপনার __PLACEHOLDER_2__ টি বার্তা রয়েছে।"
        )
        val request = TranslationRequest(
            sourceText = raw,
            sourceLanguage = "en",
            targetLanguage = "bn"
        )
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        assertTrue("Code block must be preserved verbatim", result.translatedText.contains("val count = 42"))
        assertTrue("Placeholder {userName} must be preserved verbatim", result.translatedText.contains("{userName}"))
        assertTrue("Placeholder %d must be preserved verbatim", result.translatedText.contains("%d"))
    }

    // 9. EMPTY INPUT HANDLING
    @Test
    fun testEmptyInputHandling() = runBlocking {
        val result = TranslationEngine.translate(TranslationRequest(sourceText = "   ", targetLanguage = "bn"), null)
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("empty", ignoreCase = true))
    }

    // 10. UNSUPPORTED / OFFLINE FALLBACK HANDLING
    @Test
    fun testOfflineFallbackAndLimitationReporting() = runBlocking {
        // Deterministic greeting fallback offline
        val offlineGreeting = TranslationEngine.translate(
            TranslationRequest(sourceText = "Thank you", sourceLanguage = "en", targetLanguage = "bn"),
            aiProvider = null
        )
        assertTrue(offlineGreeting.success)
        assertEquals("ধন্যবাদ", offlineGreeting.translatedText)

        // Novel complex text offline should report clear limitation without faking
        val offlineNovel = TranslationEngine.translate(
            TranslationRequest(sourceText = "The quantum thermodynamic principle governs entropy expansion.", targetLanguage = "bn"),
            aiProvider = null
        )
        assertFalse(offlineNovel.success)
        assertTrue(offlineNovel.errorMessage!!.contains("offline", ignoreCase = true))
    }

    // 11. MALFORMED PROVIDER RESPONSE / THROWING RECOVERY
    @Test
    fun testProviderFailureHandling() = runBlocking {
        val mock = MockAIProvider(shouldThrow = true)
        val request = TranslationRequest(sourceText = "Complex unhandled sentence", targetLanguage = "bn")
        val result = TranslationEngine.translate(request, mock)

        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Translation failed"))
    }

    // 12. SENSITIVE DATA PROTECTION
    @Test
    fun testSensitiveDataProtectionNeverLeaksCredentials() = runBlocking {
        val apiKey = "AIzaSy" + "A".repeat(33) // 39 character standard Google API key
        val raw = "Here is my secret API key: $apiKey, please translate to Hindi"
        val mock = MockAIProvider(responseToReturn = "यहाँ मेरी गुप्त एपीআই कुंजी है: [REDACTED_GOOGLE_API_KEY]")

        val request = TranslationRequest(sourceText = raw, targetLanguage = "hi")
        val result = TranslationEngine.translate(request, mock)

        assertTrue(result.success)
        // Verify mock prompt never received raw credential
        assertFalse("Raw API key must never be sent to AI Provider", mock.lastPrompt.contains(apiKey))
        assertTrue(mock.lastPrompt.contains("[REDACTED_GOOGLE_API_KEY]"))
    }

    // 13. NATURAL LANGUAGE ROUTING IN AIPROVIDER
    @Test
    fun testTranslationIntentRouting() {
        val d1 = com.example.jarvis.provider.LocalNeuralBrainProvider.decideToolLocal("Translate this to Bengali: Welcome home", emptyList())
        assertTrue(d1.useTool)
        assertEquals("Translation", d1.toolName)

        val d2 = com.example.jarvis.provider.LocalNeuralBrainProvider.decideToolLocal("এটা ইংরেজিতে অনুবাদ করো: আমি ভালো আছি", emptyList())
        assertTrue(d2.useTool)
        assertEquals("Translation", d2.toolName)

        val d3 = com.example.jarvis.provider.LocalNeuralBrainProvider.decideToolLocal("Isko Hindi mein translate karo: Good morning", emptyList())
        assertTrue(d3.useTool)
        assertEquals("Translation", d3.toolName)

        val d4 = com.example.jarvis.provider.LocalNeuralBrainProvider.decideToolLocal("What does this mean in Hindi? Hello", emptyList())
        assertTrue(d4.useTool)
        assertEquals("Translation", d4.toolName)
    }

    // 14. TRANSLATION TOOL EXECUTION & ISOLATION
    @Test
    fun testTranslationToolExecutionIsolation() = runBlocking {
        val mock = MockAIProvider(responseToReturn = "নমস্কার বন্ধু")
        val tool = TranslationTool()
        val toolContext = ToolContext(
            repository = repository,
            bridge = bridge,
            aiProvider = mock
        )

        val result = tool.execute("Translate to Bengali: The galaxy contains billions of stars", toolContext)
        assertTrue(result.success)
        assertTrue(result.output.contains("নমস্কার বন্ধু"))
        assertFalse("Translation tool must not require user clearance confirmation", result.requiresUserAction)

        // Verify that memory was not automatically modified by translation
        val currentMemories = repository.memories.value
        assertFalse("Translation output must not be automatically written to permanent memory", currentMemories.any { it.content.contains("নমস্কার") })
    }
}
