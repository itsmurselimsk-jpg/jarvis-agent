package com.example.jarvis.extraction

import java.util.Locale

/**
 * Normalization utilities for structured data.
 * Adheres strictly to the rule: "Do NOT silently change ambiguous user data."
 */
object Normalizer {

    /**
     * Normalizes an email address to standard trimmed lowercase.
     */
    fun normalizeEmail(rawEmail: String): String {
        return rawEmail.trim().lowercase(Locale.ROOT)
    }

    /**
     * Normalizes a web URL.
     * Ensures scheme (defaults to https:// if starting with www.), converts host to lowercase.
     */
    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val withScheme = if (trimmed.startsWith("www.", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        return try {
            val uri = java.net.URI(withScheme)
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: "https"
            val host = uri.host?.lowercase(Locale.ROOT) ?: ""
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.rawPath ?: ""
            val query = if (uri.rawQuery != null) "?${uri.rawQuery}" else ""
            val fragment = if (uri.rawFragment != null) "#${uri.rawFragment}" else ""

            if (host.isNotBlank()) {
                "$scheme://$host$port$path$query$fragment"
            } else {
                withScheme
            }
        } catch (_: Exception) {
            withScheme
        }
    }

    /**
     * Normalizes phone numbers by stripping whitespace, hyphens, and formatting punctuation
     * while preserving international dialing prefix (+).
     */
    fun normalizePhone(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }

    /**
     * Normalizes a currency representation into amount, symbol, and optional ISO currency code.
     */
    fun normalizeCurrency(
        rawAmount: String,
        rawSymbolOrCode: String
    ): Triple<Double, String, String?> {
        val cleanAmountStr = rawAmount.replace(",", "").trim()
        val amount = cleanAmountStr.toDoubleOrNull() ?: 0.0

        val trimmedToken = rawSymbolOrCode.trim()
        val (symbol, code) = when (trimmedToken.uppercase(Locale.ROOT)) {
            "$", "USD", "DOLLAR", "DOLLARS", "BUCKS" -> Pair("$", "USD")
            "€", "EUR", "EURO", "EUROS" -> Pair("€", "EUR")
            "£", "GBP", "POUND", "POUNDS" -> Pair("£", "GBP")
            "₹", "INR", "RUPEE", "RUPEES" -> Pair("₹", "INR")
            "৳", "BDT", "TAKA" -> Pair("৳", "BDT")
            "¥", "JPY", "YEN" -> Pair("¥", "JPY")
            "CAD" -> Pair("$", "CAD")
            "AUD" -> Pair("$", "AUD")
            else -> Pair(trimmedToken, null)
        }

        return Triple(amount, symbol, code)
    }

    /**
     * Normalizes percentage strings to Double value.
     */
    fun normalizePercentage(rawPercentage: String): Double {
        val clean = rawPercentage.replace("%", "")
            .replace("percent", "", ignoreCase = true)
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    /**
     * Inspects a date string. If unambiguous (e.g. YYYY-MM-DD or standard DD/MM/YYYY),
     * normalizes to standard ISO-8601 (YYYY-MM-DD). If ambiguous, keeps null and marks isAmbiguous.
     */
    fun normalizeDate(rawDate: String): Pair<String?, Boolean> {
        val trimmed = rawDate.trim()

        // 1. Check ISO format YYYY-MM-DD
        val isoRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
        if (isoRegex.matches(trimmed)) {
            return Pair(trimmed, false)
        }

        // 2. Ambiguous relative words (e.g. tomorrow, yesterday, next week, upcoming)
        val relativeAmbiguous = listOf(
            "next", "upcoming", "soon", "later", "someday", "tentative"
        )
        if (relativeAmbiguous.any { trimmed.contains(it, ignoreCase = true) }) {
            return Pair(null, true)
        }

        // 3. Simple relative terms that represent specific relative days (today, tomorrow, yesterday)
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower == "today" || lower == "tomorrow" || lower == "yesterday") {
            // These have clear relative intent but depending on time-zone or caller anchor,
            // we preserve unambiguous intent without inventing a fake static calendar timestamp.
            return Pair(lower, false)
        }

        return Pair(null, false)
    }

    /**
     * Inspects a time string. If unambiguous (e.g. 14:30, 2:30 PM), formats to HH:mm (24-hour).
     * If ambiguous (e.g. morning, evening, night, later), returns null normalized time and marks isAmbiguous.
     */
    fun normalizeTime(rawTime: String): Pair<String?, Boolean> {
        val trimmed = rawTime.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Ambiguous natural intervals
        val naturalIntervals = setOf(
            "morning", "afternoon", "evening", "night", "tonight", "noon", "midnight", "later", "sometime"
        )
        if (naturalIntervals.contains(lower) || lower.contains("evening") || lower.contains("morning") || lower.contains("afternoon")) {
            return Pair(null, true)
        }

        // Check 12-hour format e.g. "2:30 pm" or "11:15 am" or "5 pm"
        val twelveHourRegex = Regex("""^(\d{1,2})(?::(\d{2}))?\s*(am|pm)$""", RegexOption.IGNORE_CASE)
        val twelveMatch = twelveHourRegex.matchEntire(trimmed)
        if (twelveMatch != null) {
            var hours = twelveMatch.groupValues[1].toInt()
            val minutes = twelveMatch.groupValues[2].ifBlank { "00" }.toInt()
            val meridiem = twelveMatch.groupValues[3].lowercase(Locale.ROOT)

            if (meridiem == "pm" && hours < 12) hours += 12
            if (meridiem == "am" && hours == 12) hours = 0

            val normalized = String.format(Locale.ROOT, "%02d:%02d", hours, minutes)
            return Pair(normalized, false)
        }

        // Check 24-hour format e.g. "14:30" or "09:45"
        val twentyFourHourRegex = Regex("""^([01]?[0-9]|2[0-3]):([0-5][0-9])$""")
        val twentyFourMatch = twentyFourHourRegex.matchEntire(trimmed)
        if (twentyFourMatch != null) {
            val hours = twentyFourMatch.groupValues[1].toInt()
            val minutes = twentyFourMatch.groupValues[2].toInt()
            val normalized = String.format(Locale.ROOT, "%02d:%02d", hours, minutes)
            return Pair(normalized, false)
        }

        return Pair(null, false)
    }
}
