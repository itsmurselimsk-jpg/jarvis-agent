package com.example.jarvis.brain

import android.util.Log
import com.example.jarvis.personality.JarvisPersonality
import com.example.jarvis.personality.LanguageStyle
import java.util.Locale

/**
 * Compound Query & Multi-Step Task Planner (inspired by isair/jarvis reply/planner.py)
 * 
 * Breaks compound user requests ("Turn on torch and set volume to 80%", "Check weather then remind me")
 * into sequential, actionable sub-steps and executes them deterministically.
 */
object CompoundTaskPlanner {

    private const val TAG = "CompoundTaskPlanner"
    private const val MAX_STEPS = 5

    data class PlannedStep(
        val stepIndex: Int,
        val subQuery: String,
        val toolName: String?,
        val toolArg: String?
    )

    data class ExecutionResult(
        val step: PlannedStep,
        val success: Boolean,
        val output: String
    )

    /**
     * Determines whether the user directive contains compound, multi-action directives.
     */
    fun isCompoundQuery(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.length < 10) return false

        val conjunctivePatterns = listOf(
            Regex("""\b(and then|and also|aur phir|aur saath mein|tarpor|ar sathe)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b\s+aur\s+\b""", RegexOption.IGNORE_CASE),
            Regex("""\b\s+and\s+\b""", RegexOption.IGNORE_CASE),
            Regex("""\b\s+then\s+\b""", RegexOption.IGNORE_CASE),
            Regex("""\s*,\s*(?:aur|and|then)\s+""", RegexOption.IGNORE_CASE)
        )

        return conjunctivePatterns.any { it.containsMatchIn(trimmed) }
    }

    /**
     * Splits a compound query into ordered sub-steps (up to MAX_STEPS).
     */
    fun decomposeIntoSubSteps(query: String): List<String> {
        val delimiters = Regex("""\b(?:and then|and also|aur phir|aur saath mein|tarpor|ar sathe|aur|and|then)\b|,+""", RegexOption.IGNORE_CASE)
        val rawParts = query.split(delimiters)
            .map { it.trim() }
            .filter { it.length >= 3 }

        return rawParts.take(MAX_STEPS)
    }

    /**
     * Executes the planned sub-steps sequentially using the agent's tool registry.
     */
    suspend fun executeSubSteps(
        subSteps: List<String>,
        registry: ToolRegistry,
        context: ToolContext,
        lang: LanguageStyle
    ): List<ExecutionResult> {
        val results = mutableListOf<ExecutionResult>()

        for ((index, stepQuery) in subSteps.withIndex()) {
            val planned = resolveStepToTool(stepQuery, index + 1)
            if (planned.toolName != null) {
                val tool = registry.getTool(planned.toolName)
                if (tool != null) {
                    try {
                        val result = tool.execute(planned.toolArg ?: "", context)
                        results.add(ExecutionResult(planned, result.success, result.output))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing step: ${planned.subQuery}", e)
                        results.add(ExecutionResult(planned, false, e.message ?: "Execution failed"))
                    }
                } else {
                    results.add(ExecutionResult(planned, false, "Tool ${planned.toolName} unavailable"))
                }
            } else {
                results.add(ExecutionResult(planned, true, "Acknowledged directive: $stepQuery"))
            }
        }

        return results
    }

    /**
     * Formats the final synthesized response of all completed sub-tasks.
     */
    fun formatCompoundExecutionReport(
        results: List<ExecutionResult>,
        lang: LanguageStyle
    ): String {
        val sb = StringBuilder()
        val allSuccessful = results.all { it.success }

        when (lang) {
            LanguageStyle.HINGLISH -> {
                sb.appendLine(if (allSuccessful) "Bilkul Sir! Maine dono/saare tasks execute kar diye hain:" else "Sir, kuch tasks execute hue hain:")
            }
            LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> {
                sb.appendLine(if (allSuccessful) "Ekdom Sir! Sobgulo kaj complete kore diyechhi:" else "Sir, kichu steps execute hoyechhe:")
            }
            LanguageStyle.HINDI -> {
                sb.appendLine(if (allSuccessful) "बिल्कुल सर! आपके सभी निर्देश पूरे कर दिए गए हैं:" else "सर, कुछ निर्देश पूरे किए गए हैं:")
            }
            else -> {
                sb.appendLine(if (allSuccessful) "All directives executed flawlessly, sir:" else "Multi-step plan completed with partial status:")
            }
        }

        results.forEachIndexed { i, res ->
            val icon = if (res.success) "✓" else "✗"
            sb.appendLine("$icon Step ${i + 1}: ${res.output.trim()}")
        }

        return sb.toString().trim()
    }

    private fun resolveStepToTool(subQuery: String, stepIndex: Int): PlannedStep {
        val lower = subQuery.lowercase(Locale.ROOT)
        return when {
            lower.contains("torch") || lower.contains("flashlight") -> {
                val arg = if (lower.contains("off") || lower.contains("band")) "off" else "on"
                PlannedStep(stepIndex, subQuery, "flashlight", arg)
            }
            lower.contains("volume") || lower.contains("awaaz") || lower.contains("awaz") -> {
                val pct = Regex("""\b(\d{1,3})\b""").find(lower)?.groupValues?.get(1) ?: "70"
                PlannedStep(stepIndex, subQuery, "volume", pct)
            }
            lower.contains("wifi") || lower.contains("wi-fi") -> {
                val arg = if (lower.contains("off") || lower.contains("band")) "off" else "on"
                PlannedStep(stepIndex, subQuery, "wifi", arg)
            }
            lower.contains("battery") -> {
                PlannedStep(stepIndex, subQuery, "battery", "")
            }
            lower.contains("weather") || lower.contains("mausam") || lower.contains("abhawa") -> {
                val location = Regex("""(?:in|at|of|ka)\s+([A-Za-z]+)""").find(lower)?.groupValues?.get(1) ?: ""
                PlannedStep(stepIndex, subQuery, "weather", location)
            }
            lower.contains("task") || lower.contains("reminder") || lower.contains("yaad") -> {
                PlannedStep(stepIndex, subQuery, "tasks", subQuery)
            }
            else -> {
                PlannedStep(stepIndex, subQuery, null, null)
            }
        }
    }
}
