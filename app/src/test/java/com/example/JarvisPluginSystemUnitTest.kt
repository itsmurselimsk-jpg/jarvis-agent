package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.plugin.PluginCapability
import com.example.jarvis.plugin.PluginManager
import com.example.jarvis.plugin.PluginRegistry
import com.example.jarvis.plugin.PluginStatus
import com.example.jarvis.plugin.builtin.GoogleServicesPlugin
import com.example.jarvis.provider.JarvisUnifiedAIProvider
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisPluginSystemUnitTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge
    private lateinit var brain: AgentBrain
    private lateinit var pluginManager: PluginManager
    private lateinit var pluginRegistry: PluginRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        bridge = AndroidBridge(context)
        brain = AgentBrain(
            repository = repository,
            bridge = bridge,
            aiProvider = JarvisUnifiedAIProvider(repository),
            onConfirmationRequired = {}
        )
        pluginManager = brain.pluginManager
        pluginRegistry = pluginManager.registry
    }

    // 1. REGISTRATION & LIFECYCLE
    @Test
    fun testPluginRegistryLifecycle() {
        val googlePlugin = GoogleServicesPlugin()
        val allPlugins = pluginRegistry.getAllPlugins()
        assertTrue(allPlugins.any { it.manifest.id == googlePlugin.manifest.id })

        val retrieved = pluginRegistry.getPlugin(googlePlugin.manifest.id)
        assertNotNull(retrieved)
        assertEquals("Google Workspace Services", retrieved!!.manifest.displayName)

        // Test status update
        pluginRegistry.setPluginEnabled(googlePlugin.manifest.id, false)
        val statusDisabled = pluginRegistry.getPluginStatus(googlePlugin.manifest.id)
        assertEquals(com.example.jarvis.plugin.PluginState.DISABLED, statusDisabled.state)

        // Re-enable
        pluginRegistry.setPluginEnabled(googlePlugin.manifest.id, true)
        val statusEnabled = pluginRegistry.getPluginStatus(googlePlugin.manifest.id)
        assertEquals(com.example.jarvis.plugin.PluginState.ENABLED, statusEnabled.state)
    }

    // 2. CAPABILITY DISCOVERY & TOOL SYNCHRONIZATION WITH BRAIN
    @Test
    fun testCapabilityDiscoveryAndBrainToolSync() {
        val tools = brain.registry.getAllTools()
        val pluginTools = tools.filter { it.name.startsWith("Plugin_") || it.name.startsWith("google_") }
        assertTrue(pluginTools.isNotEmpty())

        val googleTool = pluginTools.firstOrNull { it.name.contains("google_calendar_search") }
        assertNotNull(googleTool)
        assertEquals("google_calendar_search", googleTool!!.name)
    }

    // 3. ACTION EXECUTION & CREDENTIAL CHECK
    @Test
    fun testGooglePluginExecutionWithCredentials() = runBlocking {
        val googlePlugin = GoogleServicesPlugin()

        // Set credentials in plugin registry
        pluginRegistry.saveEncryptedCredential(googlePlugin.manifest.id, "oauth_token_test_12345")

        val pluginContext = com.example.jarvis.plugin.DefaultPluginContext(
            callId = "call_001",
            pluginId = googlePlugin.manifest.id,
            rawCredentialSupplier = { pluginRegistry.getDecryptedCredential(googlePlugin.manifest.id) }
        )

        val result = googlePlugin.executeAction(
            actionName = "google_calendar_search",
            params = mapOf("query" to "System Review"),
            context = pluginContext
        )

        assertTrue(result.success)
        assertTrue(result.rawOutput.contains("GOOGLE CALENDAR SEARCH RESULTS"))
        assertTrue(result.rawOutput.contains("Protocol Sync Meeting"))
    }

    // 4. MISSING CREDENTIALS ERROR HANDLING
    @Test
    fun testGooglePluginExecutionMissingCredentials() = runBlocking {
        val googlePlugin = GoogleServicesPlugin()

        // Clear credentials
        pluginRegistry.saveEncryptedCredential(googlePlugin.manifest.id, "")

        val pluginContext = com.example.jarvis.plugin.DefaultPluginContext(
            callId = "call_002",
            pluginId = googlePlugin.manifest.id,
            rawCredentialSupplier = { pluginRegistry.getDecryptedCredential(googlePlugin.manifest.id) }
        )

        val result = googlePlugin.executeAction(
            actionName = "google_calendar_search",
            params = mapOf("query" to "Upcoming"),
            context = pluginContext
        )

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.message.contains("credentials or OAuth token missing"))
    }

    // 5. HEALTH CHECK
    @Test
    fun testPluginHealthCheck() = runBlocking {
        val googlePlugin = GoogleServicesPlugin()

        pluginRegistry.saveEncryptedCredential(googlePlugin.manifest.id, "valid_oauth_token_12345")
        val pCtx = com.example.jarvis.plugin.DefaultPluginContext(
            callId = "call_003",
            pluginId = googlePlugin.manifest.id,
            rawCredentialSupplier = { pluginRegistry.getDecryptedCredential(googlePlugin.manifest.id) }
        )

        val check = googlePlugin.testConnection(pCtx)
        assertTrue(check.isHealthy)
        assertTrue(check.message.contains("active"))
    }
}
