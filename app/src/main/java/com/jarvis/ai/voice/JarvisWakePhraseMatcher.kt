package com.jarvis.ai.voice

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

/**
 * Isolated, deterministic Wake Phrase Matcher for JARVIS.
 * Normalizes speech input and strictly matches approved wake phrases
 * without arbitrary false-positive triggering.
 */
object JarvisWakePhraseMatcher {

    const val DISCLOSURE_TEXT =
        "Wake detection is phrase-match after speech recognition, not a neural wake-word chip."

    data class MatchResult(
        val isMatched: Boolean,
        val matchedPhrase: String? = null,
        val commandAfterWake: String = "",
        val normalizedText: String = ""
    )

    private val ACCEPTED_ENGLISH_PHRASES = listOf(
        "hey jarvis",
        "ok jarvis",
        "okay jarvis",
        "jarvis",
        "hey jarvish",
        "ok jarvish",
        "jarvish",
        "hey jarwis",
        "ok jarwis",
        "jarwis"
    )

    private val ACCEPTED_HINDI_PHRASES = listOf(
        "हे जार्विस",
        "ओके जार्विस",
        "जार्विस"
    )

    /**
     * Normalizes raw text:
     * - Converts English characters to lowercase.
     * - Removes punctuation and special symbols while preserving letters, marks/matras (\p{M}), and digits.
     * - Collapses multiple spaces, tabs, and newlines into single spaces.
     * - Trims leading and trailing whitespace.
     */
    fun normalizeText(rawText: String?): String {
        if (rawText == null) return ""
        val lower = rawText.lowercase()
        // Replace punctuation marks and special symbols with space while preserving letters (\p{L}), combining marks/matras (\p{M}), and numbers (\p{Nd})
        val cleaned = lower.replace(Regex("""[^\p{L}\p{M}\p{Nd}\s]"""), " ")
        // Normalize multiple spaces to single space and trim
        return cleaned.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Matches normalized text against approved wake phrases.
     * Rejects unrelated speech and extracts any subsequent directive text.
     */
    fun match(rawText: String?): MatchResult {
        val normalized = normalizeText(rawText)
        if (normalized.isBlank()) {
            return MatchResult(isMatched = false, normalizedText = "")
        }

        // 1. Check English accepted wake phrases
        for (phrase in ACCEPTED_ENGLISH_PHRASES) {
            if (normalized == phrase) {
                return MatchResult(
                    isMatched = true,
                    matchedPhrase = phrase,
                    commandAfterWake = "",
                    normalizedText = normalized
                )
            }
            if (normalized.startsWith("$phrase ")) {
                val remainder = normalized.removePrefix("$phrase ").trim()
                return MatchResult(
                    isMatched = true,
                    matchedPhrase = phrase,
                    commandAfterWake = remainder,
                    normalizedText = normalized
                )
            }
        }

        // 2. Check Hindi / Devanagari accepted wake phrases
        for (phrase in ACCEPTED_HINDI_PHRASES) {
            val normalizedPhrase = normalizeText(phrase)
            if (normalized == normalizedPhrase) {
                return MatchResult(
                    isMatched = true,
                    matchedPhrase = phrase,
                    commandAfterWake = "",
                    normalizedText = normalized
                )
            }
            if (normalized.startsWith("$normalizedPhrase ")) {
                val remainder = normalized.removePrefix("$normalizedPhrase ").trim()
                return MatchResult(
                    isMatched = true,
                    matchedPhrase = phrase,
                    commandAfterWake = remainder,
                    normalizedText = normalized
                )
            }
        }

        // Unrelated speech -> IGNORED
        return MatchResult(
            isMatched = false,
            matchedPhrase = null,
            commandAfterWake = "",
            normalizedText = normalized
        )
    }
}
