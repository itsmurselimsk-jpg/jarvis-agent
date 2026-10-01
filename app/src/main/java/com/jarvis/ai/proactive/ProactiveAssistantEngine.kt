package com.jarvis.ai.proactive

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import android.content.Context
import android.os.BatteryManager
import com.jarvis.ai.bridge.AndroidBridge
import com.jarvis.ai.notification.JarvisNotificationListenerService
import com.jarvis.ai.storage.JarvisRepository
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Autonomous Proactive Assistant Engine for JARVIS.
 * Generates proactive executive briefings, battery warnings, and notification digests.
 */
class ProactiveAssistantEngine(
    private val context: Context,
    private val repository: JarvisRepository,
    private val bridge: AndroidBridge
) {

    /**
     * Synthesizes an executive morning briefing with time, power telemetry, pending tasks, and notifications.
     */
    suspend fun generateMorningBriefing(isHindi: Boolean = false): String {
        val timeFormat = SimpleDateFormat("h:mm a, EEEE, MMMM d", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val batteryLevel = getBatteryLevel()
        val isCharging = isBatteryCharging()

        val pendingTasks = repository.tasks.value.filter { !it.isCompleted }
        val highPriorityTasks = pendingTasks.filter { it.priority.equals("High", ignoreCase = true) }

        val liveNotifs = JarvisNotificationListenerService.liveClassifiedNotifications.value
        val importantNotifs = liveNotifs.filter { 
            it.priority == com.jarvis.ai.notification.NotificationPriority.HIGH ||
            it.category == com.jarvis.ai.notification.NotificationCategory.IMPORTANT ||
            it.category == com.jarvis.ai.notification.NotificationCategory.MESSAGE
        }

        val sb = StringBuilder()

        if (isHindi) {
            sb.appendLine("🌅 **Good Morning, Sir.**")
            sb.appendLine("Samay: **$currentTime**")
            sb.appendLine("⚡ **Battery:** $batteryLevel% ${if (isCharging) "(Charging)" else ""}")
            if (pendingTasks.isNotEmpty()) {
                sb.appendLine("📋 **Pending Tasks (${pendingTasks.size}):**")
                pendingTasks.take(3).forEach {
                    val priorityTag = if (it.priority == "High") "🔥" else "•"
                    sb.appendLine("$priorityTag ${it.title}")
                }
            } else {
                sb.appendLine("📋 **Tasks:** Aaj koi pending task nahi hai, Sir.")
            }
            if (importantNotifs.isNotEmpty()) {
                sb.appendLine("🔔 **Notifications (${importantNotifs.size} VIP alerts):**")
                importantNotifs.take(2).forEach {
                    sb.appendLine("• ${it.appTitle}: ${it.title} - ${it.text}")
                }
            }
            sb.appendLine("\nSaare systems standby mode par hain. Kya aadesh hai?")
        } else {
            sb.appendLine("🌅 **Good Morning, Sir.**")
            sb.appendLine("Current time is **$currentTime**.")
            sb.appendLine("⚡ **Power Matrix:** Battery at $batteryLevel% ${if (isCharging) "[Charging Active]" else ""}.")
            if (pendingTasks.isNotEmpty()) {
                sb.appendLine("📋 **Scheduled Directives (${pendingTasks.size} pending):**")
                pendingTasks.take(3).forEach {
                    val priorityTag = if (it.priority == "High") "⚡ [HIGH]" else "•"
                    sb.appendLine("$priorityTag ${it.title}")
                }
            } else {
                sb.appendLine("📋 **Directives:** No pending tasks on your schedule, Sir.")
            }
            if (importantNotifs.isNotEmpty()) {
                sb.appendLine("🔔 **Screened Communications (${importantNotifs.size} alerts):**")
                importantNotifs.take(2).forEach {
                    sb.appendLine("• ${it.appTitle}: ${it.title} — ${it.text}")
                }
            }
            sb.appendLine("\nJARVIS core systems are fully nominal. How may I assist you?")
        }

        return sb.toString().trimEnd()
    }

    /**
     * Synthesizes an evening daily debrief of accomplishments and telemetry.
     */
    suspend fun generateDailyRecap(isHindi: Boolean = false): String {
        val completedTasks = repository.tasks.value.filter { it.isCompleted }
        val remainingTasks = repository.tasks.value.filter { !it.isCompleted }
        val battery = getBatteryLevel()

        return if (isHindi) {
            "🌙 **Daily Recap, Sir.**\n" +
            "• Completed Tasks: ${completedTasks.size}\n" +
            "• Remaining Tasks: ${remainingTasks.size}\n" +
            "• Current Battery: $battery%\n" +
            "Aaj ka din productively complete hua, Sir."
        } else {
            "🌙 **Daily Recap & Status Report:**\n" +
            "• Completed Directives: ${completedTasks.size}\n" +
            "• Remaining Pending Tasks: ${remainingTasks.size}\n" +
            "• Ending Power Reserves: $battery%\n" +
            "All autonomous systems secure for the night, Sir."
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
    }

    private fun isBatteryCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}
