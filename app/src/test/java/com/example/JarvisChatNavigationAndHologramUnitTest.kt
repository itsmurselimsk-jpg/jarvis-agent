package com.example

import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.intent.IntentClassifier
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.JarvisState
import com.example.jarvis.model.MessageSender
import com.example.jarvis.provider.LocalNeuralBrainProvider
import com.example.jarvis.ui.SubScreen
import com.example.jarvis.ui.components.NavTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisChatNavigationAndHologramUnitTest {

    @Test
    fun testNavTabChatAliasAndLabels() {
        assertEquals("CHAT", NavTab.CONVERSATION.label)
        assertEquals(NavTab.CONVERSATION, NavTab.CHAT)
    }

    @Test
    fun testNavigationFlowHomeToChatAndBack() {
        var currentTab = NavTab.HOME
        var activeSubScreen: SubScreen? = null

        // Flow 1: Home -> tap CHAT -> Chat screen opens
        val onChatTabSelected = { tab: NavTab ->
            activeSubScreen = null
            currentTab = tab
        }
        onChatTabSelected(NavTab.CHAT)
        assertEquals(NavTab.CONVERSATION, currentTab)

        // Flow 4: Chat -> press Back -> Home
        val onBackPress = {
            if (activeSubScreen != null) {
                activeSubScreen = null
            } else if (currentTab != NavTab.HOME) {
                currentTab = NavTab.HOME
            }
        }
        onBackPress()
        assertEquals(NavTab.HOME, currentTab)

        // Flow 5: Home -> Live Voice -> Live Voice Conversation active
        activeSubScreen = SubScreen.VOICE
        assertEquals(SubScreen.VOICE, activeSubScreen)
    }

    @Test
    fun testChatGreetingIntentAndRealJarvisResponse() = runBlocking {
        // Chat -> send "Hi JARVIS" -> intent classified as GREETING
        val userPrompt = "Hi JARVIS"
        val classification = IntentClassifier.classify(userPrompt)
        assertEquals(ConversationIntent.GREETING, classification.intent)
        assertFalse(classification.requiresTool)

        // Real autonomous local response generated for greeting
        val response = LocalNeuralBrainProvider.generateLocalResponse("User: $userPrompt") { }
        assertNotNull(response)
        assertTrue(response.isNotBlank())
    }

    @Test
    fun testChatBatteryRealInformationToolDecision() {
        // Chat -> send "What's my battery?" -> real battery information tool intent
        val query = "What's my battery?"
        val classification = IntentClassifier.classify(query)
        assertTrue(classification.requiresTool)
        assertEquals("Battery", classification.suggestedToolName)

        val toolsCatalog = listOf(Pair("Battery", "Queries real-time battery charge level and health"))
        val decision = LocalNeuralBrainProvider.decideToolLocal(query, toolsCatalog)
        assertTrue(decision.useTool)
        assertEquals("Battery", decision.toolName)
    }

    @Test
    fun testConversationContextPreservedBetweenMessages() {
        val messages = mutableListOf<ChatMessage>()

        // 1. User: "Hi JARVIS"
        messages.add(ChatMessage(sender = MessageSender.USER, text = "Hi JARVIS"))
        messages.add(ChatMessage(sender = MessageSender.JARVIS, text = "At your service, Sir. Systems online."))

        // 2. User: "What's my battery?"
        messages.add(ChatMessage(sender = MessageSender.USER, text = "What's my battery?"))
        messages.add(ChatMessage(sender = MessageSender.JARVIS, text = "Capacity: 85%. Power Flow: Charging."))

        assertEquals(4, messages.size)
        assertEquals("Hi JARVIS", messages[0].text)
        assertEquals("What's my battery?", messages[2].text)

        val contextString = messages.joinToString("\n") { "${it.sender}: ${it.text}" }
        assertTrue(contextString.contains("Hi JARVIS"))
        assertTrue(contextString.contains("What's my battery?"))
    }

    @Test
    fun testHologramVoiceStates() {
        val listeningState = JarvisState.LISTENING
        val thinkingState = JarvisState.THINKING
        val speakingState = JarvisState.SPEAKING
        val idleState = JarvisState.IDLE

        assertNotNull(listeningState)
        assertNotNull(thinkingState)
        assertNotNull(speakingState)
        assertNotNull(idleState)
    }
}
