package com.example.jarvis.plugin.builtin

import com.example.jarvis.model.RiskLevel
import com.example.jarvis.plugin.ConnectedServiceAdapter
import com.example.jarvis.plugin.PluginCapability
import com.example.jarvis.plugin.PluginCategory
import com.example.jarvis.plugin.PluginContext
import com.example.jarvis.plugin.PluginError
import com.example.jarvis.plugin.PluginErrorCode
import com.example.jarvis.plugin.PluginHealthCheck
import com.example.jarvis.plugin.PluginManifest
import com.example.jarvis.plugin.PluginPermission
import com.example.jarvis.plugin.PluginResult
import com.example.jarvis.plugin.PluginSafetyEngine
import com.example.jarvis.plugin.PluginToolDefinition
import java.util.concurrent.ConcurrentHashMap

data class CloudNote(
    val id: String,
    val title: String,
    val text: String,
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Built-in deterministic offline connected service adapter for Encrypted Cloud Notes.
 */
class MockNotesPlugin : ConnectedServiceAdapter(
    manifest = PluginManifest(
        id = "plugin-notes-mock",
        displayName = "Encrypted Cloud Notes",
        version = "1.0.0",
        description = "Provides secure synchronization and search across external encrypted cloud notebooks.",
        providerName = "JARVIS Notes Provider",
        category = PluginCategory.NOTES,
        capabilities = setOf(
            PluginCapability.READ,
            PluginCapability.SEARCH,
            PluginCapability.CREATE,
            PluginCapability.DELETE
        ),
        permissions = setOf(
            PluginPermission.READ_RECORDS,
            PluginPermission.SEARCH_RECORDS,
            PluginPermission.CREATE_RECORDS,
            PluginPermission.DELETE_RECORDS
        ),
        author = "JARVIS Core Team",
        isBuiltIn = true,
        requiresCredentials = false
    )
) {
    private val notes = ConcurrentHashMap<String, CloudNote>()

    init {
        notes["note-101"] = CloudNote(
            id = "note-101",
            title = "Android Coroutines Concurrency Notes",
            text = "Use SupervisorJob for independent worker failures. Avoid GlobalScope in production.",
            tags = listOf("android", "kotlin")
        )
        notes["note-102"] = CloudNote(
            id = "note-102",
            title = "Jetpack Compose Performance Checklist",
            text = "Use remember and derivedStateOf to prevent unnecessary recompositions on state updates.",
            tags = listOf("compose", "ui")
        )
    }

    override val exposedTools: List<PluginToolDefinition> = listOf(
        PluginToolDefinition(
            toolId = "SearchNotesRecords",
            pluginId = manifest.id,
            name = "SearchNotesRecords",
            description = "Search across connected cloud notes and notebook archives.",
            requiredCapabilities = setOf(PluginCapability.READ, PluginCapability.SEARCH),
            riskLevel = RiskLevel.SAFE,
            inputSchema = mapOf("query" to "Keyword search term")
        ),
        PluginToolDefinition(
            toolId = "CreateNotesRecord",
            pluginId = manifest.id,
            name = "CreateNotesRecord",
            description = "Create a new note in the connected cloud notes repository.",
            requiredCapabilities = setOf(PluginCapability.CREATE),
            riskLevel = RiskLevel.CONFIRMATION,
            inputSchema = mapOf("title" to "Note title", "text" to "Note body text")
        ),
        PluginToolDefinition(
            toolId = "DeleteNotesRecord",
            pluginId = manifest.id,
            name = "DeleteNotesRecord",
            description = "Delete a specific note from the connected cloud notebook.",
            requiredCapabilities = setOf(PluginCapability.DELETE),
            riskLevel = RiskLevel.CONFIRMATION,
            inputSchema = mapOf("noteId" to "ID of note to remove")
        )
    )

    override suspend fun onExecuteAction(
        actionName: String,
        params: Map<String, Any?>,
        context: PluginContext
    ): PluginResult {
        return when (actionName) {
            "SearchNotesRecords" -> {
                val q = (params["query"] as? String ?: "").trim().lowercase()
                val matches = notes.values.filter { note ->
                    q.isBlank() || note.title.lowercase().contains(q) || note.text.lowercase().contains(q)
                }

                val out = if (matches.isEmpty()) {
                    "No cloud notes matched query '$q'."
                } else {
                    buildString {
                        appendLine("Found ${matches.size} note(s):")
                        matches.forEachIndexed { i, n ->
                            appendLine("[${i + 1}] ID: ${n.id} | ${n.title}")
                            appendLine("    ${n.text}")
                        }
                    }.trim()
                }

                val sanitized = PluginSafetyEngine.sanitizePluginOutput(out)
                PluginResult(
                    success = true,
                    data = mapOf("notes" to matches.map { mapOf("id" to it.id, "title" to it.title) }),
                    rawOutput = sanitized,
                    itemsCount = matches.size
                )
            }

            "CreateNotesRecord" -> {
                val title = (params["title"] as? String ?: "New Note").trim()
                val text = (params["text"] as? String ?: "").trim()
                val id = "note-${System.currentTimeMillis() % 100000}"
                notes[id] = CloudNote(id = id, title = title, text = text)

                PluginResult(
                    success = true,
                    data = mapOf("noteId" to id, "title" to title),
                    rawOutput = "Created note [ID: $id]: '$title'.",
                    itemsCount = 1
                )
            }

            "DeleteNotesRecord" -> {
                val noteId = (params["noteId"] as? String ?: "").trim()
                if (noteId.isBlank() || !notes.containsKey(noteId)) {
                    return PluginResult(
                        success = false,
                        error = PluginError(PluginErrorCode.INVALID_REQUEST, "Note '$noteId' not found"),
                        rawOutput = "Error: Note '$noteId' does not exist."
                    )
                }
                val removed = notes.remove(noteId)
                PluginResult(
                    success = true,
                    data = mapOf("deletedId" to noteId),
                    rawOutput = "Deleted note [ID: $noteId]: '${removed?.title}'.",
                    itemsCount = 1
                )
            }

            else -> PluginResult(
                success = false,
                error = PluginError(PluginErrorCode.UNSUPPORTED_OPERATION, "Action '$actionName' unsupported"),
                rawOutput = "Error: Unsupported action '$actionName'"
            )
        }
    }

    override suspend fun testConnection(context: PluginContext): PluginHealthCheck {
        return PluginHealthCheck(
            isHealthy = true,
            latencyMs = 15L,
            message = "Notes repository online (${notes.size} notes stored)."
        )
    }
}
