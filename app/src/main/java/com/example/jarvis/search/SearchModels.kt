package com.example.jarvis.search

enum class SearchSource(val displayName: String, val badge: String) {
    ALL("All", "🌐"),
    APPS("Apps", "📱"),
    CONTACTS("Contacts", "👤"),
    MEMORY("Memory", "🧠"),
    TASKS("Tasks", "📋"),
    NOTIFICATIONS("Notifications", "🔔")
}

enum class SearchActionType {
    OPEN_APP,
    VIEW_CONTACT,
    CALL_CONTACT,
    VIEW_TASK,
    VIEW_MEMORY,
    VIEW_NOTIFICATION
}

data class UniversalSearchResult(
    val id: String,
    val source: SearchSource,
    val title: String,
    val subtitle: String,
    val actionType: SearchActionType,
    val actionPayload: String,
    val timestamp: Long? = null,
    val score: Float = 1.0f,
    val badge: String = source.badge,
    val extraData: Map<String, String> = emptyMap()
)
