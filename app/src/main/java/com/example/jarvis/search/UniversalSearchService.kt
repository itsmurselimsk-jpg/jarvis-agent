package com.example.jarvis.search

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.notification.NotificationIntelligenceEngine
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

class UniversalSearchService(
    private val context: Context,
    private val repository: JarvisRepository,
    private val bridge: AndroidBridge
) {

    /**
     * Checks whether READ_CONTACTS permission is granted.
     */
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Executes deterministic, multi-source local unified search.
     * All matching and ranking is executed 100% locally on device without external API calls.
     */
    suspend fun search(
        rawQuery: String,
        sourceFilter: SearchSource = SearchSource.ALL,
        limitPerSource: Int = 10
    ): List<UniversalSearchResult> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<UniversalSearchResult>()

        coroutineScope {
            val appDeferred = async {
                if (sourceFilter == SearchSource.ALL || sourceFilter == SearchSource.APPS) {
                    searchApps(query, limitPerSource)
                } else emptyList()
            }

            val contactsDeferred = async {
                if (sourceFilter == SearchSource.ALL || sourceFilter == SearchSource.CONTACTS) {
                    searchContacts(query, limitPerSource)
                } else emptyList()
            }

            val memoryDeferred = async {
                if (sourceFilter == SearchSource.ALL || sourceFilter == SearchSource.MEMORY) {
                    searchMemories(query, limitPerSource)
                } else emptyList()
            }

            val tasksDeferred = async {
                if (sourceFilter == SearchSource.ALL || sourceFilter == SearchSource.TASKS) {
                    searchTasks(query, limitPerSource)
                } else emptyList()
            }

            val notifsDeferred = async {
                if (sourceFilter == SearchSource.ALL || sourceFilter == SearchSource.NOTIFICATIONS) {
                    searchNotifications(query, limitPerSource)
                } else emptyList()
            }

            results.addAll(appDeferred.await())
            results.addAll(contactsDeferred.await())
            results.addAll(memoryDeferred.await())
            results.addAll(tasksDeferred.await())
            results.addAll(notifsDeferred.await())
        }

        // Deterministic deduplication & relevance ranking
        val deduped = deduplicateAndRank(results, query)
        return@withContext deduped
    }

    /**
     * 1. Search Launchable Installed Applications
     */
    fun searchApps(query: String, limit: Int = 10): List<UniversalSearchResult> {
        val allApps = bridge.getInstalledAppsList()
        val qLower = query.lowercase(Locale.ROOT)

        return allApps
            .mapNotNull { (appName, packageName) ->
                val nameLower = appName.lowercase(Locale.ROOT)
                val pkgLower = packageName.lowercase(Locale.ROOT)

                val score = when {
                    nameLower == qLower -> 100f
                    nameLower.startsWith(qLower) -> 80f
                    nameLower.contains(qLower) -> 60f
                    pkgLower.contains(qLower) -> 40f
                    else -> 0f
                }

                if (score > 0f) {
                    UniversalSearchResult(
                        id = "app:$packageName",
                        source = SearchSource.APPS,
                        title = appName,
                        subtitle = packageName,
                        actionType = SearchActionType.OPEN_APP,
                        actionPayload = packageName,
                        score = score,
                        extraData = mapOf("packageName" to packageName, "appName" to appName)
                    )
                } else null
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 2. Search Device Contacts (Safe ContentResolver query, requires READ_CONTACTS)
     */
    fun searchContacts(query: String, limit: Int = 10): List<UniversalSearchResult> {
        if (!hasContactsPermission()) {
            return emptyList()
        }

        val qTrim = query.trim()
        val results = mutableListOf<UniversalSearchResult>()
        val seenNumbers = mutableSetOf<String>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            )

            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$qTrim%", "%$qTrim%")

            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                while (it.moveToNext()) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Contact" else "Contact"
                    val number = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                    val contactId = if (idIdx >= 0) it.getString(idIdx) ?: name else name

                    val cleanNum = number.filter { ch -> ch.isDigit() || ch == '+' }
                    if (seenNumbers.contains(cleanNum) && cleanNum.isNotEmpty()) {
                        continue
                    }
                    if (cleanNum.isNotEmpty()) {
                        seenNumbers.add(cleanNum)
                    }

                    val nameLower = name.lowercase(Locale.ROOT)
                    val qLower = qTrim.lowercase(Locale.ROOT)

                    val score = when {
                        nameLower == qLower -> 95f
                        nameLower.startsWith(qLower) -> 85f
                        nameLower.contains(qLower) -> 70f
                        number.contains(qTrim) -> 65f
                        else -> 50f
                    }

                    results.add(
                        UniversalSearchResult(
                            id = "contact:$contactId:$cleanNum",
                            source = SearchSource.CONTACTS,
                            title = name,
                            subtitle = if (number.isNotBlank()) number else "Device Contact",
                            actionType = SearchActionType.CALL_CONTACT,
                            actionPayload = if (cleanNum.isNotBlank()) cleanNum else name,
                            score = score,
                            extraData = mapOf("contactName" to name, "phoneNumber" to number)
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }

        return results.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 3. Search JARVIS Long-term Room Memories
     */
    fun searchMemories(query: String, limit: Int = 10): List<UniversalSearchResult> {
        val memories = repository.searchMemories(query)
        val qLower = query.lowercase(Locale.ROOT)

        return memories.map { memory ->
            val titleLower = memory.title.lowercase(Locale.ROOT)
            val contentLower = memory.content.lowercase(Locale.ROOT)

            val score = when {
                titleLower == qLower -> 90f
                titleLower.contains(qLower) -> 75f
                contentLower.contains(qLower) -> 60f
                else -> 50f
            }

            UniversalSearchResult(
                id = "memory:${memory.id}",
                source = SearchSource.MEMORY,
                title = memory.title,
                subtitle = "[${memory.category}] ${memory.content}",
                actionType = SearchActionType.VIEW_MEMORY,
                actionPayload = memory.content,
                timestamp = memory.timestamp,
                score = score,
                extraData = mapOf("category" to memory.category, "memoryId" to memory.id)
            )
        }.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 4. Search JARVIS Room Agenda Tasks and Reminders
     */
    fun searchTasks(query: String, limit: Int = 10): List<UniversalSearchResult> {
        val qLower = query.lowercase(Locale.ROOT)
        val allTasks = repository.tasks.value

        return allTasks
            .filter { task ->
                task.title.contains(qLower, ignoreCase = true) ||
                task.notes.contains(qLower, ignoreCase = true) ||
                task.priority.contains(qLower, ignoreCase = true) ||
                (task.dueDate != null && task.dueDate.contains(qLower, ignoreCase = true))
            }
            .map { task ->
                val titleLower = task.title.lowercase(Locale.ROOT)
                val score = when {
                    titleLower == qLower -> 90f
                    titleLower.startsWith(qLower) -> 80f
                    titleLower.contains(qLower) -> 65f
                    task.notes.contains(qLower, ignoreCase = true) -> 55f
                    else -> 45f
                }

                val statusLabel = if (task.isCompleted) "Completed" else "Pending"
                val dueLabel = if (!task.dueDate.isNullOrBlank()) " • Due: ${task.dueDate}" else ""
                val subtitle = "[$statusLabel • Priority: ${task.priority}$dueLabel] ${task.notes.ifBlank { "No notes" }}"

                UniversalSearchResult(
                    id = "task:${task.id}",
                    source = SearchSource.TASKS,
                    title = task.title,
                    subtitle = subtitle,
                    actionType = SearchActionType.VIEW_TASK,
                    actionPayload = task.title,
                    timestamp = task.timestamp,
                    score = score,
                    extraData = mapOf("taskId" to task.id, "priority" to task.priority)
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 5. Search Smart Notifications (Batch 2 integration)
     */
    suspend fun searchNotifications(query: String, limit: Int = 10): List<UniversalSearchResult> {
        val qLower = query.lowercase(Locale.ROOT)
        // Re-use live buffer + Room DB search
        val notifs = repository.searchNotificationsSync(query)

        return notifs.map { notif ->
            val titleLower = notif.title.lowercase(Locale.ROOT)
            val appLower = notif.appTitle.lowercase(Locale.ROOT)
            val textLower = notif.text.lowercase(Locale.ROOT)

            val score = when {
                appLower.contains(qLower) -> 80f
                titleLower.contains(qLower) -> 70f
                textLower.contains(qLower) -> 55f
                else -> 40f
            }

            UniversalSearchResult(
                id = "notif:${notif.id}",
                source = SearchSource.NOTIFICATIONS,
                title = "${notif.appTitle}: ${notif.title}",
                subtitle = "${notif.text} (${notif.formattedTime})",
                actionType = SearchActionType.VIEW_NOTIFICATION,
                actionPayload = notif.packageName,
                timestamp = notif.timestamp,
                score = score,
                extraData = mapOf(
                    "packageName" to notif.packageName,
                    "category" to notif.category.name,
                    "priority" to notif.priority.name
                )
            )
        }.sortedByDescending { it.score }.take(limit)
    }

    /**
     * Deterministic deduplication by payload/id and ordering by score desc, then timestamp desc.
     */
    fun deduplicateAndRank(
        results: List<UniversalSearchResult>,
        query: String
    ): List<UniversalSearchResult> {
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<UniversalSearchResult>()

        for (item in results) {
            val key = "${item.source}:${item.title.trim().lowercase(Locale.ROOT)}:${item.actionPayload.trim().lowercase(Locale.ROOT)}"
            if (!seen.contains(key)) {
                seen.add(key)
                unique.add(item)
            }
        }

        return unique.sortedWith(
            compareByDescending<UniversalSearchResult> { it.score }
                .thenByDescending { it.timestamp ?: 0L }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )
    }
}
