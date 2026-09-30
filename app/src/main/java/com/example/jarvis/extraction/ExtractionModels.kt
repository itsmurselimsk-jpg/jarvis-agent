package com.example.jarvis.extraction

/**
 * Origin of the text subjected to information extraction.
 */
enum class ExtractionSource {
    USER_MESSAGE,
    AI_RESPONSE,
    OCR,
    NOTIFICATION,
    SEARCH,
    DOCUMENT,
    GENERIC_TEXT
}

/**
 * Standard classification of extracted semantic and syntactic entities.
 */
enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    PHONE,
    EMAIL,
    URL,
    DATE,
    TIME,
    CURRENCY,
    PERCENTAGE,
    NUMBER,
    IDENTIFIER,
    KEY_VALUE,
    INTENT,
    FACT,
    CUSTOM
}

/**
 * General entity extraction container.
 */
data class ExtractedEntity(
    val type: EntityType,
    val value: String,
    val rawText: String,
    val confidence: Float = 1.0f,
    val isAmbiguous: Boolean = false,
    val startIndex: Int = -1,
    val endIndex: Int = -1,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Key-value pair field.
 */
data class ExtractedField(
    val key: String,
    val value: String,
    val rawText: String? = null,
    val confidence: Float = 1.0f
)

/**
 * Extracted date representation with unambiguous vs ambiguous tracking.
 */
data class ExtractedDate(
    val rawText: String,
    val normalizedIso: String? = null,
    val isAmbiguous: Boolean = false,
    val confidence: Float = 1.0f
)

/**
 * Extracted time representation with unambiguous vs ambiguous tracking.
 */
data class ExtractedTime(
    val rawText: String,
    val normalizedTime: String? = null,
    val isAmbiguous: Boolean = false,
    val confidence: Float = 1.0f
)

data class ExtractedLocation(
    val name: String,
    val rawText: String = name,
    val confidence: Float = 1.0f
)

data class ExtractedPerson(
    val name: String,
    val rawText: String = name,
    val confidence: Float = 1.0f
)

data class ExtractedOrganization(
    val name: String,
    val rawText: String = name,
    val confidence: Float = 1.0f
)

data class ExtractedPhone(
    val phoneNumber: String,
    val rawText: String = phoneNumber,
    val countryCode: String? = null,
    val confidence: Float = 1.0f
)

data class ExtractedEmail(
    val email: String,
    val rawText: String = email,
    val domain: String? = null,
    val confidence: Float = 1.0f
)

data class ExtractedUrl(
    val url: String,
    val rawText: String = url,
    val domain: String? = null,
    val confidence: Float = 1.0f
)

data class ExtractedNumber(
    val value: Double,
    val rawText: String,
    val isInteger: Boolean = false,
    val confidence: Float = 1.0f
)

data class ExtractedCurrency(
    val amount: Double,
    val currencySymbol: String,
    val currencyCode: String? = null,
    val rawText: String,
    val confidence: Float = 1.0f
)

data class ExtractedPercentage(
    val value: Double,
    val rawText: String,
    val confidence: Float = 1.0f
)

data class ExtractedIdentifier(
    val id: String,
    val type: String,
    val rawText: String = id,
    val confidence: Float = 1.0f
)

data class ExtractedIntent(
    val intent: String,
    val action: String? = null,
    val target: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f
)

data class ExtractedFact(
    val fact: String,
    val sourceSnippet: String? = null,
    val confidence: Float = 1.0f
)

/**
 * Composite container for all extracted structured information.
 */
data class ExtractedData(
    val source: ExtractionSource,
    val originalText: String,
    val entities: List<ExtractedEntity> = emptyList(),
    val fields: List<ExtractedField> = emptyList(),
    val dates: List<ExtractedDate> = emptyList(),
    val times: List<ExtractedTime> = emptyList(),
    val locations: List<ExtractedLocation> = emptyList(),
    val people: List<ExtractedPerson> = emptyList(),
    val organizations: List<ExtractedOrganization> = emptyList(),
    val phoneNumbers: List<ExtractedPhone> = emptyList(),
    val emails: List<ExtractedEmail> = emptyList(),
    val urls: List<ExtractedUrl> = emptyList(),
    val numbers: List<ExtractedNumber> = emptyList(),
    val currencies: List<ExtractedCurrency> = emptyList(),
    val percentages: List<ExtractedPercentage> = emptyList(),
    val identifiers: List<ExtractedIdentifier> = emptyList(),
    val intents: List<ExtractedIntent> = emptyList(),
    val facts: List<ExtractedFact> = emptyList(),
    val containsSensitiveData: Boolean = false,
    val sensitiveDataTypes: List<String> = emptyList(),
    val hasAmbiguity: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isEmpty(): Boolean {
        return entities.isEmpty() &&
                fields.isEmpty() &&
                dates.isEmpty() &&
                times.isEmpty() &&
                locations.isEmpty() &&
                people.isEmpty() &&
                organizations.isEmpty() &&
                phoneNumbers.isEmpty() &&
                emails.isEmpty() &&
                urls.isEmpty() &&
                numbers.isEmpty() &&
                currencies.isEmpty() &&
                percentages.isEmpty() &&
                identifiers.isEmpty() &&
                intents.isEmpty() &&
                facts.isEmpty()
    }

    companion object {
        fun empty(source: ExtractionSource, text: String = ""): ExtractedData {
            return ExtractedData(
                source = source,
                originalText = text,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
