package com.jarvis.ai.brain.tools

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.jarvis.ai.brain.Tool
import com.jarvis.ai.brain.ToolContext
import com.jarvis.ai.brain.ToolResult
import com.jarvis.ai.model.RiskLevel

/**
 * Screen Action & UI Automation Tool (Feature 10: Screen Vision ➔ Action).
 * Enables JARVIS to autonomously tap visible on-screen elements, buttons, and menus
 * identified via Accessibility tree or Screen OCR inspection.
 */
class ScreenClickTool : Tool {
    override val name: String = "ScreenClick"
    override val description: String =
        "Clicks or taps on a button, link, or text element currently displayed on the screen. Usage: 'ScreenClick: Submit' or 'ScreenClick: Next'."
    override val riskLevel: RiskLevel = RiskLevel.CONFIRMATION
    override val permissions: List<String> = emptyList()

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val cleanTarget = input
            .removePrefix("ScreenClick:")
            .removePrefix("click:")
            .removePrefix("tap:")
            .removePrefix("button:")
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        if (cleanTarget.isBlank()) {
            return ToolResult(false, "No button or element text specified.")
        }

        val service = JarvisAccessibilityService.getInstance()
        if (service == null) {
            return ToolResult(
                success = false,
                output = "Accessibility Service is not running. Please enable JARVIS in Android Settings ➔ Accessibility."
            )
        }

        val success = service.tapOnText(cleanTarget)
        return if (success) {
            ToolResult(true, "Successfully clicked on element: '$cleanTarget'")
        } else {
            // Also try case-insensitive partial search
            ToolResult(
                success = false,
                output = "Element '$cleanTarget' was not found or is not clickable on the active window. Please ensure it is visible on screen."
            )
        }
    }
}
