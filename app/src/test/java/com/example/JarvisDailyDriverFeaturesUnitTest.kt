package com.example

import com.example.jarvis.brain.CalendarTool
import com.example.jarvis.brain.SmsMessagingTool
import com.example.jarvis.brain.WhatsAppMessagingTool
import com.example.jarvis.provider.LocalNeuralBrainProvider
import com.example.jarvis.service.SmartBatteryMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisDailyDriverFeaturesUnitTest {

    @Test
    fun testAiProviderRoutesWhatsAppQuery() {
        val tools = listOf(
            "WhatsAppMessage" to "Send whatsapp message",
            "SmsMessage" to "Send SMS text",
            "CalendarEvent" to "Calendar schedule"
        )
        val decision = LocalNeuralBrainProvider.decideToolLocal("send whatsapp to Rahul: 10 minute mein pahunch raha hoon", tools)
        assertTrue(decision.useTool)
        assertEquals("WhatsAppMessage", decision.toolName)
    }

    @Test
    fun testAiProviderRoutesSmsQuery() {
        val tools = listOf(
            "WhatsAppMessage" to "Send whatsapp message",
            "SmsMessage" to "Send SMS text",
            "CalendarEvent" to "Calendar schedule"
        )
        val decision = LocalNeuralBrainProvider.decideToolLocal("send sms to 9876543210: meeting started", tools)
        assertTrue(decision.useTool)
        assertEquals("SmsMessage", decision.toolName)
    }

    @Test
    fun testAiProviderRoutesCalendarQuery() {
        val tools = listOf(
            "WhatsAppMessage" to "Send whatsapp message",
            "SmsMessage" to "Send SMS text",
            "CalendarEvent" to "Calendar schedule"
        )
        val decision = LocalNeuralBrainProvider.decideToolLocal("schedule meeting with client tomorrow", tools)
        assertTrue(decision.useTool)
        assertEquals("CalendarEvent", decision.toolName)
    }

    @Test
    fun testToolsMetadata() {
        val wa = WhatsAppMessagingTool()
        assertEquals("WhatsAppMessage", wa.name)

        val sms = SmsMessagingTool()
        assertEquals("SmsMessage", sms.name)
        assertTrue(sms.permissions.contains(android.Manifest.permission.SEND_SMS))

        val cal = CalendarTool()
        assertEquals("CalendarEvent", cal.name)
        assertTrue(cal.permissions.contains(android.Manifest.permission.READ_CALENDAR))
        assertTrue(cal.permissions.contains(android.Manifest.permission.WRITE_CALENDAR))
    }

    @Test
    fun testBatteryAlertState() {
        val state = SmartBatteryMonitor.BatteryState(
            percent = 100,
            isCharging = true,
            isFull = true
        )
        assertEquals(100, state.percent)
        assertTrue(state.isCharging)
        assertTrue(state.isFull)
    }
}
