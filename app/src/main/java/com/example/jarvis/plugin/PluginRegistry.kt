package com.example.jarvis.plugin

import com.example.jarvis.security.EncryptedStorage
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe central registry managing plugin lifecycles, configs, security validation,
 * and encrypted credentials.
 */
class PluginRegistry(
    private val persistenceCallback: ((pluginId: String, config: PluginConfig) -> Unit)? = null
) {
    private val plugins = ConcurrentHashMap<String, Plugin>()
    private val configs = ConcurrentHashMap<String, PluginConfig>()
    private val statuses = ConcurrentHashMap<String, PluginStatus>()

    /**
     * Registers a new plugin after strict manifest and tool schema validation.
     * @throws IllegalArgumentException if validation fails or duplicate ID exists.
     */
    @Synchronized
    fun register(plugin: Plugin): ValidationResult {
        val manifest = plugin.manifest

        // 1. Validate manifest
        val manifestValidation = PluginSafetyEngine.validateManifest(manifest)
        if (!manifestValidation.isValid) {
            throw IllegalArgumentException("Plugin manifest validation failed for '${manifest.id}': ${manifestValidation.errors.joinToString()}")
        }

        // 2. Reject duplicates
        if (plugins.containsKey(manifest.id)) {
            throw IllegalArgumentException("Plugin with ID '${manifest.id}' is already registered")
        }

        // 3. Validate exposed tools
        val registeredToolIds = mutableSetOf<String>()
        for (tool in plugin.exposedTools) {
            val toolValidation = PluginSafetyEngine.validateToolDefinition(tool, manifest)
            if (!toolValidation.isValid) {
                throw IllegalArgumentException("Tool '${tool.name}' validation failed in plugin '${manifest.id}': ${toolValidation.errors.joinToString()}")
            }
            if (!registeredToolIds.add(tool.toolId.lowercase())) {
                throw IllegalArgumentException("Duplicate tool ID '${tool.toolId}' in plugin '${manifest.id}'")
            }
        }

        plugins[manifest.id] = plugin

        if (!configs.containsKey(manifest.id)) {
            configs[manifest.id] = PluginConfig(pluginId = manifest.id, enabled = true)
        }
        if (!statuses.containsKey(manifest.id)) {
            statuses[manifest.id] = PluginStatus(state = PluginState.ENABLED, isHealthy = true)
        }

        return ValidationResult(true)
    }

    /**
     * Unregisters a plugin safely.
     */
    @Synchronized
    fun unregister(pluginId: String): Boolean {
        val plugin = plugins.remove(pluginId) ?: return false
        statuses.remove(pluginId)
        return true
    }

    fun getPlugin(pluginId: String): Plugin? = plugins[pluginId]

    fun getAllPlugins(): List<Plugin> = plugins.values.toList()

    fun getEnabledPlugins(): List<Plugin> {
        return plugins.values.filter { isPluginEnabled(it.manifest.id) }
    }

    fun getPluginsByCapability(capability: PluginCapability): List<Plugin> {
        return plugins.values.filter {
            isPluginEnabled(it.manifest.id) && it.manifest.capabilities.contains(capability)
        }
    }

    fun isPluginEnabled(pluginId: String): Boolean {
        return configs[pluginId]?.enabled ?: true
    }

    fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        val current = configs[pluginId] ?: PluginConfig(pluginId = pluginId)
        val updated = current.copy(enabled = enabled)
        configs[pluginId] = updated

        val currentStatus = statuses[pluginId] ?: PluginStatus()
        val newState = if (enabled) PluginState.ENABLED else PluginState.DISABLED
        statuses[pluginId] = currentStatus.copy(state = newState)

        persistenceCallback?.invoke(pluginId, updated)
    }

    fun getPluginConfig(pluginId: String): PluginConfig? = configs[pluginId]

    fun setPluginConfig(config: PluginConfig) {
        configs[config.pluginId] = config
        persistenceCallback?.invoke(config.pluginId, config)
    }

    fun getPluginStatus(pluginId: String): PluginStatus {
        return statuses[pluginId] ?: PluginStatus(state = PluginState.UNCONFIGURED, isHealthy = false)
    }

    fun updatePluginStatus(pluginId: String, status: PluginStatus) {
        statuses[pluginId] = status
    }

    /**
     * Encrypts and securely stores raw credentials for a plugin.
     */
    fun saveEncryptedCredential(pluginId: String, rawCredential: String) {
        val encrypted = EncryptedStorage.encrypt(rawCredential)
        val current = configs[pluginId] ?: PluginConfig(pluginId = pluginId)
        val updated = current.copy(encryptedCredentials = encrypted)
        configs[pluginId] = updated
        persistenceCallback?.invoke(pluginId, updated)
    }

    /**
     * Retrieves decrypted credentials in memory only.
     */
    fun getDecryptedCredential(pluginId: String): String? {
        val encrypted = configs[pluginId]?.encryptedCredentials ?: return null
        if (encrypted.isBlank()) return null
        return EncryptedStorage.decrypt(encrypted)
    }

    /**
     * Safely clears credentials and resets plugin state.
     */
    fun revokePluginAccess(pluginId: String) {
        val current = configs[pluginId] ?: PluginConfig(pluginId = pluginId)
        val updated = current.copy(encryptedCredentials = null)
        configs[pluginId] = updated

        val currentStatus = statuses[pluginId] ?: PluginStatus()
        statuses[pluginId] = currentStatus.copy(
            lastSyncTimestamp = null,
            lastErrorMessage = null,
            lastErrorCode = null
        )

        persistenceCallback?.invoke(pluginId, updated)
    }

    /**
     * Probes the health of a specific plugin.
     */
    suspend fun checkPluginHealth(pluginId: String): PluginHealthCheck {
        val plugin = plugins[pluginId] ?: return PluginHealthCheck(
            isHealthy = false,
            latencyMs = 0L,
            message = "Plugin '$pluginId' not found"
        )

        val context = DefaultPluginContext(
            callId = "health-${System.currentTimeMillis()}",
            pluginId = pluginId,
            rawCredentialSupplier = { getDecryptedCredential(pluginId) },
            settingsMap = configs[pluginId]?.settings ?: emptyMap()
        )

        return try {
            val check = plugin.testConnection(context)
            val currentStatus = statuses[pluginId] ?: PluginStatus()
            statuses[pluginId] = currentStatus.copy(
                isHealthy = check.isHealthy,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastErrorMessage = if (!check.isHealthy) check.message else null
            )
            check
        } catch (t: Throwable) {
            val check = PluginHealthCheck(
                isHealthy = false,
                latencyMs = 0L,
                message = "Health check failed: ${t.message ?: "Unknown error"}"
            )
            val currentStatus = statuses[pluginId] ?: PluginStatus()
            statuses[pluginId] = currentStatus.copy(
                isHealthy = false,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastErrorMessage = t.message
            )
            check
        }
    }

    /**
     * Retrieves all exposed tools from currently enabled plugins.
     */
    fun getAllExposedTools(): List<PluginToolDefinition> {
        return getEnabledPlugins().flatMap { it.exposedTools }
    }

    fun clearAll() {
        plugins.clear()
        configs.clear()
        statuses.clear()
    }
}
