package com.example.jarvis.recovery

import com.example.jarvis.provider.ToolDecision
import org.json.JSONObject

/**
 * Intelligent AI response repair and parser.
 * Handles common LLM quirks such as markdown backticks, conversational preambles,
 * and trailing whitespace without crashing or inventing tool decisions.
 */
object ResponseRepair {

    /**
     * Extracts and repairs JSON content from raw LLM output strings.
     */
    fun extractCleanJson(rawText: String): JSONObject? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        // 1. Direct attempt
        try {
            return JSONObject(trimmed)
        } catch (_: Exception) {
            // Proceed to repair pass
        }

        // 2. Extract JSON enclosed in markdown code blocks: ```json ... ``` or ``` ... ```
        val codeBlockRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""", RegexOption.IGNORE_CASE)
        val match = codeBlockRegex.find(trimmed)
        if (match != null) {
            val candidate = match.groups[1]?.value?.trim() ?: ""
            try {
                return JSONObject(candidate)
            } catch (_: Exception) {
                // Continue to bracket search
            }
        }

        // 3. Extract substring between first '{' and last '}'
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            val candidate = trimmed.substring(firstBrace, lastBrace + 1).trim()
            try {
                return JSONObject(candidate)
            } catch (_: Exception) {
                // Fall through
            }
        }

        return null
    }

    /**
     * Safely parses a ToolDecision from an AI response with automated repair.
     */
    fun parseToolDecisionWithRepair(
        rawText: String,
        fallbackInput: String
    ): ToolDecision? {
        val json = extractCleanJson(rawText) ?: return null

        return try {
            val useTool = json.optBoolean("useTool", false)
            val toolName = json.optString("toolName", "").ifBlank { null }
            val toolInput = json.optString("toolInput", fallbackInput).ifBlank { fallbackInput }
            val reasoning = json.optString("reasoning", "")
            ToolDecision(useTool = useTool, toolName = toolName, toolInput = toolInput, reasoning = reasoning)
        } catch (_: Exception) {
            null
        }
    }
}
