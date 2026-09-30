package com.example.jarvis.plugin

import kotlinx.coroutines.CancellationException

/**
 * Base adapter class encapsulating standard validation, capability verification,
 * timing, and exception mapping for connected services.
 */
abstract class ConnectedServiceAdapter(
    override val manifest: PluginManifest
) : Plugin {

    /**
     * Internal implementation of action dispatching after capability validation.
     */
    protected abstract suspend fun onExecuteAction(
        actionName: String,
        params: Map<String, Any?>,
        context: PluginContext
    ): PluginResult

    override suspend fun initialize(context: PluginContext): Boolean {
        context.log("Initializing service adapter: ${manifest.displayName}")
        return true
    }

    override suspend fun executeAction(
        actionName: String,
        params: Map<String, Any?>,
        context: PluginContext
    ): PluginResult {
        val startTime = System.currentTimeMillis()

        // 1. Verify action tool definition exists in exposedTools
        val toolDef = exposedTools.find { it.name.equals(actionName, ignoreCase = true) || it.toolId.equals(actionName, ignoreCase = true) }
        if (toolDef == null) {
            return PluginResult(
                success = false,
                error = PluginError(
                    code = PluginErrorCode.UNSUPPORTED_OPERATION,
                    message = "Action '$actionName' is not exposed by plugin '${manifest.id}'"
                ),
                rawOutput = "Error: Unsupported action '$actionName'",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 2. Verify all required capabilities for this tool are declared in the manifest
        for (requiredCap in toolDef.requiredCapabilities) {
            if (!manifest.capabilities.contains(requiredCap)) {
                return PluginResult(
                    success = false,
                    error = PluginError(
                        code = PluginErrorCode.PERMISSION_DENIED,
                        message = "Capability '$requiredCap' requested by action '$actionName' is NOT declared in manifest for '${manifest.id}'"
                    ),
                    rawOutput = "Security Violation: Undeclared capability '$requiredCap'",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        }

        // 3. Execute with exception translation
        return try {
            val result = onExecuteAction(actionName, params, context)
            val elapsed = System.currentTimeMillis() - startTime
            result.copy(executionTimeMs = elapsed)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val elapsed = System.currentTimeMillis() - startTime
            val (errorCode, retryable) = classifyThrowable(t)
            PluginResult(
                success = false,
                error = PluginError(
                    code = errorCode,
                    message = t.message ?: "Unknown plugin runtime error",
                    details = t.stackTraceToString().take(500),
                    retryable = retryable
                ),
                rawOutput = "Plugin execution failed: ${t.message ?: "Unknown error"}",
                executionTimeMs = elapsed
            )
        }
    }

    override suspend fun onDisconnect(context: PluginContext) {
        context.log("Disconnecting service adapter: ${manifest.displayName}")
    }

    private fun classifyThrowable(t: Throwable): Pair<PluginErrorCode, Boolean> {
        val msg = (t.message ?: "").lowercase()
        return when {
            msg.contains("timeout") || msg.contains("timed out") -> PluginErrorCode.TIMEOUT to true
            msg.contains("401") || msg.contains("auth") || msg.contains("unauthorized") || msg.contains("forbidden") -> PluginErrorCode.AUTH_ERROR to false
            msg.contains("429") || msg.contains("rate limit") || msg.contains("quota") -> PluginErrorCode.RATE_LIMITED to false
            msg.contains("network") || msg.contains("connect") || msg.contains("socket") || msg.contains("host") -> PluginErrorCode.NETWORK_ERROR to true
            msg.contains("unavailable") || msg.contains("503") || msg.contains("500") -> PluginErrorCode.SERVICE_UNAVAILABLE to true
            msg.contains("invalid") || msg.contains("malformed") -> PluginErrorCode.INVALID_REQUEST to false
            else -> PluginErrorCode.UNKNOWN_ERROR to false
        }
    }
}
