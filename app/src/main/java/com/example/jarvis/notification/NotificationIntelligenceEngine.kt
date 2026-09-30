package com.example.jarvis.notification

import java.util.Locale

/**
 * Groups and aggregates notifications from the same application or with identical content.
 */
data class GroupedNotification(
    val appTitle: String,
    val packageName: String,
    val count: Int,
    val latestTimestamp: Long,
    val category: NotificationCategory,
    val highestPriority: NotificationPriority,
    val items: List<JarvisNotification>,
    val latestTitle: String,
    val latestText: String
)

/**
 * Local deterministic intelligence & summarizer engine.
 * Never calls external APIs or uploads sensitive data.
 */
object NotificationIntelligenceEngine {

    /**
     * Groups a list of notifications by application and aggregates duplicates.
     */
    fun groupByApp(notifications: List<JarvisNotification>): List<GroupedNotification> {
        return notifications
            .groupBy { it.packageName.ifBlank { it.appTitle } }
            .map { (_, list) ->
                val sorted = list.sortedByDescending { it.timestamp }
                val latest = sorted.first()
                val highestPrio = if (list.any { it.priority == NotificationPriority.HIGH }) {
                    NotificationPriority.HIGH
                } else if (list.any { it.priority == NotificationPriority.NORMAL }) {
                    NotificationPriority.NORMAL
                } else {
                    NotificationPriority.LOW
                }

                GroupedNotification(
                    appTitle = latest.appTitle.ifBlank { latest.packageName },
                    packageName = latest.packageName,
                    count = list.size,
                    latestTimestamp = latest.timestamp,
                    category = latest.category,
                    highestPriority = highestPrio,
                    items = sorted,
                    latestTitle = latest.title,
                    latestText = latest.text
                )
            }
            .sortedByDescending { it.latestTimestamp }
    }

    /**
     * Groups identical or repeated notifications (e.g. repeated alerts from same app with same title).
     */
    fun deduplicateAndSummarizeRepeated(notifications: List<JarvisNotification>): List<JarvisNotification> {
        val seen = mutableMapOf<String, JarvisNotification>()
        for (item in notifications) {
            val key = "${item.packageName}:::${item.title.trim().lowercase(Locale.ROOT)}"
            if (!seen.containsKey(key)) {
                seen[key] = item
            }
        }
        return seen.values.toList()
    }

    /**
     * Filters notifications matching a local free-text search query across title, text, and app name.
     */
    fun search(notifications: List<JarvisNotification>, query: String): List<JarvisNotification> {
        if (query.isBlank()) return notifications
        val q = query.trim().lowercase(Locale.ROOT)
        return notifications.filter {
            it.title.lowercase(Locale.ROOT).contains(q) ||
            it.text.lowercase(Locale.ROOT).contains(q) ||
            it.appTitle.lowercase(Locale.ROOT).contains(q) ||
            it.packageName.lowercase(Locale.ROOT).contains(q) ||
            it.category.name.lowercase(Locale.ROOT).contains(q)
        }
    }

    /**
     * Filters notifications for a specific app name or package.
     */
    fun filterByApp(notifications: List<JarvisNotification>, appQuery: String): List<JarvisNotification> {
        val q = appQuery.trim().lowercase(Locale.ROOT)
        return notifications.filter {
            it.appTitle.lowercase(Locale.ROOT).contains(q) ||
            it.packageName.lowercase(Locale.ROOT).contains(q)
        }
    }

    /**
     * Filters notifications for high priority and critical categories (Security, Finance, etc.).
     */
    fun getImportant(notifications: List<JarvisNotification>): List<JarvisNotification> {
        return notifications.filter {
            it.priority == NotificationPriority.HIGH ||
            it.category == NotificationCategory.SECURITY ||
            it.category == NotificationCategory.FINANCE
        }
    }

    /**
     * Generates a concise natural language summary for JARVIS voice and chat.
     */
    fun generateSmartSummary(
        notifications: List<JarvisNotification>,
        specificCategory: NotificationCategory? = null,
        specificApp: String? = null
    ): String {
        if (notifications.isEmpty()) {
            return "No intercepted notifications found in local cache."
        }

        var filtered = notifications
        if (specificCategory != null) {
            filtered = filtered.filter { it.category == specificCategory }
        }
        if (!specificApp.isNullOrBlank()) {
            filtered = filterByApp(filtered, specificApp)
        }

        if (filtered.isEmpty()) {
            return if (specificApp != null) {
                "No notifications recorded from '$specificApp'."
            } else if (specificCategory != null) {
                "No ${specificCategory.name.lowercase(Locale.ROOT)} notifications detected."
            } else {
                "No notifications match your query."
            }
        }

        val highPriority = filtered.filter { it.priority == NotificationPriority.HIGH }
        val groupedByApp = groupByApp(filtered)

        val sb = StringBuilder()
        if (specificApp != null) {
            sb.append("Found ${filtered.size} notification(s) from $specificApp:\n")
        } else if (specificCategory != null) {
            sb.append("Found ${filtered.size} ${specificCategory.name.lowercase(Locale.ROOT)} notification(s):\n")
        } else {
            sb.append("You have ${filtered.size} total notification(s) across ${groupedByApp.size} application(s).\n")
            if (highPriority.isNotEmpty()) {
                sb.append("⚠️ ${highPriority.size} high-priority alert(s) require attention.\n")
            }
        }

        sb.append("\nSummary by Application:\n")
        for (group in groupedByApp.take(6)) {
            val preview = if (group.latestText.isNotBlank()) " - \"${group.latestText.take(60)}\"" else ""
            sb.append("• ${group.appTitle} (${group.count}): ${group.latestTitle}$preview\n")
        }

        if (groupedByApp.size > 6) {
            val remainder = groupedByApp.drop(6).sumOf { it.count }
            sb.append("• ... plus $remainder more from other applications.\n")
        }

        return sb.toString().trimEnd()
    }
}
