package com.example

import com.example.jarvis.search.SearchActionType
import com.example.jarvis.search.SearchSource
import com.example.jarvis.search.UniversalSearchResult
import com.example.jarvis.search.UniversalSearchService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UniversalSearchUnitTest {

    @Test
    fun testSearchResultModelCreation() {
        val result = UniversalSearchResult(
            id = "app:com.spotify.music",
            source = SearchSource.APPS,
            title = "Spotify",
            subtitle = "com.spotify.music",
            actionType = SearchActionType.OPEN_APP,
            actionPayload = "com.spotify.music",
            score = 100f,
            badge = "📱"
        )

        assertEquals("app:com.spotify.music", result.id)
        assertEquals(SearchSource.APPS, result.source)
        assertEquals("Spotify", result.title)
        assertEquals(SearchActionType.OPEN_APP, result.actionType)
        assertEquals("com.spotify.music", result.actionPayload)
        assertEquals(100f, result.score, 0.01f)
    }

    @Test
    fun testSearchSourcesBadgesAndNames() {
        assertEquals("All", SearchSource.ALL.displayName)
        assertEquals("Apps", SearchSource.APPS.displayName)
        assertEquals("Contacts", SearchSource.CONTACTS.displayName)
        assertEquals("Memory", SearchSource.MEMORY.displayName)
        assertEquals("Tasks", SearchSource.TASKS.displayName)
        assertEquals("Notifications", SearchSource.NOTIFICATIONS.displayName)

        assertEquals("📱", SearchSource.APPS.badge)
        assertEquals("👤", SearchSource.CONTACTS.badge)
        assertEquals("🧠", SearchSource.MEMORY.badge)
        assertEquals("📋", SearchSource.TASKS.badge)
        assertEquals("🔔", SearchSource.NOTIFICATIONS.badge)
    }

    @Test
    fun testDeduplicationAndRanking() {
        val item1 = UniversalSearchResult(
            id = "app:com.google.android.youtube",
            source = SearchSource.APPS,
            title = "YouTube",
            subtitle = "com.google.android.youtube",
            actionType = SearchActionType.OPEN_APP,
            actionPayload = "com.google.android.youtube",
            score = 90f,
            timestamp = 1000L
        )

        val item1Duplicate = UniversalSearchResult(
            id = "app:com.google.android.youtube_dup",
            source = SearchSource.APPS,
            title = "YouTube",
            subtitle = "Different subtitle",
            actionType = SearchActionType.OPEN_APP,
            actionPayload = "com.google.android.youtube",
            score = 80f,
            timestamp = 1500L
        )

        val item2 = UniversalSearchResult(
            id = "contact:123",
            source = SearchSource.CONTACTS,
            title = "Pepper Potts",
            subtitle = "+123456789",
            actionType = SearchActionType.CALL_CONTACT,
            actionPayload = "+123456789",
            score = 95f,
            timestamp = 2000L
        )

        val item3 = UniversalSearchResult(
            id = "task:456",
            source = SearchSource.TASKS,
            title = "Review Arc Reactor telemetry",
            subtitle = "Pending",
            actionType = SearchActionType.VIEW_TASK,
            actionPayload = "Review Arc Reactor telemetry",
            score = 70f,
            timestamp = 3000L
        )

        // Mock instance or static test for deduplication logic
        val rawList = listOf(item1, item1Duplicate, item2, item3)

        val seen = mutableSetOf<String>()
        val unique = mutableListOf<UniversalSearchResult>()
        for (item in rawList) {
            val key = "${item.source}:${item.title.trim().lowercase(Locale.ROOT)}:${item.actionPayload.trim().lowercase(Locale.ROOT)}"
            if (!seen.contains(key)) {
                seen.add(key)
                unique.add(item)
            }
        }

        val ranked = unique.sortedWith(
            compareByDescending<UniversalSearchResult> { it.score }
                .thenByDescending { it.timestamp ?: 0L }
        )

        // Verify duplicate was pruned
        assertEquals(3, ranked.size)
        // Highest score (Pepper Potts: 95f) first
        assertEquals("Pepper Potts", ranked[0].title)
        // Second highest (YouTube: 90f)
        assertEquals("YouTube", ranked[1].title)
        // Third highest (Review Arc Reactor: 70f)
        assertEquals("Review Arc Reactor telemetry", ranked[2].title)
    }

    @Test
    fun testActionTypesCoverage() {
        val types = SearchActionType.values()
        assertTrue(types.contains(SearchActionType.OPEN_APP))
        assertTrue(types.contains(SearchActionType.VIEW_CONTACT))
        assertTrue(types.contains(SearchActionType.CALL_CONTACT))
        assertTrue(types.contains(SearchActionType.VIEW_TASK))
        assertTrue(types.contains(SearchActionType.VIEW_MEMORY))
        assertTrue(types.contains(SearchActionType.VIEW_NOTIFICATION))
    }
}
