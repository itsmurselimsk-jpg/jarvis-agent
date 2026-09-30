package com.example.jarvis.security

import java.util.regex.Pattern

/**
 * Privacy & Secret Redaction Guard (inspired by isair/jarvis utils/redact.py)
 * 
 * Deterministically strips API keys, OAuth tokens, JWTs, credentials, 
 * payment cards, and private identifiers before passing text to LLMs,
 * logs, or remote telemetry.
 */
object PrivacyRedactionGuard {

    private data class RedactionRule(
        val pattern: Pattern,
        val replacement: String,
        val name: String
    )

    private val rules = listOf(
        // Email addresses
        RedactionRule(
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", Pattern.CASE_INSENSITIVE),
            "[REDACTED_EMAIL]",
            "Email"
        ),
        // Payment Cards (13-19 digits with optional spaces/hyphens)
        RedactionRule(
            Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b"),
            "[REDACTED_CARD]",
            "Payment Card"
        ),
        // Cloud & AI API Keys (AWS, OpenAI, Google AI, Stripe, GitHub)
        RedactionRule(
            Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b"),
            "[REDACTED_AWS_KEY]",
            "AWS Key"
        ),
        RedactionRule(
            Pattern.compile("\\b(?:sk|pk|rk)_(?:live|test)_[A-Za-z0-9]{16,}\\b"),
            "[REDACTED_STRIPE_KEY]",
            "Stripe Key"
        ),
        RedactionRule(
            Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{36,}\\b"),
            "[REDACTED_GH_TOKEN]",
            "GitHub Token"
        ),
        RedactionRule(
            Pattern.compile("\\bsk-[A-Za-z0-9_-]{32,}\\b"),
            "[REDACTED_OPENAI_KEY]",
            "OpenAI Key"
        ),
        RedactionRule(
            Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"),
            "[REDACTED_GOOG_KEY]",
            "Google API Key"
        ),
        // Authorization Headers
        RedactionRule(
            Pattern.compile("Authorization:\\s*Bearer\\s+\\S+", Pattern.CASE_INSENSITIVE),
            "Authorization: Bearer [REDACTED]",
            "Bearer Auth"
        ),
        RedactionRule(
            Pattern.compile("Authorization:\\s*Basic\\s+[A-Za-z0-9+/=]+", Pattern.CASE_INSENSITIVE),
            "Authorization: Basic [REDACTED]",
            "Basic Auth"
        ),
        // JSON Web Tokens (JWT)
        RedactionRule(
            Pattern.compile("\\beyJ[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\b"),
            "[REDACTED_JWT]",
            "JWT"
        ),
        // Keyword-anchored credentials (password, secret, apikey)
        RedactionRule(
            Pattern.compile(
                "\\b(pass(?:word)?|secret|token|apikey|api_key|(?:refresh|access|id|oauth)_?token|session(?:_?id)?|sid)\\s*[:=]\\s*\\S+\\b",
                Pattern.CASE_INSENSITIVE
            ),
            "$1=[REDACTED]",
            "Credential Parameter"
        ),
        // OTPs & 2FA codes
        RedactionRule(
            Pattern.compile("\\b\\d{4,8}\\b(?=.*(?:otp|2fa|code|verification|pin))", Pattern.CASE_INSENSITIVE),
            "[REDACTED_OTP]",
            "OTP/2FA"
        )
    )

    private var totalRedactionsCount = 0

    val totalRedactions: Int
        get() = totalRedactionsCount

    /**
     * Sanitizes input text, replacing sensitive tokens with safe redaction placeholders.
     */
    fun redact(text: String, maxLen: Int = 8000): String {
        if (text.isBlank()) return text
        var scrubbed = text
        var matchFound = false

        for (rule in rules) {
            val matcher = rule.pattern.matcher(scrubbed)
            if (matcher.find()) {
                matchFound = true
                scrubbed = matcher.replaceAll(rule.replacement)
            }
        }

        if (matchFound) {
            totalRedactionsCount++
        }

        return if (scrubbed.length > maxLen) {
            scrubbed.take(maxLen) + "... [TRUNCATED]"
        } else {
            scrubbed
        }
    }

    /**
     * Returns true if the given text contains any credential or sensitive data token.
     */
    fun containsSensitiveData(text: String): Boolean {
        if (text.isBlank()) return false
        return rules.any { it.pattern.matcher(text).find() }
    }
}
