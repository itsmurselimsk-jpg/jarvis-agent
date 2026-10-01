package com.jarvis.ai.voice

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import java.util.Locale

/**
 * Intelligent Language Detector & Multilingual Command Extractor
 * Identifies English, Bengali (বাংলা), Hindi (हिंदी), and mixed code-switching (Banglish, Hinglish).
 */
object LanguageDetector {

    enum class DetectedLanguage {
        ENGLISH,
        BENGALI,
        HINDI
    }

    /**
     * Determines whether text contains Bengali, Hindi, or English characters/patterns.
     */
    fun detectLanguage(text: String): DetectedLanguage {
        // Bengali Unicode range: 0980 - 09FF
        val hasBengaliScript = text.any { it in '\u0980'..'\u09FF' }
        if (hasBengaliScript) return DetectedLanguage.BENGALI

        // Devanagari (Hindi) Unicode range: 0900 - 097F
        val hasDevanagariScript = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagariScript) return DetectedLanguage.HINDI

        // Check phonetic transliterations (Banglish / Hinglish)
        val lower = text.lowercase()
        val bengaliPhonetics = listOf(
            "kholo", "koro", "bondho", "koto", "chalao", "thamao", "bolo",
            "khule dao", "on koro", "off koro", "shuncho", "ki obostha", "musa ke", "musake",
            "bangla", "bangla bolo", "kemon acho", "tumi ke", "ki khobor", "kichu bolo", "bhalo", "valo"
        )
        if (bengaliPhonetics.any { lower.contains(it) }) {
            return DetectedLanguage.BENGALI
        }

        val hindiPhonetics = listOf(
            "kholo", "chalu karo", "band karo", "kitna", "batao", "karo", "bajao",
            "roko", "aawaz", "badhao", "kam karo", "musa ko"
        )
        if (hindiPhonetics.any { lower.contains(it) } && (lower.contains("batao") || lower.contains("karo") || lower.contains("chalu") || lower.contains("band") || lower.contains("aawaz"))) {
            return DetectedLanguage.HINDI
        }

        return DetectedLanguage.ENGLISH
    }

    /**
     * Checks if speech contains wake-words for JARVIS in any supported language.
     * Extracts the remaining command if user spoke "Hey JARVIS open Facebook" in one breath.
     */
    data class WakeWordCheck(
        val isWakeWordPresent: Boolean,
        val commandAfterWake: String = "",
        val language: DetectedLanguage = DetectedLanguage.ENGLISH
    )

    private val wakePatterns = listOf(
        // English
        Regex("""(?i)\b(hey jarvis|ok jarvis|okay jarvis|hello jarvis|jarvis)\b"""),
        // Bengali
        Regex("""(?i)\b(জারভিস|হে জারভিস|ওহে জারভিস|জার্ভিস)\b"""),
        // Hindi
        Regex("""(?i)\b(जार्विस|हे जार्विस|ओके जार्विस)\b""")
    )

    fun inspectForWakeWord(rawText: String): WakeWordCheck {
        val lang = detectLanguage(rawText)
        val matchResult = JarvisWakePhraseMatcher.match(rawText)
        return WakeWordCheck(
            isWakeWordPresent = matchResult.isMatched,
            commandAfterWake = matchResult.commandAfterWake,
            language = lang
        )
    }

    /**
     * Checks if input contains conversation termination keywords.
     */
    fun isTerminationCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val stopWords = listOf(
            "stop", "goodbye", "exit", "cancel", "sleep", "bye", "quit", "shut down",
            "থামো", "বিদায়", "বন্ধ করো", "আর দরকার নেই", "ঘুমাও",
            "रुक जाओ", "अलविदा", "बंद करो", "बस", "सो जाओ"
        )
        return stopWords.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }
    }

    /**
     * Localizes standard operational greetings and status messages into the appropriate language.
     */
    fun getWakeGreeting(language: DetectedLanguage): String {
        return when (language) {
            DetectedLanguage.BENGALI -> "হ্যাঁ স্যার, আমি শুনছি। কীভাবে সাহায্য করতে পারি?"
            DetectedLanguage.HINDI -> "हाँ सर, मैं सुन रहा हूँ। मैं आपकी क्या मदद कर सकता हूँ?"
            DetectedLanguage.ENGLISH -> "Yes Sir, I am listening. How may I assist you?"
        }
    }

    fun getActionSuccessMessage(action: String, target: String, language: DetectedLanguage): String {
        return when (language) {
            DetectedLanguage.BENGALI -> when (action.lowercase()) {
                "open_app" -> "স্যার, $target খোলা হচ্ছে।"
                "youtube_search" -> "ইউটিউবে $target সার্চ করা হচ্ছে।"
                "call" -> "$target এর নম্বরে কল ডায়াল করা হচ্ছে।"
                "flashlight_on" -> "ফ্ল্যাশলাইট অন করা হয়েছে, স্যার।"
                "flashlight_off" -> "ফ্ল্যাশলাইট বন্ধ করা হয়েছে, স্যার।"
                "volume_up" -> "ভলিউম বাড়ানো হয়েছে।"
                "volume_down" -> "ভলিউম কমানো হয়েছে।"
                else -> "$action সম্পন্ন হয়েছে, স্যার।"
            }
            DetectedLanguage.HINDI -> when (action.lowercase()) {
                "open_app" -> "सर, $target खोला जा रहा है।"
                "youtube_search" -> "यूट्यूब पर $target खोजा जा रहा है।"
                "call" -> "$target को कॉल किया जा रहा है।"
                "flashlight_on" -> "फ्लैशलाइट चालू कर दी गई है, सर।"
                "flashlight_off" -> "फ्लैशलाइट बंद कर दी गई है, सर।"
                "volume_up" -> "आवाज़ बढ़ा दी गई है।"
                "volume_down" -> "आवाज़ कम कर दी गई है।"
                else -> "$action पूरा हो गया है, सर।"
            }
            DetectedLanguage.ENGLISH -> when (action.lowercase()) {
                "open_app" -> "Opening $target, Sir."
                "youtube_search" -> "Searching YouTube for $target, Sir."
                "call" -> "Initiating call to $target, Sir."
                "flashlight_on" -> "Flashlight illumination active, Sir."
                "flashlight_off" -> "Flashlight deactivated, Sir."
                "volume_up" -> "Audio stream volume increased."
                "volume_down" -> "Audio stream volume lowered."
                else -> "$action executed successfully, Sir."
            }
        }
    }

    fun getLocaleForLanguage(language: DetectedLanguage): Locale {
        return when (language) {
            DetectedLanguage.BENGALI -> Locale.forLanguageTag("bn-BD")
            DetectedLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            DetectedLanguage.ENGLISH -> Locale.US
        }
    }
}
