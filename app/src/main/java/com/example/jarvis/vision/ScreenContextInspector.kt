package com.example.jarvis.vision

import android.content.Context
import com.example.jarvis.accessibility.JarvisAccessibilityService
import com.example.jarvis.accessibility.UiElementNode

data class ScreenContextResult(
    val success: Boolean,
    val visibleText: String,
    val elementCount: Int = 0,
    val message: String? = null,
    val containsSensitiveData: Boolean = false
)

/**
 * Safe, user-approved screen context inspector.
 * Respects Android privacy boundaries and will not scrape when service is inactive.
 */
object ScreenContextInspector {

    fun inspectCurrentScreen(context: Context): ScreenContextResult {
        if (!JarvisAccessibilityService.isServiceEnabled(context)) {
            return ScreenContextResult(
                success = false,
                visibleText = "",
                message = "Screen context extraction requires JARVIS Accessibility Service to be enabled in Android Settings."
            )
        }

        val service = JarvisAccessibilityService.getInstance()
        if (service == null) {
            return ScreenContextResult(
                success = false,
                visibleText = "",
                message = "Accessibility service is configured but not currently active in memory."
            )
        }

        val nodes: List<UiElementNode> = service.inspectVisibleUi()
        if (nodes.isEmpty()) {
            return ScreenContextResult(
                success = true,
                visibleText = "",
                message = "Accessibility window inspection completed: No text elements were found on current foreground surface."
            )
        }

        // Safe filtering: Ignore password fields and hidden editable tokens
        val safeTextLines = nodes
            .filter { node ->
                val cls = node.className.lowercase()
                !cls.contains("password") && !cls.contains("pin")
            }
            .mapNotNull { node ->
                when {
                    node.text.isNotBlank() -> node.text.trim()
                    node.contentDescription.isNotBlank() -> node.contentDescription.trim()
                    else -> null
                }
            }
            .distinct()

        val fullText = safeTextLines.joinToString("\n")
        val isSensitive = SensitiveDataFilter.containsSensitiveData(fullText)

        return ScreenContextResult(
            success = true,
            visibleText = fullText,
            elementCount = safeTextLines.size,
            containsSensitiveData = isSensitive,
            message = "Successfully inspected ${safeTextLines.size} visible elements."
        )
    }
}
