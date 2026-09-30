package com.example.jarvis.plugin

import android.content.Context
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.plugin.builtin.MockNotesPlugin
import com.example.jarvis.plugin.builtin.MockProductivityPlugin
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * High-level manager orchestrating plugin discovery, persistent configuration syncing,
 * security lifecycle, and dynamic tool binding with [AgentBrain].
 */
class PluginManager(
    private val context: Context,
    private val repository: JarvisRepository? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("jarvis_plugin_system_prefs", Context.MODE_PRIVATE)

    val registry: PluginRegistry = PluginRegistry(
        persistenceCallback = { pluginId, config ->
            persistPluginConfig(pluginId, config)
            _pluginsState.value = registry.getAllPlugins()
        }
    )

    private val _pluginsState = MutableStateFlow<List<Plugin>>(emptyList())
    val pluginsState: StateFlow<List<Plugin>> = _pluginsState.asStateFlow()

    init {
        // 1. Register standard built-in plugins
        val productivityPlugin = MockProductivityPlugin()
        val notesPlugin = MockNotesPlugin()
        val googlePlugin = com.example.jarvis.plugin.builtin.GoogleServicesPlugin()

        registry.register(productivityPlugin)
        registry.register(notesPlugin)
        registry.register(googlePlugin)

        // 2. Load stored configurations
        loadStoredConfigs()

        _pluginsState.value = registry.getAllPlugins()
    }

    private fun loadStoredConfigs() {
        for (plugin in registry.getAllPlugins()) {
            val pluginId = plugin.manifest.id
            val rawJson = prefs.getString("config_$pluginId", null)
            if (rawJson != null) {
                try {
                    val json = JSONObject(rawJson)
                    val enabled = json.optBoolean("enabled", true)
                    val customEndpoint = if (json.has("customEndpoint")) json.getString("customEndpoint") else null
                    val encryptedCreds = if (json.has("encryptedCredentials")) json.getString("encryptedCredentials") else null
                    val config = PluginConfig(
                        pluginId = pluginId,
                        enabled = enabled,
                        customEndpoint = customEndpoint,
                        encryptedCredentials = encryptedCreds
                    )
                    registry.setPluginConfig(config)
                } catch (_: Throwable) {
                    // Default configuration remains intact
                }
            }
        }
    }

    private fun persistPluginConfig(pluginId: String, config: PluginConfig) {
        try {
            val json = JSONObject().apply {
                put("pluginId", config.pluginId)
                put("enabled", config.enabled)
                config.customEndpoint?.let { put("customEndpoint", it) }
                config.encryptedCredentials?.let { put("encryptedCredentials", it) }
            }
            prefs.edit().putString("config_$pluginId", json.toString()).apply()
        } catch (_: Throwable) {
            // Non-critical persistence failure
        }
    }

    /**
     * Dynamically syncs all tools exposed by enabled plugins into [AgentBrain.registry].
     */
    fun syncToolsWithBrain(brain: AgentBrain) {
        for (plugin in registry.getAllPlugins()) {
            val isEnabled = registry.isPluginEnabled(plugin.manifest.id)
            for (toolDef in plugin.exposedTools) {
                if (isEnabled) {
                    val adapter = PluginAdapterTool(plugin, toolDef, registry)
                    brain.registry.register(adapter)
                }
            }
        }
    }

    fun togglePluginEnabled(pluginId: String, enabled: Boolean, brain: AgentBrain? = null) {
        registry.setPluginEnabled(pluginId, enabled)
        _pluginsState.value = registry.getAllPlugins()
        brain?.let { syncToolsWithBrain(it) }
    }

    fun saveCredential(pluginId: String, rawSecret: String) {
        registry.saveEncryptedCredential(pluginId, rawSecret)
        _pluginsState.value = registry.getAllPlugins()
    }

    fun revokeAccess(pluginId: String) {
        registry.revokePluginAccess(pluginId)
        _pluginsState.value = registry.getAllPlugins()
    }

    suspend fun testConnection(pluginId: String): PluginHealthCheck {
        val result = registry.checkPluginHealth(pluginId)
        _pluginsState.value = registry.getAllPlugins()
        return result
    }
}
