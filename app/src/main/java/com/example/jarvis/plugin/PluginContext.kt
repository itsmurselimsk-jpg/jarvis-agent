package com.example.jarvis.plugin

import com.example.jarvis.security.SensitiveDataFilter

/**
 * Narrow, sandboxed execution context provided to plugins.
 * Strictly prevents direct access to Android Context, Raw FileSystem, or Shell commands.
 */
interface PluginContext {
    val callId: String
    val pluginId: String
    val isCancelled: Boolean

    /**
     * Supplies decrypted credentials if configured for this plugin.
     * Returned value must remain strictly in-memory and never be logged or echoed.
     */
    fun getCredential(): String?

    /**
     * Redacts known sensitive patterns and credentials from a text string.
     */
    fun redact(text: String): String

    /**
     * Safely logs internal plugin telemetry without leaking credentials.
     */
    fun log(message: String)

    /**
     * Gets a non-secret configuration setting by key.
     */
    fun getSetting(key: String, defaultValue: String = ""): String
}

/**
 * Standard implementation of [PluginContext] enforcing data isolation and logging sanitization.
 */
class DefaultPluginContext(
    override val callId: String,
    override val pluginId: String,
    private val rawCredentialSupplier: () -> String?,
    private val settingsMap: Map<String, String> = emptyMap(),
    private val logger: (String) -> Unit = {}
) : PluginContext {

    override var isCancelled: Boolean = false

    override fun getCredential(): String? {
        return rawCredentialSupplier()
    }

    override fun redact(text: String): String {
        if (text.isBlank()) return text
        var sanitized = SensitiveDataFilter.redactSensitiveData(text)
        val cred = rawCredentialSupplier()
        if (!cred.isNullOrBlank() && cred.length >= 4) {
            sanitized = sanitized.replace(cred, "[PLUGIN_CREDENTIAL_REDACTED]")
        }
        return sanitized
    }

    override fun log(message: String) {
        val safeMessage = redact(message)
        logger("[$pluginId:$callId] $safeMessage")
    }

    override fun getSetting(key: String, defaultValue: String): String {
        return settingsMap[key] ?: defaultValue
    }
}
