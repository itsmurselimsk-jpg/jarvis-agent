package com.example.jarvis.search.research

/**
 * Classification of web sources. Descriptive only — never arbitrary numerical scores.
 */
enum class SourceType(val displayName: String, val badge: String) {
    OFFICIAL("Official", "🏛️"),
    GOVERNMENT("Government", "🏛️"),
    ACADEMIC("Academic", "🎓"),
    DOCUMENTATION("Documentation", "📖"),
    NEWS("News", "📰"),
    COMPANY("Company", "🏢"),
    ORGANIZATION("Organization", "🌐"),
    COMMUNITY("Community", "👥"),
    BLOG("Blog", "✍️"),
    FORUM("Forum", "💬"),
    UNKNOWN("Unknown", "❓")
}

/**
 * Descriptive source quality and corroboration signals.
 */
enum class SourceQualitySignal(val label: String) {
    PRIMARY_SOURCE("Primary source"),
    OFFICIAL_SOURCE("Official source"),
    DIRECT_EVIDENCE("Direct evidence"),
    RECENT_SOURCE("Recent source"),
    INDEPENDENT_CORROBORATION("Independent corroboration"),
    SECONDARY_REPORTING("Secondary reporting"),
    DATE_UNAVAILABLE("Date unavailable"),
    PUBLISHER_UNAVAILABLE("Publisher unavailable")
}

/**
 * Structured verification states for claims and research outcomes.
 */
enum class VerificationStatus(val displayName: String, val badge: String) {
    SUPPORTED("Supported", "✅"),
    PARTIALLY_SUPPORTED("Partially Supported", "⚠️"),
    CONFLICTING("Conflicting Evidence", "⚔️"),
    UNVERIFIED("Unverified", "❓"),
    OUTDATED("Outdated", "⏳"),
    INSUFFICIENT_EVIDENCE("Insufficient Evidence", "🚫")
}

/**
 * Structured representation of a retrieved web source.
 * Missing metadata is strictly kept null/unknown and never fabricated.
 */
data class WebSource(
    val id: String,
    val title: String,
    val url: String,
    val domain: String,
    val publisher: String? = null,
    val publicationDate: String? = null,
    val retrievedDate: String,
    val snippet: String,
    val relevantContent: String = snippet,
    val sourceType: SourceType = SourceType.UNKNOWN,
    val credibilitySignals: List<SourceQualitySignal> = emptyList(),
    val citationIdentifier: String = "[$id]",
    val isPrimary: Boolean = false
)

/**
 * Represents an individual factual claim and its supporting/contradicting citations.
 */
data class ResearchedFact(
    val claim: String,
    val supportingSources: List<String>, // e.g. ["[1]", "[2]"]
    val verificationStatus: VerificationStatus = VerificationStatus.SUPPORTED,
    val contradictingSources: List<String> = emptyList(),
    val context: String? = null
)

/**
 * Represents a detected contradiction between two or more sources.
 */
data class ConflictingClaim(
    val topic: String,
    val claimA: String,
    val sourcesA: List<String>,
    val claimB: String,
    val sourcesB: List<String>,
    val conflictReason: String
)

/**
 * Research request parameters.
 */
data class WebResearchRequest(
    val query: String,
    val preferredDomains: List<String> = emptyList(),
    val excludedDomains: List<String> = emptyList(),
    val maxSources: Int = 5,
    val freshnessRequirement: String? = null, // e.g. "latest", "today", "recent", "this week", "this month"
    val language: String? = null
)

/**
 * Comprehensive result of a web research inquiry.
 */
data class WebResearchResult(
    val originalQuery: String,
    val normalizedQuery: String,
    val sources: List<WebSource>,
    val extractedFindings: List<String>,
    val keyFacts: List<ResearchedFact>,
    val conflictingClaims: List<ConflictingClaim> = emptyList(),
    val verificationStatus: VerificationStatus,
    val limitations: List<String> = emptyList(),
    val researchTimestamp: Long = System.currentTimeMillis(),
    val answerSummary: String,
    val formattedOutput: String = "",
    val success: Boolean = true,
    val errorMessage: String? = null
)
