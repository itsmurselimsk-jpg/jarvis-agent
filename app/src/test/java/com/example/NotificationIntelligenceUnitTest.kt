package com.example

import com.example.jarvis.notification.JarvisNotification
import com.example.jarvis.notification.NotificationCategory
import com.example.jarvis.notification.NotificationClassifier
import com.example.jarvis.notification.NotificationIntelligenceEngine
import com.example.jarvis.notification.NotificationPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIntelligenceUnitTest {

    @Test
    fun testSecurityClassification() {
        val cat1 = NotificationClassifier.classify(
            packageName = "com.google.android.gms",
            appTitle = "Google Play Services",
            title = "Security Alert: New sign-in detected",
            text = "Was this you? A new sign-in from Linux was observed."
        )
        assertEquals(NotificationCategory.SECURITY, cat1)

        val cat2 = NotificationClassifier.classify(
            packageName = "com.github.android",
            appTitle = "GitHub",
            title = "Two-factor authentication code",
            text = "Your GitHub 2FA verification code is 482910"
        )
        assertEquals(NotificationCategory.SECURITY, cat2)
    }

    @Test
    fun testFinanceClassification() {
        val cat = NotificationClassifier.classify(
            packageName = "com.chase.sig.android",
            appTitle = "Chase",
            title = "Account Alert",
            text = "Payment of $42.50 was debited from your checking account."
        )
        assertEquals(NotificationCategory.FINANCE, cat)

        val prio = NotificationClassifier.calculatePriority(
            category = cat,
            isOngoing = false,
            combinedText = "Account Alert Payment debited"
        )
        assertEquals(NotificationPriority.HIGH, prio)
    }

    @Test
    fun testMessageClassification() {
        val cat = NotificationClassifier.classify(
            packageName = "com.whatsapp",
            appTitle = "WhatsApp",
            title = "Tony Stark",
            text = "Flight systems calibration completed. Check the lab."
        )
        assertEquals(NotificationCategory.MESSAGE, cat)
    }

    @Test
    fun testReminderClassification() {
        val cat = NotificationClassifier.classify(
            packageName = "com.google.android.calendar",
            appTitle = "Google Calendar",
            title = "Upcoming Meeting",
            text = "Project JARVIS briefing starts in 15 minutes."
        )
        assertEquals(NotificationCategory.REMINDER, cat)
    }

    @Test
    fun testPromotionClassification() {
        val cat = NotificationClassifier.classify(
            packageName = "com.ubercab.eats",
            appTitle = "Uber Eats",
            title = "50% off your next order!",
            text = "Exclusive deal: Save big on dinner tonight with code SAVE50."
        )
        assertEquals(NotificationCategory.PROMOTION, cat)

        val prio = NotificationClassifier.calculatePriority(
            category = cat,
            isOngoing = false,
            combinedText = "50% off save big"
        )
        assertEquals(NotificationPriority.LOW, prio)
    }

    @Test
    fun testGroupingAndAggregation() {
        val sampleList = listOf(
            JarvisNotification(
                id = "1",
                packageName = "com.whatsapp",
                appTitle = "WhatsApp",
                title = "Steve",
                text = "Are you there?",
                timestamp = 1000L,
                category = NotificationCategory.MESSAGE,
                priority = NotificationPriority.NORMAL
            ),
            JarvisNotification(
                id = "2",
                packageName = "com.whatsapp",
                appTitle = "WhatsApp",
                title = "Bruce",
                text = "Gamma levels normalized.",
                timestamp = 2000L,
                category = NotificationCategory.MESSAGE,
                priority = NotificationPriority.NORMAL
            ),
            JarvisNotification(
                id = "3",
                packageName = "com.chase.sig.android",
                appTitle = "Chase",
                title = "Debit Alert",
                text = "$100 spent",
                timestamp = 3000L,
                category = NotificationCategory.FINANCE,
                priority = NotificationPriority.HIGH
            )
        )

        val grouped = NotificationIntelligenceEngine.groupByApp(sampleList)
        assertEquals(2, grouped.size)

        val whatsappGroup = grouped.firstOrNull { it.packageName == "com.whatsapp" }
        assertTrue(whatsappGroup != null)
        assertEquals(2, whatsappGroup?.count)

        val important = NotificationIntelligenceEngine.getImportant(sampleList)
        assertEquals(1, important.size)
        assertEquals("Chase", important.first().appTitle)
    }

    @Test
    fun testSearchAndFiltering() {
        val sampleList = listOf(
            JarvisNotification(
                id = "1",
                packageName = "com.slack",
                appTitle = "Slack",
                title = "Production Incident #402",
                text = "Server latency spike detected in us-east-1",
                timestamp = 1000L,
                category = NotificationCategory.SYSTEM,
                priority = NotificationPriority.HIGH
            ),
            JarvisNotification(
                id = "2",
                packageName = "com.google.android.apps.messaging",
                appTitle = "Messages",
                title = "Pepper Potts",
                text = "Dinner reservation confirmed at 8 PM",
                timestamp = 2000L,
                category = NotificationCategory.MESSAGE,
                priority = NotificationPriority.NORMAL
            )
        )

        val searchResult1 = NotificationIntelligenceEngine.search(sampleList, "latency")
        assertEquals(1, searchResult1.size)
        assertEquals("Slack", searchResult1.first().appTitle)

        val searchResult2 = NotificationIntelligenceEngine.search(sampleList, "Pepper")
        assertEquals(1, searchResult2.size)
        assertEquals("Messages", searchResult2.first().appTitle)

        val searchResultEmpty = NotificationIntelligenceEngine.search(sampleList, "nonexistent")
        assertTrue(searchResultEmpty.isEmpty())
    }

    @Test
    fun testSummaryGenerationDoesNotLeak() {
        val sampleList = listOf(
            JarvisNotification(
                id = "1",
                packageName = "com.chase.sig.android",
                appTitle = "Chase",
                title = "Fraud Alert",
                text = "Suspicious transaction detected",
                timestamp = 5000L,
                category = NotificationCategory.FINANCE,
                priority = NotificationPriority.HIGH
            )
        )

        val summary = NotificationIntelligenceEngine.generateSmartSummary(sampleList)
        assertTrue(summary.contains("Chase"))
        assertTrue(summary.contains("Fraud Alert"))
        assertTrue(summary.contains("high-priority alert"))
    }
}
