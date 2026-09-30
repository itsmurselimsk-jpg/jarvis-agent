package com.example.jarvis.plugin

/**
 * Base interface for all connected service adapters and plugins in JARVIS.
 */
interface Plugin {
    /**
     * Immutable manifest describing identity, capabilities, and permissions.
     */
    val manifest: PluginManifest

    /**
     * List of discrete tools exposed by this plugin to AgentBrain.
     */
    val exposedTools: List<PluginToolDefinition>

    /**
     * Lifecycle initialization called when the plugin is enabled or registered.
     */
    suspend fun initialize(context: PluginContext): Boolean

    /**
     * Executes a specific action requested by a tool invocation.
     */
    suspend fun executeAction(
        actionName: String,
        params: Map<String, Any?>,
        context: PluginContext
    ): PluginResult

    /**
     * Probes the health and connectivity of the underlying service.
     */
    suspend fun testConnection(context: PluginContext): PluginHealthCheck

    /**
     * Clean-up callback when user disconnects or revokes plugin access.
     */
    suspend fun onDisconnect(context: PluginContext)
}
