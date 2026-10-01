package com.jarvis.ai.recovery

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

/**
 * Friendly Bilingual (Hindi + English) Error Recovery UX Formatter.
 * Translates low-level Android exceptions into actionable, polite instructions.
 */
object BilingualErrorUxFormatter {

    fun formatFailure(
        toolName: String,
        rawError: String,
        isHindi: Boolean = false
    ): String {
        val suggestion = when (toolName.lowercase().trim()) {
            "flashlight" -> if (isHindi) {
                "Phone ki Camera/Torch permission check karein ya device ko unlock karein."
            } else {
                "Please verify Camera/Torch permissions or unlock your screen."
            }

            "phonecall" -> if (isHindi) {
                "Contact ka naam confirm karein aur Call Phone permission verify karein."
            } else {
                "Please check the contact name or ensure Phone Call permission is enabled in Settings."
            }

            "applauncher" -> if (isHindi) {
                "Check karein ki ye app aapke phone mein installed hai ya Play Store se download karein."
            } else {
                "Ensure the app is installed on your device or download it from the Play Store."
            }

            "accessibility", "accessibilityagent", "screenclick" -> if (isHindi) {
                "Android Settings ➔ Accessibility mein jaakar JARVIS Assistant service enable karein."
            } else {
                "Please enable the JARVIS Accessibility Service in Android Settings ➔ Accessibility."
            }

            "whatsappmessage", "smsmessage" -> if (isHindi) {
                "SMS permission grant karein aur number ya contact format check karein."
            } else {
                "Please verify SMS/Contact permissions and ensure the recipient number is valid."
            }

            "volume", "brightness" -> if (isHindi) {
                "System Settings permission grant karein."
            } else {
                "Please grant 'Modify System Settings' permission to adjust hardware levels."
            }

            else -> if (isHindi) {
                "Command ko simple shabdon mein bolein ya network connect karke dobara try karein."
            } else {
                "Try rephrasing your command in simpler terms or check network connection."
            }
        }

        return if (isHindi) {
            "⚠️ **Protocol Execute Nahi Hua ($toolName)**\n$rawError\n\n💡 **Aap kya kar sakte hain:**\n$suggestion"
        } else {
            "⚠️ **Protocol Interrupted ($toolName)**\n$rawError\n\n💡 **Recommended Action:**\n$suggestion"
        }
    }
}
