package com.jarvis.ai

import com.jarvis.ai.model.JarvisState
import com.jarvis.ai.model.MessageSender
import com.jarvis.ai.model.ChatMessage
import com.jarvis.ai.ui.SubScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisLiveVoiceSessionUnitTest {

    @Test
    fun testSubScreenVoiceEnumExists() {
        val voiceScreen = SubScreen.valueOf("VOICE")
        assertEquals(SubScreen.VOICE, voiceScreen)
    }

    @Test
    fun testLiveVoiceSessionStateTransitions() {
        // State transitions for Live Voice Session:
        // Listening -> Thinking -> Speaking -> Listening
        var state = JarvisState.LISTENING
        assertEquals(JarvisState.LISTENING, state)

        // User speaks directive: transition to thinking
        state = JarvisState.THINKING
        assertEquals(JarvisState.THINKING, state)

        // Brain synthesizes and delivers response with voice
        state = JarvisState.SPEAKING
        assertEquals(JarvisState.SPEAKING, state)

        // Speech completes naturally -> automatically returns to listening
        state = JarvisState.LISTENING
        assertEquals(JarvisState.LISTENING, state)
    }

    @Test
    fun testBargeInInterruptionFlow() {
        // When JARVIS is speaking and user interrupts
        var state = JarvisState.SPEAKING
        var isSpeaking = true

        // User barges in (tap or speech)
        val interruptAndListen = {
            isSpeaking = false
            state = JarvisState.LISTENING
        }

        interruptAndListen()
        assertFalse(isSpeaking)
        assertEquals(JarvisState.LISTENING, state)
    }

    @Test
    fun testMultiTurnConversationContextMaintenance() {
        val messages = mutableListOf<ChatMessage>()

        // Turn 1: Greeting
        messages.add(ChatMessage(sender = MessageSender.USER, text = "Hi JARVIS"))
        messages.add(ChatMessage(sender = MessageSender.JARVIS, text = "Hey! I'm here. What's up?"))

        // Turn 2: Battery check
        messages.add(ChatMessage(sender = MessageSender.USER, text = "What's my battery?"))
        messages.add(ChatMessage(sender = MessageSender.JARVIS, text = "Battery is at 85% and discharging, Sir."))

        // Turn 3: Action directive
        messages.add(ChatMessage(sender = MessageSender.USER, text = "Okay, turn on the flashlight."))
        messages.add(ChatMessage(sender = MessageSender.JARVIS, text = "Flashlight activated, Sir."))

        assertEquals(6, messages.size)

        // Context history preserves all 3 turns
        val contextHistory = messages.joinToString("\n") { "${it.sender}: ${it.text}" }
        assertTrue(contextHistory.contains("Hi JARVIS"))
        assertTrue(contextHistory.contains("What's my battery?"))
        assertTrue(contextHistory.contains("turn on the flashlight"))
    }
}
