package com.example.jarvis.recovery

import com.example.jarvis.brain.Tool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.brain.ToolResult
import com.example.jarvis.model.RiskLevel

/**
 * Intelligent Tool Recovery Controller.
 * Ensures read-only and idempotent tools can gracefully recover from transient hardware hiccups
 * while strictly guarding state-altering tools from duplicating external actions.
 */
object ToolRecovery {

    private val STATE_ALTERING_TOOLS = setOf(
        "phonecall",
        "applauncher",
        "openurl",
        "androidsettings",
        "clipboard",
        "flashlight",
        "mediacontrol",
        "tasks",
        "timer",
        "accessibilityagent",
        "youtubesearch",
        "filegeneration"
    )

    /**
     * Determines whether a tool is purely observational, read-only, or safe to retry.
     */
    fun isReadOnlyOrIdempotent(tool: Tool, input: String): Boolean {
        val name = tool.name.lowercase()

        // Tools that are inherently state-changing must not be re-executed automatically
        if (STATE_ALTERING_TOOLS.contains(name)) {
            // Special exception: Volume or Brightness query without numeric target
            if (name == "volume" && !input.contains("%") && !input.contains("up") && !input.contains("down") && !input.contains("set")) {
                return true
            }
            if (name == "tasks" && (input.startsWith("list") || input.contains("show") || input.contains("view"))) {
                return true
            }
            if (name == "memory" && (input.startsWith("search") || input.contains("query") || input.contains("find"))) {
                return true
            }
            return false
        }

        return tool.riskLevel == RiskLevel.SAFE
    }

    /**
     * Executes a tool safely with appropriate bounded retry for read-only tools
     * and strictly single execution for state-altering actions.
     */
    suspend fun executeSafely(
        tool: Tool,
        input: String,
        context: ToolContext,
        onRetryAttempt: (attempt: Int, error: JarvisError) -> Unit = { _, _ -> }
    ): ToolResult {
        val canRetry = isReadOnlyOrIdempotent(tool, input)

        if (!canRetry) {
            // State-altering or high-risk: Execute exactly once. Never repeat.
            return try {
                tool.execute(input, context)
            } catch (t: Throwable) {
                val classified = ErrorClassifier.classify(t, source = "Tool:${tool.name}")
                ToolResult(
                    success = false,
                    output = "Execution failed for ${tool.name}: ${classified.userSafeMessage}",
                    verified = false,
                    metadata = mapOf("errorCategory" to classified.category.name)
                )
            }
        }

        // Read-only / idempotent: Safe to retry up to 2 attempts on transient failure
        val retryPolicy = RetryPolicy.READ_ONLY_TOOL
        val result = executeWithRetry(
            policy = retryPolicy,
            operationName = "Tool:${tool.name}",
            source = "Tool:${tool.name}",
            onRetry = { attempt, error, _ ->
                onRetryAttempt(attempt, error)
            }
        ) { attempt ->
            val toolRes = tool.execute(input, context)
            if (!toolRes.success && attempt < retryPolicy.maxAttempts) {
                throw RuntimeException("Tool returned unsuccessful status: ${toolRes.output}")
            }
            toolRes
        }

        return result.getOrElse { throwable ->
            val classified = ErrorClassifier.classify(throwable, source = "Tool:${tool.name}")
            ToolResult(
                success = false,
                output = "Sensor or query unavailable for ${tool.name}: ${classified.userSafeMessage}",
                verified = false,
                metadata = mapOf("errorCategory" to classified.category.name)
            )
        }
    }
}
