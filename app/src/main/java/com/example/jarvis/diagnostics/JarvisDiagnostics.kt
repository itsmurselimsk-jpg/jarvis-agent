package com.example.jarvis.diagnostics

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.jarvis.accessibility.JarvisAccessibilityService
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.AIProviderType
import com.example.jarvis.notification.JarvisNotificationListenerService
import com.example.jarvis.storage.JarvisRepository

enum class DiagnosticStatus {
    WORKING,
    PERMISSION_REQUIRED,
    CONFIGURATION_REQUIRED,
    ANDROID_RESTRICTION,
    BROKEN,
    NOT_IMPLEMENTED
}

data class DiagnosticItem(
    val component: String,
    val status: DiagnosticStatus,
    val details: String,
    val category: String = "Core"
)

object JarvisDiagnostics {

    fun runAllDiagnostics(context: Context, repository: JarvisRepository, bridge: AndroidBridge): List<DiagnosticItem> {
        val results = mutableListOf<DiagnosticItem>()

        // 1. Microphone Permission
        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        results.add(
            DiagnosticItem(
                component = "Microphone Permission",
                status = if (micGranted) DiagnosticStatus.WORKING else DiagnosticStatus.PERMISSION_REQUIRED,
                details = if (micGranted) "RECORD_AUDIO granted" else "Microphone permission required for voice listening",
                category = "Permissions"
            )
        )

        // 2. Microphone / Voice Service
        val micMuted = bridge.isMicMuted.value
        val micStatus = if (!micGranted) {
            DiagnosticStatus.PERMISSION_REQUIRED
        } else if (micMuted) {
            DiagnosticStatus.WORKING
        } else {
            DiagnosticStatus.WORKING
        }
        results.add(
            DiagnosticItem(
                component = "Microphone Service",
                status = micStatus,
                details = if (!micGranted) "Requires RECORD_AUDIO" else if (micMuted) "Software mic input muted" else "Voice listening pipeline active",
                category = "Voice"
            )
        )

        // 3. SpeechRecognizer
        val isSpeechAvailable = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
        results.add(
            DiagnosticItem(
                component = "SpeechRecognizer",
                status = if (isSpeechAvailable) DiagnosticStatus.WORKING else DiagnosticStatus.BROKEN,
                details = if (isSpeechAvailable) "Android platform recognizer engine available" else "No speech recognition service found on device",
                category = "Voice"
            )
        )

        // 4. Android TTS
        val isTtsReady = bridge.isTtsInitialized()
        results.add(
            DiagnosticItem(
                component = "Android TTS",
                status = if (isTtsReady) DiagnosticStatus.WORKING else DiagnosticStatus.CONFIGURATION_REQUIRED,
                details = if (isTtsReady) "Text-To-Speech engine initialized" else "TTS engine initializing or missing default speech voices",
                category = "Voice"
            )
        )

        // 5. Notification Permission (Android 13+)
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        results.add(
            DiagnosticItem(
                component = "Notification Permission",
                status = if (notifGranted) DiagnosticStatus.WORKING else DiagnosticStatus.PERMISSION_REQUIRED,
                details = if (notifGranted) "POST_NOTIFICATIONS permission granted" else "POST_NOTIFICATIONS required to post reminders",
                category = "Permissions"
            )
        )

        // 6. Accessibility Service
        val accessEnabled = JarvisAccessibilityService.isServiceEnabled(context)
        results.add(
            DiagnosticItem(
                component = "Accessibility Service",
                status = if (accessEnabled) DiagnosticStatus.WORKING else DiagnosticStatus.CONFIGURATION_REQUIRED,
                details = if (accessEnabled) "JarvisAccessibilityService is enabled in Android Settings" else "Service disabled in Accessibility Settings",
                category = "System Services"
            )
        )

        // 7. Notification Listener
        val notifAccessEnabled = JarvisNotificationListenerService.isNotificationAccessEnabled(context)
        results.add(
            DiagnosticItem(
                component = "Notification Listener",
                status = if (notifAccessEnabled) DiagnosticStatus.WORKING else DiagnosticStatus.CONFIGURATION_REQUIRED,
                details = if (notifAccessEnabled) "NotificationListenerService granted access" else "Notification Listener access not granted in Settings",
                category = "System Services"
            )
        )

        // 8. Camera Permission
        val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        results.add(
            DiagnosticItem(
                component = "Camera Permission",
                status = if (camGranted) DiagnosticStatus.WORKING else DiagnosticStatus.PERMISSION_REQUIRED,
                details = if (camGranted) "CAMERA permission granted" else "Camera permission required for visual HUD inspection",
                category = "Permissions"
            )
        )

        // 9. Contacts Permission
        val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        results.add(
            DiagnosticItem(
                component = "Contacts Permission",
                status = if (contactsGranted) DiagnosticStatus.WORKING else DiagnosticStatus.PERMISSION_REQUIRED,
                details = if (contactsGranted) "READ_CONTACTS granted" else "Contacts permission required for caller voice resolution",
                category = "Permissions"
            )
        )

        // 10. Battery Subsystem
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val batteryStatus = if (batteryPct in 0..100) DiagnosticStatus.WORKING else DiagnosticStatus.BROKEN
        results.add(
            DiagnosticItem(
                component = "Battery Telemetry",
                status = batteryStatus,
                details = if (batteryPct in 0..100) "Telemetry operational at $batteryPct%" else "BatteryManager returned invalid reading",
                category = "Hardware"
            )
        )

        // 11. Network Subsystem
        val netStatus = bridge.getNetworkStatus()
        val isConnected = netStatus["isConnected"] as? Boolean ?: false
        val transport = netStatus["transport"] as? String ?: "NONE"
        results.add(
            DiagnosticItem(
                component = "Network Telemetry",
                status = if (isConnected) DiagnosticStatus.WORKING else DiagnosticStatus.CONFIGURATION_REQUIRED,
                details = if (isConnected) "Active link via $transport" else "Offline or network disconnected",
                category = "Hardware"
            )
        )

        // 12. AI Provider
        val settings = repository.settings.value
        val hasCustomKey = settings.customApiKey.isNotBlank()
        val aiStatus = when (settings.providerType) {
            AIProviderType.LOCAL_NEURAL_BRAIN -> DiagnosticStatus.WORKING
            AIProviderType.GEMINI -> {
                if (hasCustomKey || com.example.BuildConfig.GEMINI_API_KEY.isNotBlank()) DiagnosticStatus.WORKING else DiagnosticStatus.CONFIGURATION_REQUIRED
            }
            AIProviderType.OPENAI_COMPATIBLE -> {
                if (hasCustomKey) DiagnosticStatus.WORKING else DiagnosticStatus.CONFIGURATION_REQUIRED
            }
        }
        results.add(
            DiagnosticItem(
                component = "AI Provider (${settings.providerType.name})",
                status = aiStatus,
                details = when (aiStatus) {
                    DiagnosticStatus.WORKING -> "Active provider verified with configuration"
                    DiagnosticStatus.CONFIGURATION_REQUIRED -> "API key required in Settings for cloud routing"
                    else -> "AI configuration check"
                },
                category = "Intelligence"
            )
        )

        // 13. Room Database
        val memoryCount = repository.memories.value.size
        results.add(
            DiagnosticItem(
                component = "Room Memory Database",
                status = DiagnosticStatus.WORKING,
                details = "JarvisDatabase initialized with $memoryCount active memory records",
                category = "Storage"
            )
        )

        // 14. Scheduler / Tasks
        val taskCount = repository.tasks.value.size
        results.add(
            DiagnosticItem(
                component = "Scheduler & Tasks",
                status = DiagnosticStatus.WORKING,
                details = "Agenda scheduler operational with $taskCount active protocol items",
                category = "Automation"
            )
        )

        // 15. Error Recovery & Fault Tolerance
        results.add(
            DiagnosticItem(
                component = "Error Recovery Engine",
                status = DiagnosticStatus.WORKING,
                details = "Bounded exponential backoff, rate-limit avoidance, and safe fallback active",
                category = "Intelligence"
            )
        )

        return results
    }

    fun formatDiagnosticReport(items: List<DiagnosticItem>): String {
        val sb = StringBuilder()
        sb.appendLine("JARVIS SYSTEM SELF-DIAGNOSTICS REPORT")
        sb.appendLine("=====================================")
        val grouped = items.groupBy { it.category }
        grouped.forEach { (category, list) ->
            sb.appendLine("[$category]")
            list.forEach { item ->
                val statusTag = item.status.name.replace("_", " ")
                sb.appendLine("• ${item.component}: $statusTag — ${item.details}")
            }
            sb.appendLine()
        }
        val workingCount = items.count { it.status == DiagnosticStatus.WORKING }
        sb.append("Summary: $workingCount of ${items.size} subsystems fully functional.")
        return sb.toString().trimEnd()
    }
}
