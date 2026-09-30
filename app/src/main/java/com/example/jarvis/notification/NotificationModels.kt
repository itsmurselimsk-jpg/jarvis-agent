package com.example.jarvis.notification

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standard categories defined for Batch 2 Smart Notification Intelligence.
 */
enum class NotificationCategory {
    IMPORTANT,
    MESSAGE,
    REMINDER,
    FINANCE,
    SECURITY,
    SYSTEM,
    SOCIAL,
    PROMOTION,
    OTHER
}

/**
 * Priority levels for incoming notifications.
 */
enum class NotificationPriority {
    HIGH,
    NORMAL,
    LOW
}

/**
 * Rich model representing an ingested, classified notification.
 */
data class JarvisNotification(
    val id: String,
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val category: NotificationCategory = NotificationCategory.OTHER,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val isOngoing: Boolean = false,
    val groupKey: String = ""
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

/**
 * Local deterministic classifier for notifications.
 * Uses transparent rules without uploading any data to cloud AI services.
 */
object NotificationClassifier {

    private val SECURITY_KEYWORDS = listOf(
        "security", "two-factor", "2fa", "verification code", "auth code", "login attempt",
        "suspicious", "unauthorized", "password reset", "new login", "otp", "passcode",
        "fraud", "security alert", "device connected", "unrecognized"
    )

    private val FINANCE_KEYWORDS = listOf(
        "bank", "payment", "paid", "received", "credited", "debited", "transfer",
        "transaction", "card", "wallet", "paypal", "gpay", "google pay", "balance",
        "statement", "invoice", "receipt", "purchase", "refund", "bKash", "nagad",
        "visa", "mastercard", "rupay", "bill due", "atm"
    )

    private val REMINDER_KEYWORDS = listOf(
        "reminder", "alarm", "calendar", "event", "meeting", "due", "schedule",
        "upcoming", "appointment", "task", "to-do", "deadline"
    )

    private val MESSAGE_KEYWORDS = listOf(
        "message", "chat", "sent you", "replied", "incoming call", "missed call",
        "whatsapp", "telegram", "messenger", "signal", "sms", "text message", "viber"
    )

    private val SOCIAL_KEYWORDS = listOf(
        "liked your", "commented", "followed you", "tagged you", "retweeted",
        "new subscriber", "feed", "facebook", "instagram", "twitter", "x.com",
        "tiktok", "snapchat", "linkedin", "reddit"
    )

    private val PROMOTION_KEYWORDS = listOf(
        "sale", "discount", "% off", "offer", "deal", "cashback", "coupon",
        "promo", "free delivery", "order now", "limited time", "exclusive deal",
        "special discount", "save big", "black friday"
    )

    private val SYSTEM_PACKAGES = setOf(
        "android", "com.android.systemui", "com.google.android.gms",
        "com.android.settings", "com.android.vending", "com.google.android.packageinstaller"
    )

    private val MESSAGE_PACKAGES = setOf(
        "com.whatsapp", "org.telegram.messenger", "com.facebook.orca",
        "com.google.android.apps.messaging", "com.facebook.mlite",
        "org.thoughtcrime.securesms", "com.discord", "com.slack"
    )

    private val FINANCE_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", "com.paypal.android.p2pmobile",
        "com.phonepe.app", "net.one97.paytm", "com.chase.sig.android",
        "com.bKash.customerapp"
    )

    /**
     * Classifies a notification into a NotificationCategory deterministically.
     */
    fun classify(
        packageName: String,
        appTitle: String,
        title: String,
        text: String,
        androidCategory: String? = null
    ): NotificationCategory {
        val lowerPkg = packageName.lowercase(Locale.ROOT)
        val lowerApp = appTitle.lowercase(Locale.ROOT)
        val combined = "$lowerApp $title $text".lowercase(Locale.ROOT)

        // 1. Security Check (Highest urgency)
        if (SECURITY_KEYWORDS.any { combined.contains(it) }) {
            return NotificationCategory.SECURITY
        }

        // 2. Finance Check
        if (FINANCE_PACKAGES.any { lowerPkg.contains(it) } || FINANCE_KEYWORDS.any { combined.contains(it) }) {
            return NotificationCategory.FINANCE
        }

        // 3. Android System Category Hint
        when (androidCategory) {
            "msg", "call" -> return NotificationCategory.MESSAGE
            "alarm", "reminder", "event" -> return NotificationCategory.REMINDER
            "promo" -> return NotificationCategory.PROMOTION
            "sys" -> return NotificationCategory.SYSTEM
        }

        // 4. Reminders & Calendar
        if (REMINDER_KEYWORDS.any { combined.contains(it) }) {
            return NotificationCategory.REMINDER
        }

        // 5. Messaging Apps & Communications
        if (MESSAGE_PACKAGES.any { lowerPkg.contains(it) } || MESSAGE_KEYWORDS.any { combined.contains(it) }) {
            return NotificationCategory.MESSAGE
        }

        // 6. Promotions / Marketing
        if (PROMOTION_KEYWORDS.any { combined.contains(it) }) {
            return NotificationCategory.PROMOTION
        }

        // 7. Social Media
        if (SOCIAL_KEYWORDS.any { combined.contains(it) }) {
            return NotificationCategory.SOCIAL
        }

        // 8. System & OS
        if (SYSTEM_PACKAGES.contains(lowerPkg) || lowerPkg.startsWith("com.android.") || combined.contains("system update")) {
            return NotificationCategory.SYSTEM
        }

        return NotificationCategory.OTHER
    }

    /**
     * Calculates Priority (HIGH, NORMAL, LOW) deterministically.
     */
    fun calculatePriority(
        category: NotificationCategory,
        isOngoing: Boolean,
        combinedText: String
    ): NotificationPriority {
        val lower = combinedText.lowercase(Locale.ROOT)

        // High priority: Security, Finance alerts, Urgent keywords, Missed calls
        if (category == NotificationCategory.SECURITY || category == NotificationCategory.FINANCE) {
            return NotificationPriority.HIGH
        }
        if (lower.contains("urgent") || lower.contains("emergency") || lower.contains("missed call") || lower.contains("action required")) {
            return NotificationPriority.HIGH
        }

        // Low priority: Ongoing background notifications, Promotions, Social updates
        if (category == NotificationCategory.PROMOTION) {
            return NotificationPriority.LOW
        }
        if (isOngoing && (lower.contains("running in background") || lower.contains("is active") || lower.contains("tap to open"))) {
            return NotificationPriority.LOW
        }

        // Reminders & Messages default to NORMAL (or HIGH if urgent)
        return NotificationPriority.NORMAL
    }
}
