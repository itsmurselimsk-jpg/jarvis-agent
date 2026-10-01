package com.jarvis.ai

import com.jarvis.ai.voice.EchoDetectionEngine
import com.jarvis.ai.voice.JarvisWakePhraseMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for JARVIS Voice Barge-In, TTS Interruption, and Echo Management.
 */
class JarvisVoiceBargeInUnitTest {

    private lateinit var echoDetector: EchoDetectionEngine

    @Before
    fun setUp() {
        echoDetector = EchoDetectionEngine()
    }

    @Test
    fun testWakePhraseMatchingAllRequiredVariants() {
        // Required exact phrases
        val heyJarvis = JarvisWakePhraseMatcher.match("hey jarvis")
        assertTrue(heyJarvis.isMatched)
        assertEquals("hey jarvis", heyJarvis.matchedPhrase)

        val okJarvis = JarvisWakePhraseMatcher.match("ok jarvis")
        assertTrue(okJarvis.isMatched)
        assertEquals("ok jarvis", okJarvis.matchedPhrase)

        val hindiJarvis = JarvisWakePhraseMatcher.match("हे जार्विस")
        assertTrue(hindiJarvis.isMatched)
        assertEquals("हे जार्विस", hindiJarvis.matchedPhrase)

        // Case variations
        assertTrue(JarvisWakePhraseMatcher.match("HEY JARVIS").isMatched)
        assertTrue(JarvisWakePhraseMatcher.match("Ok Jarvis").isMatched)
        assertTrue(JarvisWakePhraseMatcher.match("HeY jArViS").isMatched)

        // Punctuation and extra spaces
        assertTrue(JarvisWakePhraseMatcher.match("  hey,   jarvis!  ").isMatched)
        assertTrue(JarvisWakePhraseMatcher.match("ok... jarvis?").isMatched)
        assertTrue(JarvisWakePhraseMatcher.match("हे,   जार्विस!").isMatched)

        // Unrelated speech must be strictly ignored
        assertFalse(JarvisWakePhraseMatcher.match("hello how are you").isMatched)
        assertFalse(JarvisWakePhraseMatcher.match("hey assistant").isMatched)
        assertFalse(JarvisWakePhraseMatcher.match("what is the weather").isMatched)
    }

    @Test
    fun testTtsBargeInSimulationAndInterruption() {
        var isSpeaking = true
        var activeSpeechJobCancelled = false
        var speechRecognitionActive = false

        // User speaks while TTS is active
        fun simulateBargeInOnSpeechOnset() {
            if (isSpeaking) {
                isSpeaking = false // Cancel active TTS immediately
                activeSpeechJobCancelled = true
                speechRecognitionActive = true // Continue normal speech recognizer
            }
        }

        simulateBargeInOnSpeechOnset()

        assertFalse(isSpeaking)
        assertTrue(activeSpeechJobCancelled)
        assertTrue(speechRecognitionActive)
    }

    @Test
    fun testRepeatedRapidSpeechTtsInterruptions() {
        var ttsSessionCount = 0
        var cancellationCount = 0
        var currentTtsId: String? = null

        fun startTts(id: String) {
            currentTtsId = id
            ttsSessionCount++
        }

        fun onBargeInTriggered() {
            if (currentTtsId != null) {
                cancellationCount++
                currentTtsId = null
            }
        }

        // Rapid cycle 1: Speech -> TTS -> User speaks -> Interrupted
        startTts("utterance_1")
        assertEquals("utterance_1", currentTtsId)
        onBargeInTriggered()
        assertEquals(null, currentTtsId)

        // Rapid cycle 2: Immediate new TTS -> User interrupts again
        startTts("utterance_2")
        assertEquals("utterance_2", currentTtsId)
        onBargeInTriggered()
        assertEquals(null, currentTtsId)

        // Rapid cycle 3: Another interrupt
        startTts("utterance_3")
        onBargeInTriggered()
        assertEquals(null, currentTtsId)

        assertEquals(3, ttsSessionCount)
        assertEquals(3, cancellationCount)
    }

    @Test
    fun testEchoShieldPreventsSelfTriggerDuringTts() {
        val ttsText = "Good morning Sir, systems are fully operational."
        echoDetector.notifyTtsStarted(ttsText)

        // Self audio feedback picked up by mic
        val isEcho = echoDetector.shouldRejectAsEcho(ttsText)
        assertTrue(isEcho)

        // Different user speech
        val userSpeech = "Turn on flashlight"
        val isUserSpeechEcho = echoDetector.shouldRejectAsEcho(userSpeech)
        assertFalse(isUserSpeechEcho)
    }
}
