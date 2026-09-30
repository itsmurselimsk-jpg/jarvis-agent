package com.example.jarvis.privacy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.jarvis.accessibility.JarvisAccessibilityService
import com.example.jarvis.notification.JarvisNotificationListenerService
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.storage.db.JarvisDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PrivacyTelemetry(
    val hasMicPermission: Boolean,
    val hasCameraPermission: Boolean,
    val isNotificationAccessEnabled: Boolean,
    val isAccessibilityServiceEnabled: Boolean,
    val activeProviderName: String
)

class PrivacyAuditor(
    private val context: Context,
    private val repository: JarvisRepository
) {
    private val db = JarvisDatabase.getInstance(context)

    fun getPrivacyTelemetry(): PrivacyTelemetry {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val notifAccess = JarvisNotificationListenerService.isNotificationAccessEnabled(context)
        val accessibilityAccess = JarvisAccessibilityService.isServiceEnabled(context)
        val providerName = repository.settings.value.providerType.name

        return PrivacyTelemetry(
            hasMicPermission = hasMic,
            hasCameraPermission = hasCamera,
            isNotificationAccessEnabled = notifAccess,
            isAccessibilityServiceEnabled = accessibilityAccess,
            activeProviderName = providerName
        )
    }

    val memoryCountFlow: Flow<Int> = db.memoryDao().getMemoryCount()
    val taskCountFlow: Flow<Int> = db.taskDao().getTaskCount()
    val notificationCountFlow: Flow<Int> = db.notificationDao().getNotificationCount()
    val messageCountFlow: Flow<Int> = repository.messages.map { it.size }

    suspend fun purgeAllMemories() {
        db.memoryDao().clearAllMemories()
        repository.reloadMemories()
    }

    suspend fun purgeAllTasks() {
        db.taskDao().clearAllTasks()
        repository.reloadTasks()
    }

    suspend fun purgeAllNotifications() {
        db.notificationDao().clearAllNotifications()
    }

    fun purgeAllConversation() {
        repository.clearMessages()
    }

    suspend fun purgeAllLocalData() {
        purgeAllMemories()
        purgeAllTasks()
        purgeAllNotifications()
        purgeAllConversation()
    }
}
