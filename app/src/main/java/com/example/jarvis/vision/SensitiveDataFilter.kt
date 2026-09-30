package com.example.jarvis.vision

import java.util.Locale

/**
 * Filter and privacy inspector for optical characters.
 * Ensures OTPs, passwords, card numbers, and credentials are never persisted into long-term memory.
 */
object SensitiveDataFilter {

    private val OTP_REGEX = Regex(
        """(?i)\b(?:otp|one[\s-]*time[\s-]*password|code|verification[\s-]*code|auth[\s-]*code|পিন|ओটিপি|কোড)\s*(?:is|[:=-])?\s*([0-9]{4,8})\b"""
    )
    private val STANDALONE_OTP_PROXIMITY = Regex(
        """(?i)\b(?:valid\s+for|expires\s+in|do\s+not\s+share|verification|confirm|login\s+code)\b"""
    )

    private val CARD_REGEX = Regex(
        """\b(?:\d{4}[ -]?){3}\d{4}\b|\b(?:\d{4}[ -]\d{6}[ -]\d{5})\b"""
    )

    private val CVV_REGEX = Regex(
        """(?i)\b(?:cvv|cvc|security\s*code)\s*[:=]?\s*([0-9]{3,4})\b"""
    )

    private val PASSWORD_REGEX = Regex(
        """(?i)\b(?:password|passwd|pwd|passphrase|পাসওয়ার্ড|पासवर्ड)\s*(?:is|[:=])\s*(\S+)"""
    )

    private val API_KEY_REGEX = Regex(
        """\b(?:AIza[0-9A-Za-z-_]{35}|sk-[0-9A-Za-z]{20,}|ghp_[0-9A-Za-z]{36})\b"""
    )

    private val BANK_ACCOUNT_REGEX = Regex(
        """(?i)\b(?:account\s*(?:no|num|number)|a/c\s*no|iban|routing\s*no)\s*[:=]?\s*([0-9A-Z]{8,24})\b"""
    )

    // Action extraction patterns
    private val PHONE_REGEX = Regex(
        """(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b|\b(?:\+880|\+91)\d{10}\b"""
    )

    private val URL_REGEX = Regex(
        """https?://[^\s<>"]+|www\.[^\s<>"]+"""
    )

    private val EMAIL_REGEX = Regex(
        """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""
    )

    /**
     * Inspects text and returns a list of detected sensitive entity descriptions.
     */
    fun detectSensitiveEntities(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val detected = mutableListOf<String>()

        if (OTP_REGEX.containsMatchIn(text) || (STANDALONE_OTP_PROXIMITY.containsMatchIn(text) && Regex("""\b\d{4,6}\b""").containsMatchIn(text))) {
            detected.add("One-Time Password (OTP) / Verification Code")
        }

        if (CARD_REGEX.containsMatchIn(text)) {
            detected.add("Payment Card Number (Debit/Credit)")
        }

        if (CVV_REGEX.containsMatchIn(text)) {
            detected.add("Card Security Code (CVV/CVC)")
        }

        if (PASSWORD_REGEX.containsMatchIn(text)) {
            detected.add("Account Password / Passphrase")
        }

        if (API_KEY_REGEX.containsMatchIn(text)) {
            detected.add("API Secret / Authentication Token")
        }

        if (BANK_ACCOUNT_REGEX.containsMatchIn(text)) {
            detected.add("Bank Account / Routing Identifier")
        }

        return detected
    }

    /**
     * Returns true if any sensitive entities are detected in the text.
     */
    fun containsSensitiveData(text: String): Boolean {
        return detectSensitiveEntities(text).isNotEmpty()
    }

    /**
     * Replaces detected sensitive sequences with redaction masks.
     */
    fun redactSensitiveData(text: String): String {
        if (text.isBlank()) return text
        var redacted = text

        redacted = OTP_REGEX.replace(redacted) { mr ->
            val full = mr.value
            val code = mr.groupValues[1]
            full.replace(code, "[OTP REDACTED]")
        }

        redacted = CARD_REGEX.replace(redacted) { mr ->
            val digits = mr.value.replace(Regex("""\s|-"""), "")
            if (digits.length >= 12) {
                val prefix = digits.take(4)
                val suffix = digits.takeLast(4)
                "$prefix •••• •••• $suffix"
            } else {
                "[CARD REDACTED]"
            }
        }

        redacted = CVV_REGEX.replace(redacted) { mr ->
            val full = mr.value
            val code = mr.groupValues[1]
            full.replace(code, "[CVV •••]")
        }

        redacted = PASSWORD_REGEX.replace(redacted) { mr ->
            val full = mr.value
            val pwd = mr.groupValues[1]
            full.replace(pwd, "[SECRET REDACTED]")
        }

        redacted = API_KEY_REGEX.replace(redacted) { "[API_KEY_REDACTED]" }

        return redacted
    }

    /**
     * Extracts actionable items from the recognized text (Phone numbers, URLs, Emails).
     */
    fun extractActions(text: String): List<VisionDerivedAction> {
        if (text.isBlank()) return emptyList()
        val actions = mutableListOf<VisionDerivedAction>()

        // Phone numbers
        PHONE_REGEX.findAll(text).forEach { match ->
            val number = match.value.trim()
            if (number.length >= 7) {
                actions.add(
                    VisionDerivedAction(
                        type = VisionActionType.DIAL_PHONE,
                        label = "Dial $number",
                        payload = number,
                        isSensitiveOrDestructive = true // Requires confirmation through RiskEngine!
                    )
                )
                actions.add(
                    VisionDerivedAction(
                        type = VisionActionType.SEARCH_PHONE,
                        label = "Search Contacts for $number",
                        payload = number,
                        isSensitiveOrDestructive = false
                    )
                )
            }
        }

        // URLs
        URL_REGEX.findAll(text).forEach { match ->
            val url = match.value.trim()
            actions.add(
                VisionDerivedAction(
                    type = VisionActionType.OPEN_URL,
                    label = "Open $url",
                    payload = url,
                    isSensitiveOrDestructive = true // Requires confirmation through RiskEngine!
                )
            )
        }

        // Emails
        EMAIL_REGEX.findAll(text).forEach { match ->
            val email = match.value.trim()
            actions.add(
                VisionDerivedAction(
                    type = VisionActionType.SEND_EMAIL,
                    label = "Email $email",
                    payload = email,
                    isSensitiveOrDestructive = true
                )
            )
        }

        // OTP / Security verification codes
        OTP_REGEX.findAll(text).forEach { match ->
            val code = match.groupValues[1].trim()
            if (code.isNotBlank()) {
                actions.add(
                    VisionDerivedAction(
                        type = VisionActionType.COPY_TEXT,
                        label = "Copy OTP Code ($code)",
                        payload = code,
                        isSensitiveOrDestructive = true
                    )
                )
            }
        }

        return actions.distinctBy { it.label }
    }
}
