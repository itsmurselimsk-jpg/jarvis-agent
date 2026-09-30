package com.example.jarvis.translation

import java.util.Locale

/**
 * Lightweight deterministic offline fallback dictionary for common greetings and phrases.
 * Covers English, Bengali, Hindi, Urdu, Assamese, and Nepali.
 * When full text translation cannot be completed offline, it provides clear and accurate limitation reporting.
 */
object OfflineTranslationFallback {

    // Common phrases mapped by standard English key
    private data class Phrase(
        val en: String,
        val bn: String,
        val hi: String,
        val ur: String,
        val `as`: String,
        val ne: String
    ) {
        fun getForLang(langCode: String): String? {
            return when (langCode.lowercase(Locale.ROOT)) {
                "en" -> en
                "bn" -> bn
                "hi" -> hi
                "ur" -> ur
                "as" -> `as`
                "ne" -> ne
                else -> null
            }
        }
    }

    private val commonPhrases = listOf(
        Phrase(
            en = "Hello",
            bn = "হ্যালো",
            hi = "नमस्ते",
            ur = "ہیلو",
            `as` = "নমস্কাৰ",
            ne = "नमस्ते"
        ),
        Phrase(
            en = "Good morning",
            bn = "সুপ্রভাত",
            hi = "शुभ प्रभात",
            ur = "صبح بخیر",
            `as` = "সু-প্ৰভাত",
            ne = "शुभ प्रभात"
        ),
        Phrase(
            en = "Good evening",
            bn = "শুভ সন্ধ্যা",
            hi = "शुभ संध्या",
            ur = "شب بخیر",
            `as` = "শুভ সন্ধিয়া",
            ne = "शुभ साँझ"
        ),
        Phrase(
            en = "Good night",
            bn = "শুভ রাত্রি",
            hi = "शुभ रात्रि",
            ur = "شب بخیر",
            `as` = "শুভ ৰাত্ৰি",
            ne = "शुभ रात्री"
        ),
        Phrase(
            en = "Thank you",
            bn = "ধন্যবাদ",
            hi = "धन्यवाद",
            ur = "شکریہ",
            `as` = "ধন্যবাদ",
            ne = "धन्यवाद"
        ),
        Phrase(
            en = "Thank you very much",
            bn = "আপনাকে অনেক ধন্যবাদ",
            hi = "आपका बहुत-बहुत धन्यवाद",
            ur = "آپ کا بہت شکریہ",
            `as` = "আপোনাক বহুত ধন্যবাদ",
            ne = "धेरै धेरै धन्यवाद"
        ),
        Phrase(
            en = "How are you?",
            bn = "আপনি কেমন আছেন?",
            hi = "आप कैसे हैं?",
            ur = "آپ کیسے ہیں؟",
            `as` = "আপুনি কেনেকুৱা আছে?",
            ne = "तपाईंलाई कस्तो छ?"
        ),
        Phrase(
            en = "I am fine",
            bn = "আমি ভালো আছি",
            hi = "मैं ठीक हूँ",
            ur = "میں ٹھیک ہوں",
            `as` = "মই ভালে আছোঁ",
            ne = "म ठीक छु"
        ),
        Phrase(
            en = "Welcome",
            bn = "স্বাগতম",
            hi = "स्वागत है",
            ur = "خوش آمدید",
            `as` = "স্বাগতম",
            ne = "स्वागत छ"
        ),
        Phrase(
            en = "Yes",
            bn = "হ্যাঁ",
            hi = "हाँ",
            ur = "ہاں",
            `as` = "হয়",
            ne = "हो"
        ),
        Phrase(
            en = "No",
            bn = "না",
            hi = "नहीं",
            ur = "نہیں",
            `as` = "নহয়",
            ne = "होइन"
        ),
        Phrase(
            en = "Please",
            bn = "দয়া করে",
            hi = "कृपया",
            ur = "براہ کرم",
            `as` = "অনুগ্ৰহ কৰি",
            ne = "कृपया"
        ),
        Phrase(
            en = "Goodbye",
            bn = "বিদায়",
            hi = "अलविदा",
            ur = "خدا حافظ",
            `as` = "বিদায়",
            ne = "अलविदा"
        )
    )

    /**
     * Attempts a lightweight deterministic lookup for basic phrases offline.
     */
    fun attemptOfflineTranslation(
        text: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // If source language is identical to target language
        if (sourceLang.equals(targetLang, ignoreCase = true) && sourceLang != "auto") {
            return TranslationResult(
                translatedText = text,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                qualityIndicator = "Identical language passthrough",
                success = true
            )
        }

        // Search phrase book
        for (phrase in commonPhrases) {
            val isMatch = listOf(phrase.en, phrase.bn, phrase.hi, phrase.ur, phrase.`as`, phrase.ne)
                .any { it.equals(trimmed, ignoreCase = true) }

            if (isMatch) {
                val targetText = phrase.getForLang(targetLang)
                if (targetText != null) {
                    val resolvedSource = when {
                        trimmed.equals(phrase.en, ignoreCase = true) -> "en"
                        trimmed.equals(phrase.bn, ignoreCase = true) -> "bn"
                        trimmed.equals(phrase.hi, ignoreCase = true) -> "hi"
                        trimmed.equals(phrase.ur, ignoreCase = true) -> "ur"
                        trimmed.equals(phrase.`as`, ignoreCase = true) -> "as"
                        trimmed.equals(phrase.ne, ignoreCase = true) -> "ne"
                        else -> sourceLang
                    }

                    return TranslationResult(
                        translatedText = targetText,
                        sourceLanguage = resolvedSource,
                        targetLanguage = targetLang,
                        qualityIndicator = "Deterministic offline phrase dictionary",
                        success = true
                    )
                }
            }
        }

        return null
    }

    /**
     * Reports limitation when offline and phrase is not in the lightweight dictionary.
     */
    fun createOfflineUnavailableResult(
        sourceLang: String,
        targetLang: String
    ): TranslationResult {
        val srcName = SupportedLanguage.getDisplayName(sourceLang)
        val tgtName = SupportedLanguage.getDisplayName(targetLang)
        return TranslationResult(
            translatedText = "",
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            qualityIndicator = "Unavailable offline",
            success = false,
            errorMessage = "Offline neural translation from $srcName to $tgtName is currently unavailable. Please configure an AI Provider (Gemini / OpenAI) in Settings for full neural translation."
        )
    }
}
