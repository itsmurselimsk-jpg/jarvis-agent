package com.example.jarvis.extraction

import com.example.jarvis.notification.JarvisNotification
import com.example.jarvis.provider.AIProvider
import com.example.jarvis.recovery.ResponseRepair
import com.example.jarvis.search.UniversalSearchResult
import com.example.jarvis.security.SensitiveDataFilter
import com.example.jarvis.vision.VisionResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Robust Information Extraction Engine for the JARVIS operating system.
 * Extracts entities, key/value fields, dates, times, currencies, percentages,
 * locations, contacts, URLs, identifiers, and facts from multiple sources.
 *
 * Guarantees:
 * - Deterministic extraction runs locally without external dependencies.
 * - AI-assisted extraction strictly validates structured output.
 * - Sensitive credentials (OTP, passwords, card numbers) are detected and protected.
 * - Ambiguous values are flagged and never fabricated.
 * - Does NOT persist to memory or execute tools.
 */
object InformationExtractionEngine {

    // Regex patterns for deterministic extraction
    private val EMAIL_REGEX = Regex(
        """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"""
    )

    private val PHONE_REGEX = Regex(
        """(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b|\b(?:\+880|\+91)\d{10}\b|\b\d{10,11}\b"""
    )

    private val URL_REGEX = Regex(
        """\bhttps?://[^\s<>"]+|www\.[^\s<>"]+\b""",
        RegexOption.IGNORE_CASE
    )

    private val CURRENCY_SYMBOL_REGEX = Regex(
        """([$€£¥₹৳])\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)\b"""
    )

    private val CURRENCY_CODE_REGEX = Regex(
        """\b(\d+(?:,\d{3})*(?:\.\d{1,2})?)\s*(USD|EUR|GBP|INR|BDT|CAD|AUD|JPY|dollars?|taka|rupees?|bucks?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val PERCENTAGE_REGEX = Regex(
        """\b(\d+(?:\.\d+)?)\s*%|\b(\d+(?:\.\d+)?)\s*percent\b""",
        RegexOption.IGNORE_CASE
    )

    private val ISO_DATE_REGEX = Regex(
        """\b(\d{4})-(\d{2})-(\d{2})\b"""
    )

    private val FORMATTED_DATE_REGEX = Regex(
        """\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""
    )

    private val NAMED_MONTH_DATE_REGEX = Regex(
        """\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?,?\s*(?:\d{4})?\b""",
        RegexOption.IGNORE_CASE
    )

    private val RELATIVE_DATE_REGEX = Regex(
        """\b(today|tomorrow|yesterday)\b|\bnext\s+(week|month|year|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""",
        RegexOption.IGNORE_CASE
    )

    private val TWELVE_HOUR_TIME_REGEX = Regex(
        """\b(?:0?[1-9]|1[0-2])(?::[0-5][0-9])?\s*(?:am|pm)\b""",
        RegexOption.IGNORE_CASE
    )

    private val TWENTY_FOUR_HOUR_TIME_REGEX = Regex(
        """\b(?:[01]?[0-9]|2[0-3]):[0-5][0-9]\b"""
    )

    private val NATURAL_TIME_INTERVAL_REGEX = Regex(
        """\b(morning|afternoon|evening|night|tonight|noon|midnight)\b""",
        RegexOption.IGNORE_CASE
    )

    private val IDENTIFIER_PREFIX_REGEX = Regex(
        """\b(ORDER|ORD|REF|CASE|INV|TICKET|TRK|ID|CODE)[#:\-\s]+([A-Za-z0-9\-_]{3,20})\b""",
        RegexOption.IGNORE_CASE
    )

    private val UUID_REGEX = Regex(
        """\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""
    )

    private val KEY_VALUE_REGEX = Regex(
        """\b([A-Za-z0-9_]{2,25})\s*[:=]\s*([^\n,;]{1,60})"""
    )

    private val INTENT_COMMAND_REGEX = Regex(
        """\b(call|dial|open|launch|search|play|set timer|schedule|check|remind)\s+([A-Za-z0-9\s+._-]{2,40})""",
        RegexOption.IGNORE_CASE
    )

    // Common non-numeric stop words for field keys
    private val EXCLUDED_FIELD_KEYS = setOf(
        "http", "https", "note", "example", "warning", "error", "at", "on"
    )

    /**
     * Primary entry point for deterministic extraction from plain text.
     */
    fun extract(
        text: String,
        source: ExtractionSource = ExtractionSource.GENERIC_TEXT
    ): ExtractedData {
        if (text.isBlank()) {
            return ExtractedData.empty(source, text)
        }

        val entities = mutableListOf<ExtractedEntity>()
        val fields = mutableListOf<ExtractedField>()
        val dates = mutableListOf<ExtractedDate>()
        val times = mutableListOf<ExtractedTime>()
        val locations = mutableListOf<ExtractedLocation>()
        val people = mutableListOf<ExtractedPerson>()
        val organizations = mutableListOf<ExtractedOrganization>()
        val phoneNumbers = mutableListOf<ExtractedPhone>()
        val emails = mutableListOf<ExtractedEmail>()
        val urls = mutableListOf<ExtractedUrl>()
        val numbers = mutableListOf<ExtractedNumber>()
        val currencies = mutableListOf<ExtractedCurrency>()
        val percentages = mutableListOf<ExtractedPercentage>()
        val identifiers = mutableListOf<ExtractedIdentifier>()
        val intents = mutableListOf<ExtractedIntent>()
        val facts = mutableListOf<ExtractedFact>()

        var hasAmbiguity = false

        // 1. Sensitive Data Check
        val containsSensitiveData = SensitiveDataFilter.containsSensitiveData(text)
        val sensitiveDataTypes = mutableListOf<String>()
        if (containsSensitiveData) {
            if (text.contains(Regex("""(?i)\b(?:otp|code|verification)\b"""))) sensitiveDataTypes.add("OTP / Verification Code")
            if (text.contains(Regex("""(?i)\b(?:password|passwd|pin)\b"""))) sensitiveDataTypes.add("Password / PIN")
            if (text.contains(Regex("""\b(?:\d{4}[ -]?){3}\d{4}\b"""))) sensitiveDataTypes.add("Card Number")
            if (text.contains(Regex("""\b(?:AIza|sk-|Bearer)\b"""))) sensitiveDataTypes.add("API Key / Token")
        }

        // 2. Emails
        EMAIL_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val normalized = Normalizer.normalizeEmail(raw)
            val domain = normalized.substringAfter("@", "")
            val emailObj = ExtractedEmail(email = normalized, rawText = raw, domain = domain)
            emails.add(emailObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.EMAIL,
                    value = normalized,
                    rawText = raw,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 3. URLs
        URL_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val normalized = Normalizer.normalizeUrl(raw)
            val domain = try { java.net.URI(normalized).host } catch (_: Exception) { null }
            val urlObj = ExtractedUrl(url = normalized, rawText = raw, domain = domain)
            urls.add(urlObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.URL,
                    value = normalized,
                    rawText = raw,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 4. Phone numbers
        PHONE_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val normalized = Normalizer.normalizePhone(raw)
            if (normalized.length >= 7) {
                val countryCode = if (normalized.startsWith("+")) {
                    normalized.take(3)
                } else null
                val phoneObj = ExtractedPhone(phoneNumber = normalized, rawText = raw, countryCode = countryCode)
                phoneNumbers.add(phoneObj)
                entities.add(
                    ExtractedEntity(
                        type = EntityType.PHONE,
                        value = normalized,
                        rawText = raw,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1
                    )
                )
            }
        }

        // 5. Currencies
        CURRENCY_SYMBOL_REGEX.findAll(text).forEach { match ->
            val symbol = match.groupValues[1]
            val amountStr = match.groupValues[2]
            val (amount, normSymbol, normCode) = Normalizer.normalizeCurrency(amountStr, symbol)
            val currObj = ExtractedCurrency(
                amount = amount,
                currencySymbol = normSymbol,
                currencyCode = normCode,
                rawText = match.value
            )
            currencies.add(currObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.CURRENCY,
                    value = "$normSymbol$amount",
                    rawText = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        CURRENCY_CODE_REGEX.findAll(text).forEach { match ->
            val amountStr = match.groupValues[1]
            val codeOrWord = match.groupValues[2]
            val (amount, normSymbol, normCode) = Normalizer.normalizeCurrency(amountStr, codeOrWord)
            val currObj = ExtractedCurrency(
                amount = amount,
                currencySymbol = normSymbol,
                currencyCode = normCode,
                rawText = match.value
            )
            currencies.add(currObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.CURRENCY,
                    value = "$amount ${normCode ?: normSymbol}",
                    rawText = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 6. Percentages
        PERCENTAGE_REGEX.findAll(text).forEach { match ->
            val numStr = match.groupValues[1].ifBlank { match.groupValues[2] }
            val normValue = Normalizer.normalizePercentage(numStr)
            val pctObj = ExtractedPercentage(value = normValue, rawText = match.value)
            percentages.add(pctObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.PERCENTAGE,
                    value = "$normValue%",
                    rawText = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 7. Dates (ISO, formatted, named month, relative)
        ISO_DATE_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeDate(raw)
            if (isAmbiguous) hasAmbiguity = true
            val dateObj = ExtractedDate(rawText = raw, normalizedIso = normalized, isAmbiguous = isAmbiguous)
            dates.add(dateObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.DATE,
                    value = normalized ?: raw,
                    rawText = raw,
                    isAmbiguous = isAmbiguous,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        FORMATTED_DATE_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeDate(raw)
            if (isAmbiguous) hasAmbiguity = true
            val dateObj = ExtractedDate(rawText = raw, normalizedIso = normalized, isAmbiguous = isAmbiguous)
            dates.add(dateObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.DATE,
                    value = normalized ?: raw,
                    rawText = raw,
                    isAmbiguous = isAmbiguous,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        NAMED_MONTH_DATE_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeDate(raw)
            if (isAmbiguous) hasAmbiguity = true
            val dateObj = ExtractedDate(rawText = raw, normalizedIso = normalized, isAmbiguous = isAmbiguous)
            dates.add(dateObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.DATE,
                    value = normalized ?: raw,
                    rawText = raw,
                    isAmbiguous = isAmbiguous,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        RELATIVE_DATE_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeDate(raw)
            if (isAmbiguous) hasAmbiguity = true
            val dateObj = ExtractedDate(rawText = raw, normalizedIso = normalized, isAmbiguous = isAmbiguous)
            dates.add(dateObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.DATE,
                    value = normalized ?: raw,
                    rawText = raw,
                    isAmbiguous = isAmbiguous,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 8. Times (12h, 24h, natural intervals)
        TWELVE_HOUR_TIME_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeTime(raw)
            if (isAmbiguous) hasAmbiguity = true
            val timeObj = ExtractedTime(rawText = raw, normalizedTime = normalized, isAmbiguous = isAmbiguous)
            times.add(timeObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.TIME,
                    value = normalized ?: raw,
                    rawText = raw,
                    isAmbiguous = isAmbiguous,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        TWENTY_FOUR_HOUR_TIME_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeTime(raw)
            if (isAmbiguous) hasAmbiguity = true
            val timeObj = ExtractedTime(rawText = raw, normalizedTime = normalized, isAmbiguous = isAmbiguous)
            times.add(timeObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.TIME,
                    value = normalized ?: raw,
                    rawText = raw,
                    isAmbiguous = isAmbiguous,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        NATURAL_TIME_INTERVAL_REGEX.findAll(text).forEach { match ->
            val raw = match.value
            val (normalized, isAmbiguous) = Normalizer.normalizeTime(raw)
            hasAmbiguity = true
            val timeObj = ExtractedTime(rawText = raw, normalizedTime = normalized, isAmbiguous = true)
            times.add(timeObj)
            entities.add(
                ExtractedEntity(
                    type = EntityType.TIME,
                    value = raw,
                    rawText = raw,
                    isAmbiguous = true,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 9. Identifiers (UUID, prefixed codes)
        UUID_REGEX.findAll(text).forEach { match ->
            val id = match.value
            identifiers.add(ExtractedIdentifier(id = id, type = "UUID", rawText = id))
            entities.add(
                ExtractedEntity(
                    type = EntityType.IDENTIFIER,
                    value = id,
                    rawText = id,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        IDENTIFIER_PREFIX_REGEX.findAll(text).forEach { match ->
            val prefix = match.groupValues[1].uppercase(Locale.ROOT)
            val code = match.groupValues[2]
            val fullId = "$prefix-$code"
            identifiers.add(ExtractedIdentifier(id = fullId, type = prefix, rawText = match.value))
            entities.add(
                ExtractedEntity(
                    type = EntityType.IDENTIFIER,
                    value = fullId,
                    rawText = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }

        // 10. Key-Value Fields
        KEY_VALUE_REGEX.findAll(text).forEach { match ->
            val rawKey = match.groupValues[1].trim()
            val rawVal = match.groupValues[2].trim()

            if (!EXCLUDED_FIELD_KEYS.contains(rawKey.lowercase(Locale.ROOT)) && rawVal.isNotBlank()) {
                val field = ExtractedField(key = rawKey, value = rawVal, rawText = match.value)
                fields.add(field)
                entities.add(
                    ExtractedEntity(
                        type = EntityType.KEY_VALUE,
                        value = "$rawKey=$rawVal",
                        rawText = match.value,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1
                    )
                )
            }
        }

        // 11. Explicit Command Intent
        INTENT_COMMAND_REGEX.findAll(text).forEach { match ->
            val action = match.groupValues[1].lowercase(Locale.ROOT)
            val target = match.groupValues[2].trim()
            intents.add(
                ExtractedIntent(
                    intent = action,
                    action = action,
                    target = target,
                    confidence = 0.9f
                )
            )
        }

        // 12. Numbers (standalone numbers not covered by phone, date, currency)
        val standaloneNumRegex = Regex("""\b(\d+(?:\.\d+)?)\b""")
        standaloneNumRegex.findAll(text).forEach { match ->
            val numStr = match.value
            val dbl = numStr.toDoubleOrNull()
            if (dbl != null) {
                // Avoid redundant inclusion if already matched in phone, date, or currency
                val isAlreadyCovered = currencies.any { it.rawText.contains(numStr) } ||
                        percentages.any { it.rawText.contains(numStr) } ||
                        phoneNumbers.any { it.phoneNumber.contains(numStr) } ||
                        dates.any { it.rawText.contains(numStr) } ||
                        times.any { it.rawText.contains(numStr) }

                if (!isAlreadyCovered) {
                    val isInt = !numStr.contains(".")
                    numbers.add(ExtractedNumber(value = dbl, rawText = numStr, isInteger = isInt))
                }
            }
        }

        return ExtractedData(
            source = source,
            originalText = text,
            entities = entities,
            fields = fields,
            dates = dates,
            times = times,
            locations = locations,
            people = people,
            organizations = organizations,
            phoneNumbers = phoneNumbers,
            emails = emails,
            urls = urls,
            numbers = numbers,
            currencies = currencies,
            percentages = percentages,
            identifiers = identifiers,
            intents = intents,
            facts = facts,
            containsSensitiveData = containsSensitiveData,
            sensitiveDataTypes = sensitiveDataTypes,
            hasAmbiguity = hasAmbiguity
        )
    }

    /**
     * AI-Assisted Semantic Extraction.
     * Combines deterministic local extraction with structured LLM semantic parsing.
     * Uses ResponseRepair to ensure only verified JSON is processed.
     */
    suspend fun extractWithAi(
        text: String,
        source: ExtractionSource,
        aiProvider: AIProvider?
    ): ExtractedData {
        val base = extract(text, source)
        if (text.isBlank() || aiProvider == null) {
            return base
        }

        return try {
            val schemaPrompt = """
                Extract structured entities and facts from the text below.
                Respond with ONLY a raw valid JSON object matching this schema:
                {
                  "people": ["string"],
                  "organizations": ["string"],
                  "locations": ["string"],
                  "facts": ["string"],
                  "fields": [{"key": "string", "value": "string"}],
                  "intent": {"action": "string", "target": "string"}
                }
                Do not invent values. If not present, use empty lists/null.
                
                Text: "$text"
            """.trimIndent()

            val rawResponse = aiProvider.generateResponse(
                prompt = schemaPrompt,
                systemInstruction = "You are a precision information extraction engine. Return only JSON without conversational preamble or markdown backticks.",
                onChunkReceived = {}
            )

            parseAndMergeAiOutput(base, rawResponse)
        } catch (_: Exception) {
            // Gracefully degrade to deterministic local extraction
            base
        }
    }

    /**
     * Validates and parses the structured AI response, then merges it with base deterministic extractions.
     */
    fun parseAndMergeAiOutput(base: ExtractedData, rawAiResponse: String): ExtractedData {
        val root = ResponseRepair.extractCleanJson(rawAiResponse) ?: return base

        return try {
            val people = base.people.toMutableList()
            val organizations = base.organizations.toMutableList()
            val locations = base.locations.toMutableList()
            val facts = base.facts.toMutableList()
            val fields = base.fields.toMutableList()
            val intents = base.intents.toMutableList()
            val entities = base.entities.toMutableList()

            // People
            root.optJSONArray("people")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val name = arr.optString(i).trim()
                    if (name.isNotBlank() && people.none { it.name.equals(name, ignoreCase = true) }) {
                        people.add(ExtractedPerson(name = name))
                        entities.add(ExtractedEntity(type = EntityType.PERSON, value = name, rawText = name))
                    }
                }
            }

            // Organizations
            root.optJSONArray("organizations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val org = arr.optString(i).trim()
                    if (org.isNotBlank() && organizations.none { it.name.equals(org, ignoreCase = true) }) {
                        organizations.add(ExtractedOrganization(name = org))
                        entities.add(ExtractedEntity(type = EntityType.ORGANIZATION, value = org, rawText = org))
                    }
                }
            }

            // Locations
            root.optJSONArray("locations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val loc = arr.optString(i).trim()
                    if (loc.isNotBlank() && locations.none { it.name.equals(loc, ignoreCase = true) }) {
                        locations.add(ExtractedLocation(name = loc))
                        entities.add(ExtractedEntity(type = EntityType.LOCATION, value = loc, rawText = loc))
                    }
                }
            }

            // Facts
            root.optJSONArray("facts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val fact = arr.optString(i).trim()
                    if (fact.isNotBlank() && facts.none { it.fact.equals(fact, ignoreCase = true) }) {
                        facts.add(ExtractedFact(fact = fact))
                        entities.add(ExtractedEntity(type = EntityType.FACT, value = fact, rawText = fact))
                    }
                }
            }

            // Fields
            root.optJSONArray("fields")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        val k = obj.optString("key").trim()
                        val v = obj.optString("value").trim()
                        if (k.isNotBlank() && v.isNotBlank() && fields.none { it.key.equals(k, ignoreCase = true) }) {
                            fields.add(ExtractedField(key = k, value = v))
                            entities.add(ExtractedEntity(type = EntityType.KEY_VALUE, value = "$k=$v", rawText = "$k: $v"))
                        }
                    }
                }
            }

            // Intent
            root.optJSONObject("intent")?.let { obj ->
                val action = obj.optString("action").trim()
                val target = obj.optString("target").trim()
                if (action.isNotBlank() && intents.none { it.action.equals(action, ignoreCase = true) }) {
                    intents.add(ExtractedIntent(intent = action, action = action, target = target))
                }
            }

            base.copy(
                people = people,
                organizations = organizations,
                locations = locations,
                facts = facts,
                fields = fields,
                intents = intents,
                entities = entities
            )
        } catch (_: Exception) {
            base
        }
    }

    // Source-specific entry helpers
    fun extractFromUserMessage(text: String): ExtractedData {
        return extract(text, ExtractionSource.USER_MESSAGE)
    }

    fun extractFromAiResponse(text: String): ExtractedData {
        return extract(text, ExtractionSource.AI_RESPONSE)
    }

    fun extractFromOcr(ocrResult: VisionResult): ExtractedData {
        return extract(ocrResult.extractedText, ExtractionSource.OCR)
    }

    fun extractFromNotification(notification: JarvisNotification): ExtractedData {
        val composite = buildString {
            appendLine(notification.title)
            if (notification.text.isNotBlank()) appendLine(notification.text)
        }
        return extract(composite.trim(), ExtractionSource.NOTIFICATION)
    }

    fun extractFromSearch(result: UniversalSearchResult): ExtractedData {
        val composite = "${result.title}. ${result.subtitle ?: ""}"
        return extract(composite.trim(), ExtractionSource.SEARCH)
    }
}
