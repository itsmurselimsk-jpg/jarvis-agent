package com.example.jarvis.plugin

import com.example.jarvis.model.RiskLevel

/**
 * Granular capabilities that plugins can declare and request.
 */
enum class PluginCapability {
    READ,
    WRITE,
    SEARCH,
    CREATE,
    UPDATE,
    DELETE,
    NETWORK,
    ACCOUNT_DATA,
    SENSITIVE_DATA
}

/**
 * Android-aligned granular permissions for plugin operations.
 */
enum class PluginPermission {
    READ_RECORDS,
    WRITE_RECORDS,
    SEARCH_RECORDS,
    CREATE_RECORDS,
    UPDATE_RECORDS,
    DELETE_RECORDS,
    ACCESS_NETWORK,
    ACCESS_ACCOUNT,
    ACCESS_SENSITIVE
}

/**
 * Functional category of connected service adapters.
 */
enum class PluginCategory {
    API_SERVICE,
    CALENDAR,
    CLOUD_STORAGE,
    MESSAGING,
    NOTES,
    PRODUCTIVITY,
    RESEARCH_DATA
}

/**
 * Lifecycle operational state of a plugin.
 */
enum class PluginState {
    ENABLED,
    DISABLED,
    ERROR,
    UNCONFIGURED
}

/**
 * Standardized error classification codes for plugin failures.
 */
enum class PluginErrorCode {
    NETWORK_ERROR,
    AUTH_ERROR,
    PERMISSION_DENIED,
    RATE_LIMITED,
    INVALID_REQUEST,
    INVALID_RESPONSE,
    TIMEOUT,
    SERVICE_UNAVAILABLE,
    PLUGIN_DISABLED,
    UNSUPPORTED_OPERATION,
    UNKNOWN_ERROR
}

/**
 * Structured error details returned by plugin executions.
 */
data class PluginError(
    val code: PluginErrorCode,
    val message: String,
    val details: String? = null,
    val retryable: Boolean = false
)

/**
 * Manifest defining immutable metadata, identity, and security declarations of a plugin.
 */
data class PluginManifest(
    val id: String,
    val displayName: String,
    val version: String,
    val description: String,
    val providerName: String,
    val category: PluginCategory,
    val capabilities: Set<PluginCapability>,
    val permissions: Set<PluginPermission>,
    val author: String = "JARVIS Core",
    val isBuiltIn: Boolean = true,
    val requiresCredentials: Boolean = false,
    val credentialPlaceholder: String? = null,
    val documentationUrl: String? = null
)

/**
 * Health and operational telemetry for an installed plugin.
 */
data class PluginStatus(
    val state: PluginState = PluginState.ENABLED,
    val isHealthy: Boolean = true,
    val lastSyncTimestamp: Long? = null,
    val lastErrorMessage: String? = null,
    val lastErrorCode: PluginErrorCode? = null,
    val failureCount: Int = 0
)

/**
 * Persistent configuration and encrypted credentials reference for a plugin.
 */
data class PluginConfig(
    val pluginId: String,
    val enabled: Boolean = true,
    val customEndpoint: String? = null,
    val encryptedCredentials: String? = null,
    val settings: Map<String, String> = emptyMap()
)

/**
 * Health check telemetry returned from connection tests.
 */
data class PluginHealthCheck(
    val isHealthy: Boolean,
    val latencyMs: Long,
    val message: String,
    val lastChecked: Long = System.currentTimeMillis()
)

/**
 * Structured result produced by a plugin action.
 */
data class PluginResult(
    val success: Boolean,
    val data: Map<String, Any?> = emptyMap(),
    val error: PluginError? = null,
    val rawOutput: String = "",
    val executionTimeMs: Long = 0L,
    val itemsCount: Int = 0
)

/**
 * Definition of a single tool exposed by a plugin to AgentBrain.
 */
data class PluginToolDefinition(
    val toolId: String,
    val pluginId: String,
    val name: String,
    val description: String,
    val requiredCapabilities: Set<PluginCapability>,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val inputSchema: Map<String, String> = emptyMap()
)
