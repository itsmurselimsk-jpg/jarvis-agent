package com.example.jarvis.translation

import java.util.Locale

/**
 * Standard supported languages for JARVIS dedicated translation engine.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val aliases: List<String>
) {
    AUTO("auto", "Auto Detect", "স্বয়ংক্রিয়", listOf("auto", "detect", "automatic", "any")),
    ENGLISH("en", "English", "English", listOf("english", "en", "eng", "angrezi", "ingreji")),
    BENGALI("bn", "Bengali", "বাংলা", listOf("bengali", "bangla", "বাংলা", "bn", "ben")),
    HINDI("hi", "Hindi", "हिंदी", listOf("hindi", "हिंदी", "hi", "hin")),
    URDU("ur", "Urdu", "اردو", listOf("urdu", "اردو", "ur", "urd")),
    ASSAMESE("as", "Assamese", "অসমীয়া", listOf("assamese", "asomiya", "অসমীয়া", "as", "asm")),
    NEPALI("ne", "Nepali", "नेपाली", listOf("nepali", "नेपाली", "ne", "nep")),
    SPANISH("es", "Spanish", "Español", listOf("spanish", "es", "spa", "espanol")),
    FRENCH("fr", "French", "Français", listOf("french", "fr", "fra", "francais")),
    GERMAN("de", "German", "Deutsch", listOf("german", "de", "deu", "deutsch")),
    ARABIC("ar", "Arabic", "العربية", listOf("arabic", "ar", "ara")),
    CHINESE("zh", "Chinese", "中文", listOf("chinese", "zh", "mandarin")),
    JAPANESE("ja", "Japanese", "日本語", listOf("japanese", "ja", "jpn"));

    companion object {
        fun fromCodeOrName(input: String?): SupportedLanguage? {
            if (input.isNullOrBlank()) return null
            val clean = input.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { lang ->
                lang.code.equals(clean, ignoreCase = true) ||
                        lang.displayName.equals(clean, ignoreCase = true) ||
                        lang.nativeName.equals(clean, ignoreCase = true) ||
                        lang.aliases.any { it.equals(clean, ignoreCase = true) }
            }
        }

        fun resolveTargetCode(input: String?, defaultCode: String = "en"): String {
            if (input.isNullOrBlank()) return defaultCode
            val matched = fromCodeOrName(input)
            return matched?.code ?: input.trim().lowercase(Locale.ROOT)
        }

        fun getDisplayName(code: String): String {
            val matched = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
            return matched?.displayName ?: code.uppercase(Locale.ROOT)
        }
    }
}

/**
 * Structured request for translating text.
 */
data class TranslationRequest(
    val sourceText: String,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en",
    val preserveFormatting: Boolean = true
)

/**
 * Structured result produced by the Translation Engine.
 */
data class TranslationResult(
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val qualityIndicator: String? = null,
    val success: Boolean,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
