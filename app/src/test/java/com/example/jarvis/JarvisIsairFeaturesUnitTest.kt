package com.example.jarvis

import com.example.jarvis.brain.CompoundTaskPlanner
import com.example.jarvis.memory.KnowledgeGraphEngine
import com.example.jarvis.memory.RecallGatingEngine
import com.example.jarvis.personality.JarvisPersonality
import com.example.jarvis.personality.LanguageStyle
import com.example.jarvis.security.PrivacyRedactionGuard
import com.example.jarvis.voice.EchoDetectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisIsairFeaturesUnitTest {

    @Test
    fun testPrivacyRedactionGuard_scrubsSensitiveData() {
        val rawInput = "My key is sk-1234567890abcdef1234567890abcdef12 and auth Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-ae7nit9P6alC34"
        val redacted = PrivacyRedactionGuard.redact(rawInput)

        assertFalse(redacted.contains("sk-1234567890"))
        assertTrue(redacted.contains("[REDACTED_OPENAI_KEY]"))
        assertTrue(redacted.contains("[REDACTED_JWT]"))

        val rawCreditCard = "Payment card is 4532-1234-5678-9010 for billing"
        val cardRedacted = PrivacyRedactionGuard.redact(rawCreditCard)
        assertTrue(cardRedacted.contains("[REDACTED_CARD]"))
        assertFalse(cardRedacted.contains("4532-1234-5678-9010"))
    }

    @Test
    fun testEchoDetectionEngine_filtersSelfFeedback() {
        val echoEngine = EchoDetectionEngine()
        val speech = "All systems operational, Sir. How may I assist?"

        echoEngine.notifyTtsStarted(speech)

        // Same text recognized while TTS is speaking should be rejected
        assertTrue(echoEngine.shouldRejectAsEcho("All systems operational, Sir. How may I assist?"))

        // Slightly noisy acoustic transcription should also be rejected
        assertTrue(echoEngine.shouldRejectAsEcho("All systems operational Sir How may I assist"))

        // Completely new command from user must NOT be rejected
        assertFalse(echoEngine.shouldRejectAsEcho("Turn on flashlight and check weather"))

        echoEngine.notifyTtsFinished()
    }

    @Test
    fun testCompoundTaskPlanner_detectsAndDecomposes() {
        val compound1 = "Turn on flashlight and set volume to 80"
        assertTrue(CompoundTaskPlanner.isCompoundQuery(compound1))

        val steps = CompoundTaskPlanner.decomposeIntoSubSteps(compound1)
        assertTrue(steps.size >= 2)

        val hinglishCompound = "Torch on karo aur volume 50 percent karo"
        assertTrue(CompoundTaskPlanner.isCompoundQuery(hinglishCompound))
        val hinglishSteps = CompoundTaskPlanner.decomposeIntoSubSteps(hinglishCompound)
        assertTrue(hinglishSteps.size >= 2)

        val simpleQuery = "What is the time?"
        assertFalse(CompoundTaskPlanner.isCompoundQuery(simpleQuery))
    }

    @Test
    fun testKnowledgeGraphEngine_extractsFacts() {
        KnowledgeGraphEngine.extractFactsFromConversation("My name is Murselim")
        KnowledgeGraphEngine.extractFactsFromConversation("I am working on Jarvis Android project")
        KnowledgeGraphEngine.extractFactsFromConversation("I like dark chocolate")

        val digest = KnowledgeGraphEngine.getRelevantKnowledgeDigest("Who am I and what is my project?")
        assertTrue(digest != null)
        assertTrue(digest!!.contains("Murselim") || digest.contains("Jarvis"))
    }

    @Test
    fun testRecallGatingEngine_filtersGreetings() {
        assertFalse(RecallGatingEngine.shouldRecall("hi", emptyList()))
        assertFalse(RecallGatingEngine.shouldRecall("hello", emptyList()))
        assertTrue(RecallGatingEngine.shouldRecall("what do you remember about my project", emptyList()))
    }

    @Test
    fun testJarvisPersonality_detectsLanguageAndGeneratesStarkVoice() {
        assertEquals(LanguageStyle.HINGLISH, JarvisPersonality.detectLanguageStyle("Torch on karo bhai"))
        assertEquals(LanguageStyle.BANGLISH, JarvisPersonality.detectLanguageStyle("Kemon acho dada sob thik ache?"))
        assertEquals(LanguageStyle.BENGALI, JarvisPersonality.detectLanguageStyle("কেমন আছেন স্যার"))
        assertEquals(LanguageStyle.HINDI, JarvisPersonality.detectLanguageStyle("नमस्ते आप कैसे हैं"))

        val systemPrompt = JarvisPersonality.getSystemPrompt(LanguageStyle.HINGLISH, "User fact: developer")
        assertTrue(systemPrompt.contains("JARVIS"))
        assertTrue(systemPrompt.contains("Stark"))
        assertTrue(systemPrompt.contains("developer"))
    }
}
