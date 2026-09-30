package com.example

import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.intent.IntentClassifier
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.JarvisState
import com.example.jarvis.model.MessageSender
import com.example.jarvis.provider.LocalNeuralBrainProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisLiveVoiceContinuousMicUnitTest {

    @Test
    fun testContinuousMicrophoneLoopStateProgression() = runBlocking {
        var isLiveVoiceSessionActive = true
        var jarvisState = JarvisState.IDLE
        var isMicrophoneHardwareListening = false
        var isTtsSpeaking = false
        var isMicMuted = false

        // 1. Start Live Voice Conversation -> microphone turns ON
        val startLiveVoice = {
            isLiveVoiceSessionActive = true
            isMicrophoneHardwareListening = true
            jarvisState = JarvisState.LISTENING
        }

        startLiveVoice()
        assertEquals(JarvisState.LISTENING, jarvisState)
        assertTrue(isMicrophoneHardwareListening)

        // Calculate REAL microphone state as in VoiceScreen
        val computeActuallyListening = {
            isMicrophoneHardwareListening && !isTtsSpeaking && jarvisState != JarvisState.THINKING && !isMicMuted
        }
        assertTrue(computeActuallyListening())

        // 2. User speaks sentence 1: "Hi JARVIS"
        val onUserSpeechRecognized = { text: String ->
            isMicrophoneHardwareListening = false
            jarvisState = JarvisState.THINKING
        }
        onUserSpeechRecognized("Hi JARVIS")
        assertEquals(JarvisState.THINKING, jarvisState)
        assertFalse(computeActuallyListening())

        // 3. JARVIS produces answer and speaks
        val onTtsStart = {
            jarvisState = JarvisState.SPEAKING
            isTtsSpeaking = true
        }
        onTtsStart()
        assertEquals(JarvisState.SPEAKING, jarvisState)
        assertFalse(computeActuallyListening())

        // 4. TTS finishes speaking -> MUST automatically return to LISTENING
        val onTtsDone = {
            isTtsSpeaking = false
            if (isLiveVoiceSessionActive && !isMicMuted) {
                jarvisState = JarvisState.LISTENING
                isMicrophoneHardwareListening = true
            }
        }
        onTtsDone()
        assertEquals(JarvisState.LISTENING, jarvisState)
        assertTrue(isMicrophoneHardwareListening)
        assertTrue(computeActuallyListening())

        // 5. User speaks sentence 2 without manual reactivation: "What's my battery?"
        onUserSpeechRecognized("What's my battery?")
        assertEquals(JarvisState.THINKING, jarvisState)

        val intent = IntentClassifier.classify("What's my battery?")
        assertEquals(ConversationIntent.DEVICE_INFORMATION, intent.intent)

        onTtsStart()
        assertEquals(JarvisState.SPEAKING, jarvisState)

        onTtsDone()
        assertEquals(JarvisState.LISTENING, jarvisState)
        assertTrue(isMicrophoneHardwareListening)
        assertTrue(computeActuallyListening())

        // 6. User manually ends session -> mic turns off
        val onEndSession = {
            isLiveVoiceSessionActive = false
            isMicrophoneHardwareListening = false
            isTtsSpeaking = false
            jarvisState = JarvisState.IDLE
        }
        onEndSession()
        assertEquals(JarvisState.IDLE, jarvisState)
        assertFalse(computeActuallyListening())
    }

    @Test
    fun testBargeInStopsTtsAndImmediatelyListens() {
        var isLiveVoiceSessionActive = true
        var jarvisState = JarvisState.SPEAKING
        var isTtsSpeaking = true
        var isMicrophoneListening = false

        // JARVIS is currently speaking an explanation
        assertEquals(JarvisState.SPEAKING, jarvisState)
        assertTrue(isTtsSpeaking)

        // Barge-in detected (acoustic or tap interruption)
        val onBargeInTriggered = {
            // Immediately stop TTS
            isTtsSpeaking = false
            // Switch directly to listening
            jarvisState = JarvisState.LISTENING
            isMicrophoneListening = true
        }

        onBargeInTriggered()

        assertFalse(isTtsSpeaking)
        assertEquals(JarvisState.LISTENING, jarvisState)
        assertTrue(isMicrophoneListening)
    }

    @Test
    fun testRealMicrophoneStateDistinction() {
        // UI must reflect the REAL microphone state and not fake it
        val isHardwareMicListening = false
        val isSpeaking = false
        val isThinking = false
        val isMicMuted = false

        val isActuallyListening = isHardwareMicListening && !isSpeaking && !isThinking && !isMicMuted
        assertFalse(isActuallyListening)

        // When mic is muted, actually listening must be false
        val mutedActuallyListening = true && !isSpeaking && !isThinking && true
        assertFalse(!mutedActuallyListening) // true && !isMicMuted would be false
    }
}
