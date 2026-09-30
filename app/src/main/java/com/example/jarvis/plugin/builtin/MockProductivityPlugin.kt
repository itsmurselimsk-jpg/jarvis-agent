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

data class CloudRecord(
    val id: String,
    val title: String,
    val category: String,
    val content: String,
    val createdTimestamp: Long = System.currentTimeMillis()
)

/**
 * Built-in deterministic offline connected service adapter for Cloud Workspace & Productivity records.
 * Runs 100% offline without requiring external network, Firebase, or external API keys.
 */
class MockProductivityPlugin : ConnectedServiceAdapter(
    manifest = PluginManifest(
        id = "plugin-productivity-mock",
        displayName = "Cloud Workspace Service",
        version = "1.0.0",
        description = "Provides secure connected cloud workspace records, document summaries, and productivity tracking.",
        providerName = "JARVIS Cloud Adapter",
        category = PluginCategory.PRODUCTIVITY,
        capabilities = setOf(
            PluginCapability.READ,
            PluginCapability.WRITE,
            PluginCapability.SEARCH,
            PluginCapability.CREATE,
            PluginCapability.DELETE,
            PluginCapability.ACCOUNT_DATA
        ),
        permissions = setOf(
            PluginPermission.READ_RECORDS,
            PluginPermission.WRITE_RECORDS,
            PluginPermission.SEARCH_RECORDS,
            PluginPermission.CREATE_RECORDS,
            PluginPermission.DELETE_RECORDS,
            PluginPermission.ACCESS_ACCOUNT
        ),
        author = "JARVIS Core Team",
        isBuiltIn = true,
        requiresCredentials = true,
        credentialPlaceholder = "Bearer cl_workspace_token_..."
    )
) {

    // In-memory deterministic records store
    private val records = ConcurrentHashMap<String, CloudRecord>()

    // Configurable simulated behaviors for testing resilience
    var simulatedFailureMode: PluginErrorCode? = null
    var transientFailureCountRemaining: Int = 0

    init {
        // Seed standard deterministic records
        seedDefaultRecords()
    }

    private fun seedDefaultRecords() {
        records["rec-001"] = CloudRecord(
            id = "rec-001",
            title = "Q3 Infrastructure Roadmap",
            category = "Engineering",
            content = "Migrate background worker pools to asynchronous Kotlin coroutines and optimize cache warming."
        )
        records["rec-002"] = CloudRecord(
            id = "rec-002",
            title = "Enterprise Security Audit Summary",
            category = "Security",
            content = "All cryptographic vaults passed AES-256-GCM verification. Zero plaintext credentials detected in logs."
        )
        records["rec-003"] = CloudRecord(
            id = "rec-003",
            title = "Product Launch Schedule 2026",
            category = "Product",
            content = "Global rollout scheduled for Q4. Edge neural models deployed across all regional partitions."
        )
    }

    override val exposedTools: List<PluginToolDefinition> = listOf(
        PluginToolDefinition(
            toolId = "SearchCloudRecords",
            pluginId = manifest.id,
            name = "SearchCloudRecords",
            description = "Search documents and items across the connected Cloud Workspace service.",
            requiredCapabilities = setOf(PluginCapability.READ, PluginCapability.SEARCH),
            riskLevel = RiskLevel.SAFE,
            inputSchema = mapOf("query" to "Search query term", "category" to "Optional category filter")
        ),
        PluginToolDefinition(
            toolId = "CreateCloudRecord",
            pluginId = manifest.id,
            name = "CreateCloudRecord",
            description = "Create a new record in the connected Cloud Workspace service.",
            requiredCapabilities = setOf(PluginCapability.CREATE, PluginCapability.WRITE),
            riskLevel = RiskLevel.CONFIRMATION,
            inputSchema = mapOf("title" to "Record title", "content" to "Record content", "category" to "Category tag")
        ),
        PluginToolDefinition(
            toolId = "DeleteCloudRecord",
            pluginId = manifest.id,
            name = "DeleteCloudRecord",
            description = "Permanently delete a record from the connected Cloud Workspace service.",
            requiredCapabilities = setOf(PluginCapability.DELETE),
            riskLevel = RiskLevel.CONFIRMATION,
            inputSchema = mapOf("recordId" to "ID of the record to delete")
        ),
        PluginToolDefinition(
            toolId = "GetCloudAccountProfile",
            pluginId = manifest.id,
            name = "GetCloudAccountProfile",
            description = "Retrieve connected account identity and storage quota information.",
            requiredCapabilities = setOf(PluginCapability.ACCOUNT_DATA, PluginCapability.READ),
            riskLevel = RiskLevel.SAFE,
            inputSchema = emptyMap()
        )
    )

    override suspend fun onExecuteAction(
        actionName: String,
        params: Map<String, Any?>,
        context: PluginContext
    ): PluginResult {
        // Handle transient / simulated failure modes for testing
        if (transientFailureCountRemaining > 0) {
            transientFailureCountRemaining--
            throw java.io.IOException("Simulated transient socket timeout (retries remaining)")
        }

        simulatedFailureMode?.let { mode ->
            return when (mode) {
                PluginErrorCode.AUTH_ERROR -> PluginResult(
                    success = false,
                    error = PluginError(PluginErrorCode.AUTH_ERROR, "Invalid workspace credentials", retryable = false),
                    rawOutput = "Authentication failed with connected service."
                )
                PluginErrorCode.RATE_LIMITED -> PluginResult(
                    success = false,
                    error = PluginError(PluginErrorCode.RATE_LIMITED, "API rate limit exceeded (429)", retryable = false),
                    rawOutput = "Rate limit reached on workspace service."
                )
                PluginErrorCode.NETWORK_ERROR -> throw java.io.IOException("Simulated network outage")
                else -> PluginResult(
                    success = false,
                    error = PluginError(mode, "Simulated failure: $mode", retryable = false),
                    rawOutput = "Service failed with code: $mode"
                )
            }
        }

        return when (actionName) {
            "SearchCloudRecords" -> {
                val query = (params["query"] as? String ?: "").trim().lowercase()
                val category = (params["category"] as? String ?: "").trim().lowercase()

                val matched = records.values.filter { rec ->
                    (query.isBlank() || rec.title.lowercase().contains(query) || rec.content.lowercase().contains(query)) &&
                    (category.isBlank() || rec.category.lowercase().contains(category))
                }.sortedByDescending { it.createdTimestamp }

                val summary = if (matched.isEmpty()) {
                    "No cloud records matched query '$query'."
                } else {
                    buildString {
                        appendLine("Found ${matched.size} connected cloud record(s):")
                        matched.forEachIndexed { index, rec ->
                            appendLine("[${index + 1}] ID: ${rec.id} | ${rec.title} (${rec.category})")
                            appendLine("    ${rec.content}")
                        }
                    }.trim()
                }

                val sanitized = PluginSafetyEngine.sanitizePluginOutput(summary)
                PluginResult(
                    success = true,
                    data = mapOf("records" to matched.map { mapOf("id" to it.id, "title" to it.title, "category" to it.category) }),
                    rawOutput = sanitized,
                    itemsCount = matched.size
                )
            }

            "CreateCloudRecord" -> {
                val title = (params["title"] as? String ?: "Untitled Cloud Record").trim()
                val content = (params["content"] as? String ?: "").trim()
                val category = (params["category"] as? String ?: "General").trim()

                val newId = "rec-${System.currentTimeMillis() % 100000}"
                val newRecord = CloudRecord(
                    id = newId,
                    title = title,
                    category = category,
                    content = content
                )
                records[newId] = newRecord
                context.log("Created cloud record '$newId' with title '$title'")

                PluginResult(
                    success = true,
                    data = mapOf("recordId" to newId, "title" to title),
                    rawOutput = "Successfully created cloud record [ID: $newId]: '$title' under category '$category'.",
                    itemsCount = 1
                )
            }

            "DeleteCloudRecord" -> {
                val recordId = (params["recordId"] as? String ?: "").trim()
                if (recordId.isBlank() || !records.containsKey(recordId)) {
                    return PluginResult(
                        success = false,
                        error = PluginError(PluginErrorCode.INVALID_REQUEST, "Record ID '$recordId' not found"),
                        rawOutput = "Error: Cloud record '$recordId' does not exist."
                    )
                }

                val removed = records.remove(recordId)
                context.log("Deleted cloud record '$recordId'")

                PluginResult(
                    success = true,
                    data = mapOf("deletedId" to recordId, "title" to (removed?.title ?: "")),
                    rawOutput = "Successfully deleted cloud record [ID: $recordId]: '${removed?.title}'.",
                    itemsCount = 1
                )
            }

            "GetCloudAccountProfile" -> {
                val response = """
                    Connected Service: Cloud Workspace Service
                    Account: jarvis-operator@cloud-workspace.internal
                    Status: ACTIVE (Encrypted Channel)
                    Storage Quota: 1.2 GB / 50.0 GB (2.4% Used)
                    Total Records: ${records.size}
                """.trimIndent()

                PluginResult(
                    success = true,
                    data = mapOf(
                        "account" to "jarvis-operator@cloud-workspace.internal",
                        "status" to "ACTIVE",
                        "totalRecords" to records.size
                    ),
                    rawOutput = response,
                    itemsCount = 1
                )
            }

            else -> {
                PluginResult(
                    success = false,
                    error = PluginError(PluginErrorCode.UNSUPPORTED_OPERATION, "Unknown action '$actionName'"),
                    rawOutput = "Error: Action '$actionName' not supported."
                )
            }
        }
    }

    override suspend fun testConnection(context: PluginContext): PluginHealthCheck {
        val start = System.currentTimeMillis()
        if (simulatedFailureMode == PluginErrorCode.AUTH_ERROR) {
            return PluginHealthCheck(
                isHealthy = false,
                latencyMs = 45L,
                message = "Authentication rejected: Invalid service credentials."
            )
        }
        if (simulatedFailureMode == PluginErrorCode.NETWORK_ERROR) {
            return PluginHealthCheck(
                isHealthy = false,
                latencyMs = 120L,
                message = "Network connection unreachable."
            )
        }
        val latency = (System.currentTimeMillis() - start).coerceAtLeast(12L)
        return PluginHealthCheck(
            isHealthy = true,
            latencyMs = latency,
            message = "Connection healthy. Service operational (Latency: ${latency}ms, Records: ${records.size})."
        )
    }

    fun resetData() {
        records.clear()
        seedDefaultRecords()
        simulatedFailureMode = null
        transientFailureCountRemaining = 0
    }
}
