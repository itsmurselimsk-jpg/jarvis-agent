package com.example

import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.intent.IntentClassifier
import com.example.jarvis.overlay.FloatingArcOrbService
import com.example.jarvis.protocol.StarkProtocolEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisStarkProtocolsUnitTest {

    @Test
    fun testStarkProtocolTypeResolution() {
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.MORNING,
            StarkProtocolEngine.StarkProtocolType.fromInput("JARVIS, execute morning protocol")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.MORNING,
            StarkProtocolEngine.StarkProtocolType.fromInput("Good morning jarvis briefing")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.NIGHT,
            StarkProtocolEngine.StarkProtocolType.fromInput("JARVIS, night protocol")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.NIGHT,
            StarkProtocolEngine.StarkProtocolType.fromInput("Sleep protocol, so jao")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.SECURE_PERIMETER,
            StarkProtocolEngine.StarkProtocolType.fromInput("JARVIS, secure perimeter")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.SECURE_PERIMETER,
            StarkProtocolEngine.StarkProtocolType.fromInput("Security lockdown protocol")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.POWER_SURGE,
            StarkProtocolEngine.StarkProtocolType.fromInput("Overclock reactor to maximum")
        )
        assertEquals(
            StarkProtocolEngine.StarkProtocolType.POWER_SURGE,
            StarkProtocolEngine.StarkProtocolType.fromInput("Power surge protocol")
        )
    }

    @Test
    fun testIntentClassifierStarkProtocolsRouting() {
        val morningResult = IntentClassifier.classify("JARVIS, start morning protocol")
        assertEquals(ConversationIntent.DEVICE_ACTION, morningResult.intent)
        assertTrue(morningResult.requiresTool)
        assertEquals("StarkProtocol", morningResult.suggestedToolName)

        val nightResult = IntentClassifier.classify("JARVIS, night protocol please")
        assertEquals(ConversationIntent.DEVICE_ACTION, nightResult.intent)
        assertTrue(nightResult.requiresTool)
        assertEquals("StarkProtocol", nightResult.suggestedToolName)

        val perimeterResult = IntentClassifier.classify("JARVIS, secure perimeter now")
        assertEquals(ConversationIntent.DEVICE_ACTION, perimeterResult.intent)
        assertTrue(perimeterResult.requiresTool)
        assertEquals("StarkProtocol", perimeterResult.suggestedToolName)

        val surgeResult = IntentClassifier.classify("JARVIS, power surge")
        assertEquals(ConversationIntent.DEVICE_ACTION, surgeResult.intent)
        assertTrue(surgeResult.requiresTool)
        assertEquals("StarkProtocol", surgeResult.suggestedToolName)
    }

    @Test
    fun testFloatingArcOrbStateFlowInitial() {
        assertNotNull(FloatingArcOrbService.isOrbRunning)
        assertFalse(FloatingArcOrbService.isOrbRunning.value)
        assertEquals("jarvis_orb_channel", FloatingArcOrbService.CHANNEL_ID)
    }
}
