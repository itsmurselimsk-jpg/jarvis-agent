package com.example.jarvis.notification

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.jarvis.security.SensitiveDataFilter
import com.example.jarvis.storage.db.JarvisDatabase
import com.example.jarvis.storage.db.NotificationLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backward compatible NotificationSummary data structure for legacy call sites.
 */
data class NotificationSummary(
    val id: String,
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

/**
 * Real native Android NotificationListenerService implementation for JARVIS.
 * Ingests notifications locally only when explicitly granted permission.
 * Classifies them deterministically and stores them in Room and a bounded live buffer.
 */
class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private var instance: JarvisNotificationListenerService? = null
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        // Legacy compatibility buffer
        private val _liveNotifications = MutableStateFlow<List<NotificationSummary>>(emptyList())
        val liveNotifications: StateFlow<List<NotificationSummary>> = _liveNotifications.asStateFlow()

        // Rich classified notification stream for Smart Notification Intelligence
        private val _liveClassifiedNotifications = MutableStateFlow<List<JarvisNotification>>(emptyList())
        val liveClassifiedNotifications: StateFlow<List<JarvisNotification>> = _liveClassifiedNotifications.asStateFlow()

        /**
         * Checks if the user has explicitly enabled Notification Access for JARVIS in system settings.
         */
        fun isNotificationAccessEnabled(context: Context): Boolean {
            val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
            return packageNames.contains(context.packageName)
        }

        /**
         * Creates an Intent to launch Android's Notification Listener Settings screen.
         */
        fun getNotificationSettingsIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun getInstance(): JarvisNotificationListenerService? = instance

        /**
         * Clears all buffered live notifications in memory.
         */
        fun clearLiveBuffer() {
            _liveNotifications.value = emptyList()
            _liveClassifiedNotifications.value = emptyList()
        }

        /**
         * Injects a classified notification for testing or local simulation when permitted.
         */
        fun injectNotificationForTesting(notification: JarvisNotification) {
            val current = _liveClassifiedNotifications.value.toMutableList()
            current.add(0, notification)
            _liveClassifiedNotifications.value = current
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val pkgName = sbn.packageName ?: ""
        // Do not intercept JARVIS's own alarm notifications to avoid feedback loops
        if (pkgName == packageName) return

        val rawTitle = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = extras.getString(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (rawTitle.isBlank() && rawText.isBlank()) return

        // Privacy protection: Sanitize sensitive info (e.g. 2FA codes, cards) before display or persistence
        val sanitizedTitle = SensitiveDataFilter.sanitizeForDisplay(rawTitle)
        val sanitizedText = SensitiveDataFilter.sanitizeForDisplay(rawText)

        val pm = applicationContext.packageManager
        val appTitle = try {
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkgName
        }

        // Android notification category tag (e.g. "msg", "call", "alarm")
        val androidCategory = notification.category
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val groupKey = sbn.groupKey ?: ""

        // 1. Run local deterministic classifier
        val category = NotificationClassifier.classify(
            packageName = pkgName,
            appTitle = appTitle,
            title = sanitizedTitle,
            text = sanitizedText,
            androidCategory = androidCategory
        )

        // 2. Run local deterministic priority calculator
        val priority = NotificationClassifier.calculatePriority(
            category = category,
            isOngoing = isOngoing,
            combinedText = "$sanitizedTitle $sanitizedText"
        )

        val id = "${sbn.id}_${sbn.postTime}"

        val classifiedItem = JarvisNotification(
            id = id,
            packageName = pkgName,
            appTitle = appTitle,
            title = sanitizedTitle,
            text = sanitizedText,
            timestamp = sbn.postTime,
            category = category,
            priority = priority,
            isOngoing = isOngoing,
            groupKey = groupKey
        )

        // Update rich classified buffer (max 60 items)
        val classifiedCurrent = _liveClassifiedNotifications.value.toMutableList()
        classifiedCurrent.add(0, classifiedItem)
        if (classifiedCurrent.size > 60) classifiedCurrent.removeAt(classifiedCurrent.size - 1)
        _liveClassifiedNotifications.value = classifiedCurrent

        // Update legacy compatibility summary buffer
        val legacyItem = NotificationSummary(
            id = id,
            packageName = pkgName,
            appTitle = appTitle,
            title = sanitizedTitle,
            text = sanitizedText,
            timestamp = sbn.postTime
        )
        val legacyCurrent = _liveNotifications.value.toMutableList()
        legacyCurrent.add(0, legacyItem)
        if (legacyCurrent.size > 50) legacyCurrent.removeAt(legacyCurrent.size - 1)
        _liveNotifications.value = legacyCurrent

        // Persist to local Room database with rich categorization and priority
        serviceScope.launch {
            try {
                val db = JarvisDatabase.getInstance(applicationContext)
                db.notificationDao().insertNotification(
                    NotificationLogEntity(
                        id = id,
                        packageName = pkgName,
                        appTitle = appTitle,
                        title = sanitizedTitle,
                        text = sanitizedText,
                        timestamp = sbn.postTime,
                        category = category.name,
                        priority = priority.name,
                        isOngoing = isOngoing,
                        groupKey = groupKey
                    )
                )
            } catch (_: Exception) {}
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val removedIdPrefix = "${sbn.id}_"
        // Safely update live buffers if a notification is dismissed from system tray
        val updated = _liveClassifiedNotifications.value.filterNot { it.id.startsWith(removedIdPrefix) }
        _liveClassifiedNotifications.value = updated

        val updatedLegacy = _liveNotifications.value.filterNot { it.id.startsWith(removedIdPrefix) }
        _liveNotifications.value = updatedLegacy
    }
}
