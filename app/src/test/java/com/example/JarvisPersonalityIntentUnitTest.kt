package com.example

import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.intent.IntentClassifier
import com.example.jarvis.personality.JarvisPersonality
import com.example.jarvis.personality.LanguageStyle
import com.example.jarvis.provider.LocalNeuralBrainProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisPersonalityIntentUnitTest {

    @Test
    fun testGreetingsClassification() {
        val r1 = IntentClassifier.classify("Hi")
        assertEquals(ConversationIntent.GREETING, r1.intent)
        assertFalse(r1.requiresTool)

        val r2 = IntentClassifier.classify("Hello JARVIS")
        assertEquals(ConversationIntent.GREETING, r2.intent)
        assertFalse(r2.requiresTool)

        val r3 = IntentClassifier.classify("Hey")
        assertEquals(ConversationIntent.GREETING, r3.intent)
        assertFalse(r3.requiresTool)
    }

    @Test
    fun testCasualConversationClassification() {
        val r1 = IntentClassifier.classify("Kya haal hai?")
        assertEquals(ConversationIntent.CASUAL_CONVERSATION, r1.intent)
        assertFalse(r1.requiresTool)

        val r2 = IntentClassifier.classify("valo ache")
        assertEquals(ConversationIntent.CASUAL_CONVERSATION, r2.intent)
        assertFalse(r2.requiresTool)

        val r3 = IntentClassifier.classify("Kya kar raha hai?")
        assertEquals(ConversationIntent.CASUAL_CONVERSATION, r3.intent)
        assertFalse(r3.requiresTool)
    }

    @Test
    fun testDeviceInformationAndActions() {
        val r1 = IntentClassifier.classify("Battery check kar")
        assertEquals(ConversationIntent.DEVICE_INFORMATION, r1.intent)
        assertTrue(r1.requiresTool)
        assertEquals("Battery", r1.suggestedToolName)

        val r2 = IntentClassifier.classify("Torch on kar")
        assertEquals(ConversationIntent.DEVICE_ACTION, r2.intent)
        assertTrue(r2.requiresTool)
        assertEquals("Flashlight", r2.suggestedToolName)
    }

    @Test
    fun testSystemStatusExplicit() {
        val r1 = IntentClassifier.classify("System status bata")
        assertEquals(ConversationIntent.SYSTEM_STATUS, r1.intent)
        assertTrue(r1.requiresTool)
        assertEquals("Diagnostics", r1.suggestedToolName)
    }

    @Test
    fun testFileAndDocumentIntents() {
        val r1 = IntentClassifier.classify("Ek PDF bana")
        assertEquals(ConversationIntent.FILE_GENERATION, r1.intent)
        assertTrue(r1.requiresTool)
        assertEquals("FileGeneration", r1.suggestedToolName)

        val r2 = IntentClassifier.classify("Ye PDF samjha")
        assertEquals(ConversationIntent.DOCUMENT_ANALYSIS, r2.intent)
        assertTrue(r2.requiresTool)
        assertEquals("DocumentIntelligence", r2.suggestedToolName)
    }

    @Test
    fun testReminderAndClarifications() {
        val r1 = IntentClassifier.classify("Kal 8 baje yaad dilana")
        assertEquals(ConversationIntent.REMINDER_REQUEST, r1.intent)
        assertTrue(r1.requiresTool)

        val r2 = IntentClassifier.classify("Phone kar")
        assertEquals(ConversationIntent.CLARIFICATION, r2.intent)
        assertFalse(r2.requiresTool)
        assertNotNull(r2.clarificationQuestion)

        val r3 = IntentClassifier.classify("Kal yaad dilana")
        assertEquals(ConversationIntent.CLARIFICATION, r3.intent)
        assertFalse(r3.requiresTool)
        assertNotNull(r3.clarificationQuestion)
    }

    @Test
    fun testLocalDecideToolIntegrationWithIntents() {
        val tools = listOf(
            Pair("Flashlight", "Toggles LED torch"),
            Pair("Battery", "Checks battery level"),
            Pair("Diagnostics", "Runs system health check")
        )

        // Casual input must NOT trigger tools
        val hiDecision = LocalNeuralBrainProvider.decideToolLocal("Hi", tools)
        assertFalse(hiDecision.useTool)

        val haalDecision = LocalNeuralBrainProvider.decideToolLocal("Kya haal hai?", tools)
        assertFalse(haalDecision.useTool)

        val valoDecision = LocalNeuralBrainProvider.decideToolLocal("valo ache", tools)
        assertFalse(valoDecision.useTool)

        // Real action inputs MUST trigger tools
        val torchDecision = LocalNeuralBrainProvider.decideToolLocal("Torch on kar", tools)
        assertTrue(torchDecision.useTool)
        assertEquals("Flashlight", torchDecision.toolName)

        val batDecision = LocalNeuralBrainProvider.decideToolLocal("Battery check kar", tools)
        assertTrue(batDecision.useTool)
        assertEquals("Battery", batDecision.toolName)
    }

    @Test
    fun testLanguageStyleDetection() {
        assertEquals(LanguageStyle.HINGLISH, JarvisPersonality.detectLanguageStyle("Kya haal hai bhai?"))
        assertEquals(LanguageStyle.BANGLISH, JarvisPersonality.detectLanguageStyle("Kemon achis re dada? valo ache"))
        assertEquals(LanguageStyle.ENGLISH, JarvisPersonality.detectLanguageStyle("How are you doing today?"))
        assertEquals(LanguageStyle.BENGALI, JarvisPersonality.detectLanguageStyle("কেমন আছো?"))
        assertEquals(LanguageStyle.HINDI, JarvisPersonality.detectLanguageStyle("नमस्ते आप कैसे हैं?"))
    }

    @Test
    fun testNaturalConversationalResponses() {
        val hiResp = JarvisPersonality.generateConversationalResponse("Hi", ConversationIntent.GREETING, LanguageStyle.HINGLISH)
        assertTrue(hiResp.contains("bhai") || hiResp.contains("Hello"))

        val haalResp = JarvisPersonality.generateConversationalResponse("Kya haal hai?", ConversationIntent.CASUAL_CONVERSATION, LanguageStyle.HINGLISH)
        assertTrue(haalResp.contains("mast") || haalResp.contains("tu bata"))
    }

    @Test
    fun testAutonomousBrainGreetingIsNaturalAndNotRobotic() {
        val hiResponse = com.example.jarvis.provider.JarvisAutonomousBrain.generateAutonomousResponse("hi")
        assertFalse(hiResponse.contains("quantum neural"))
        assertFalse(hiResponse.contains("### 🛸"))
        assertTrue(hiResponse.contains("bhai") || hiResponse.contains("Sir") || hiResponse.contains("chal raha"))

        val helloResponse = com.example.jarvis.provider.JarvisAutonomousBrain.generateAutonomousResponse("hello")
        assertFalse(helloResponse.contains("quantum neural"))
        assertFalse(helloResponse.contains("### 🛸"))
        assertTrue(helloResponse.contains("Sir") || helloResponse.contains("Hello"))

        val kyaChalRaha = com.example.jarvis.provider.JarvisAutonomousBrain.generateAutonomousResponse("kya chal raha hai")
        assertFalse(kyaChalRaha.contains("quantum neural"))
        assertTrue(kyaChalRaha.contains("badhiya") || kyaChalRaha.contains("Sir"))
    }
}
