package com.example.jarvis.plugin

import com.example.jarvis.brain.Tool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.brain.ToolResult
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.recovery.RetryPolicy
import com.example.jarvis.recovery.executeWithRetry
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/**
 * Bridges a [PluginToolDefinition] exposed by a connected service plugin into JARVIS's [Tool] system.
 * Handles sandboxed parameter parsing, security enforcement, bounded retries, and output sanitization.
 */
class PluginAdapterTool(
    val plugin: Plugin,
    val toolDefinition: PluginToolDefinition,
    val registry: PluginRegistry
) : Tool {

    override val name: String = toolDefinition.name
    override val description: String = toolDefinition.description
    override val riskLevel: RiskLevel = toolDefinition.riskLevel
    override val permissions: List<String> = toolDefinition.requiredCapabilities.map { "CAPABILITY_${it.name}" }

    // Invocation depth tracker for recursion defense
    private var currentInvocationDepth = 0

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val startTime = System.currentTimeMillis()

        // 1. Verify plugin is enabled in registry
        if (!registry.isPluginEnabled(plugin.manifest.id)) {
            return ToolResult(
                success = false,
                output = "Plugin '${plugin.manifest.displayName}' is currently disabled in Connected Services settings.",
                verified = false,
                metadata = mapOf(
                    "pluginId" to plugin.manifest.id,
                    "errorCode" to PluginErrorCode.PLUGIN_DISABLED.name
                )
            )
        }

        // 2. Protect against recursive plugin execution loops
        if (!PluginSafetyEngine.checkCallDepth(currentInvocationDepth)) {
            return ToolResult(
                success = false,
                output = "Security Error: Recursive plugin call depth limit exceeded.",
                verified = false,
                metadata = mapOf(
                    "pluginId" to plugin.manifest.id,
                    "errorCode" to PluginErrorCode.PERMISSION_DENIED.name
                )
            )
        }

        // 3. Build sandboxed execution context
        val callId = "call-${System.currentTimeMillis() % 100000}"
        val pluginContext = DefaultPluginContext(
            callId = callId,
            pluginId = plugin.manifest.id,
            rawCredentialSupplier = { registry.getDecryptedCredential(plugin.manifest.id) },
            settingsMap = registry.getPluginConfig(plugin.manifest.id)?.settings ?: emptyMap(),
            logger = { msg ->
                context.repository.logActivity(
                    type = ActivityType.SYSTEM_EVENT,
                    title = "Plugin [${plugin.manifest.displayName}]",
                    detail = msg,
                    risk = RiskLevel.SAFE
                )
            }
        )

        // 4. Parse action parameters from input string
        val params = parseParams(input)

        // 5. Determine whether to allow bounded retries (strictly read-only / search actions)
        val isReadOnly = toolDefinition.requiredCapabilities.all {
            it == PluginCapability.READ || it == PluginCapability.SEARCH || it == PluginCapability.ACCOUNT_DATA
        }

        currentInvocationDepth++
        return try {
            val pluginResult = kotlinx.coroutines.withTimeout(15000L) {
                if (isReadOnly) {
                    val retryResult = executeWithRetry(
                        policy = RetryPolicy.READ_ONLY_TOOL,
                        operationName = "PluginAction_${toolDefinition.name}",
                        source = plugin.manifest.displayName
                    ) {
                        val res = plugin.executeAction(toolDefinition.name, params, pluginContext)
                        if (!res.success && res.error?.retryable == true) {
                            throw java.io.IOException(res.error.message ?: "Retryable plugin error")
                        }
                        res
                    }
                    retryResult.getOrElse { errorThrowable ->
                        PluginResult(
                            success = false,
                            error = PluginError(
                                code = PluginErrorCode.NETWORK_ERROR,
                                message = errorThrowable.message ?: "Operation failed after bounded retry",
                                retryable = false
                            ),
                            rawOutput = "Service call failed after retries: ${errorThrowable.message}"
                        )
                    }
                } else {
                    // Destructive / state-modifying actions (CREATE, DELETE, WRITE) are NEVER retried automatically
                    plugin.executeAction(toolDefinition.name, params, pluginContext)
                }
            }

            val elapsed = System.currentTimeMillis() - startTime

            // 6. Security sanitization: Defuse prompt injection and redact secrets
            val sanitizedOutput = PluginSafetyEngine.sanitizePluginOutput(pluginResult.rawOutput)
            val secret = registry.getDecryptedCredential(plugin.manifest.id)
            val finalOutput = PluginSafetyEngine.redactSecrets(sanitizedOutput, listOfNotNull(secret))

            // 7. Update status telemetry in registry
            val currentStatus = registry.getPluginStatus(plugin.manifest.id)
            if (pluginResult.success) {
                registry.updatePluginStatus(
                    plugin.manifest.id,
                    currentStatus.copy(
                        isHealthy = true,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        lastErrorMessage = null,
                        lastErrorCode = null
                    )
                )
            } else {
                registry.updatePluginStatus(
                    plugin.manifest.id,
                    currentStatus.copy(
                        lastErrorMessage = pluginResult.error?.message,
                        lastErrorCode = pluginResult.error?.code,
                        failureCount = currentStatus.failureCount + 1
                    )
                )
            }

            // 8. Log activity safely
            context.repository.logActivity(
                type = ActivityType.TOOL_EXECUTION,
                title = "Connected Service: ${plugin.manifest.displayName}",
                detail = "Executed tool '${toolDefinition.name}' with result: ${if (pluginResult.success) "SUCCESS" else "FAILED"} (${elapsed}ms)",
                risk = toolDefinition.riskLevel
            )

            ToolResult(
                success = pluginResult.success,
                output = finalOutput,
                visualDetail = if (pluginResult.itemsCount > 0) "Retrieved ${pluginResult.itemsCount} items from ${plugin.manifest.displayName}" else null,
                verified = pluginResult.success,
                metadata = mapOf(
                    "pluginId" to plugin.manifest.id,
                    "toolId" to toolDefinition.toolId,
                    "executionTimeMs" to elapsed.toString(),
                    "itemsCount" to pluginResult.itemsCount.toString(),
                    "errorCode" to (pluginResult.error?.code?.name ?: "")
                )
            )
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            ToolResult(
                success = false,
                output = "Plugin execution timed out after 15,000ms. Service response was delayed.",
                verified = false,
                metadata = mapOf(
                    "pluginId" to plugin.manifest.id,
                    "toolId" to toolDefinition.toolId,
                    "errorCode" to PluginErrorCode.TIMEOUT.name
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            ToolResult(
                success = false,
                output = "Plugin execution encountered an error: ${t.message ?: "Unknown error"}",
                verified = false,
                metadata = mapOf(
                    "pluginId" to plugin.manifest.id,
                    "toolId" to toolDefinition.toolId,
                    "error" to (t.message ?: "Unknown")
                )
            )
        } finally {
            currentInvocationDepth--
        }
    }

    private fun parseParams(input: String): Map<String, Any?> {
        val trimmed = input.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return try {
                val json = JSONObject(trimmed)
                val map = mutableMapOf<String, Any?>()
                json.keys().forEach { key ->
                    map[key] = json.opt(key)
                }
                map
            } catch (_: Throwable) {
                fallbackMap(trimmed)
            }
        }
        return fallbackMap(trimmed)
    }

    private fun fallbackMap(trimmed: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        map["query"] = trimmed
        map["title"] = trimmed
        map["content"] = trimmed
        map["recordId"] = trimmed
        map["noteId"] = trimmed
        map["text"] = trimmed
        return map
    }
}
