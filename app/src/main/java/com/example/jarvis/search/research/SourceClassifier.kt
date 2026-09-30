package com.example.jarvis.search.research

import java.net.URI
import java.util.Locale

object SourceClassifier {

    private val GOV_SUFFIXES = listOf(".gov", ".mil", ".gov.uk", ".gov.in", ".gov.bd", ".gov.au", ".gov.ca")
    private val ACADEMIC_DOMAINS = listOf(
        "arxiv.org", "nature.com", "science.org", "ieee.org", "acm.org",
        "nih.gov", "ncbi.nlm.nih.gov", "pubmed.ncbi.nlm.nih.gov", "jstor.org",
        "sciencedirect.com", "springer.com", "cell.com", "biorxiv.org", "medrxiv.org"
    )
    private val DOC_DOMAINS = listOf(
        "developer.android.com", "android.com", "kotlinlang.org", "docs.oracle.com",
        "developer.mozilla.org", "docs.python.org", "docs.microsoft.com", "learn.microsoft.com",
        "cloud.google.com", "firebase.google.com", "w3.org", "rfc-editor.org", "man7.org",
        "docs.github.com", "kubernetes.io", "react.dev", "nodejs.org"
    )
    private val NEWS_DOMAINS = listOf(
        "reuters.com", "apnews.com", "bbc.com", "bbc.co.uk", "nytimes.com",
        "wsj.com", "theguardian.com", "bloomberg.com", "ft.com", "aljazeera.com",
        "techcrunch.com", "theverge.com", "arstechnica.com", "wired.com", "cnn.com",
        "nbcnews.com", "hindustantimes.com", "thedailystar.net", "prothomalo.com"
    )
    private val COMPANY_DOMAINS = listOf(
        "google.com", "apple.com", "microsoft.com", "meta.com", "amazon.com",
        "oracle.com", "ibm.com", "samsung.com", "sony.com", "openai.com", "anthropic.com"
    )
    private val ORG_DOMAINS = listOf(
        "who.int", "un.org", "unesco.org", "wikipedia.org", "eff.org", "w3c.org",
        "apache.org", "linuxfoundation.org", "mozilla.org", "redcross.org"
    )
    private val FORUM_DOMAINS = listOf(
        "reddit.com", "quora.com", "news.ycombinator.com", "discourse.org",
        "forum.xda-developers.com", "groups.google.com"
    )
    private val COMMUNITY_DOMAINS = listOf(
        "stackoverflow.com", "stackexchange.com", "github.com", "gitlab.com",
        "dev.to", "medium.com", "hashnode.com", "substack.com"
    )

    private val DATE_REGEXES = listOf(
        Regex("""\b(20\d{2}[-/](?:0[1-9]|1[0-2])[-/](?:0[1-9]|[12]\d|3[01]))\b"""), // 2026-09-18
        Regex("""\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+(?:[0-9]{1,2}),?\s+(20\d{2})\b""", RegexOption.IGNORE_CASE), // Sep 18, 2026
        Regex("""\b([0-9]{1,2})\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?,?\s+(20\d{2})\b""", RegexOption.IGNORE_CASE), // 18 Sep 2026
        Regex("""\b(20\d{2})\b""") // 2026 (standalone year fallback)
    )

    /**
     * Extracts canonical hostname from a URL safely.
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase(Locale.ROOT) ?: ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (_: Exception) {
            val clean = url.substringAfter("://").substringBefore("/").substringBefore("?").lowercase(Locale.ROOT)
            if (clean.startsWith("www.")) clean.substring(4) else clean
        }
    }

    /**
     * Determines descriptive source type based on domain structure and patterns.
     */
    fun classifySourceType(domain: String, url: String): SourceType {
        val lowerDomain = domain.lowercase(Locale.ROOT)
        val lowerUrl = url.lowercase(Locale.ROOT)

        // 1. Government
        if (GOV_SUFFIXES.any { lowerDomain.endsWith(it) }) {
            return SourceType.GOVERNMENT
        }

        // 2. Academic (.edu or known academic repositories)
        if (lowerDomain.endsWith(".edu") || lowerDomain.endsWith(".ac.uk") || lowerDomain.endsWith(".ac.in") ||
            ACADEMIC_DOMAINS.any { lowerDomain.contains(it) }
        ) {
            return SourceType.ACADEMIC
        }

        // 3. Technical Documentation
        if (DOC_DOMAINS.any { lowerDomain.contains(it) } || lowerUrl.contains("/docs/") || lowerUrl.contains("/documentation/")) {
            return SourceType.DOCUMENTATION
        }

        // 4. Official Organizations
        if (ORG_DOMAINS.any { lowerDomain.contains(it) } || (lowerDomain.endsWith(".org") && !COMMUNITY_DOMAINS.any { lowerDomain.contains(it) })) {
            return SourceType.ORGANIZATION
        }

        // 5. News Outlets
        if (NEWS_DOMAINS.any { lowerDomain.contains(it) }) {
            return SourceType.NEWS
        }

        // 6. Companies
        if (COMPANY_DOMAINS.any { lowerDomain.contains(it) }) {
            return SourceType.COMPANY
        }

        // 7. Forums
        if (FORUM_DOMAINS.any { lowerDomain.contains(it) }) {
            return SourceType.FORUM
        }

        // 8. Community / Blog
        if (COMMUNITY_DOMAINS.any { lowerDomain.contains(it) }) {
            return if (lowerDomain.contains("medium") || lowerDomain.contains("substack") || lowerDomain.contains("blog")) {
                SourceType.BLOG
            } else {
                SourceType.COMMUNITY
            }
        }

        if (lowerDomain.contains("blog")) {
            return SourceType.BLOG
        }

        return SourceType.UNKNOWN
    }

    /**
     * Determines if a source is considered a Primary Source (official gov, doc, academic paper, or corporate announcement).
     */
    fun isPrimarySource(sourceType: SourceType, domain: String): Boolean {
        return when (sourceType) {
            SourceType.GOVERNMENT -> true
            SourceType.DOCUMENTATION -> true
            SourceType.ACADEMIC -> true
            SourceType.OFFICIAL -> true
            SourceType.COMPANY -> true
            else -> false
        }
    }

    /**
     * Derives publisher name from domain or known authority without fabricating unknown details.
     */
    fun derivePublisher(domain: String): String? {
        val lower = domain.lowercase(Locale.ROOT)
        return when {
            lower.contains("developer.android.com") || lower == "android.com" -> "Android Developers"
            lower.contains("kotlinlang.org") -> "JetBrains"
            lower.contains("arxiv.org") -> "arXiv Repository"
            lower.contains("nature.com") -> "Nature Publishing"
            lower.contains("ieee.org") -> "IEEE Xplore"
            lower.contains("who.int") -> "World Health Organization"
            lower.contains("un.org") -> "United Nations"
            lower.contains("reuters.com") -> "Reuters"
            lower.contains("apnews.com") -> "Associated Press"
            lower.contains("bbc.com") || lower.contains("bbc.co.uk") -> "BBC"
            lower.contains("theguardian.com") -> "The Guardian"
            lower.contains("nytimes.com") -> "The New York Times"
            lower.contains("wsj.com") -> "The Wall Street Journal"
            lower.contains("wikipedia.org") -> "Wikipedia"
            lower.contains("github.com") -> "GitHub"
            lower.contains("stackoverflow.com") -> "Stack Overflow"
            lower.endsWith(".gov") -> "Official Government Agency"
            lower.isNotBlank() -> domain // Use domain directly as identifiable publisher
            else -> null
        }
    }

    /**
     * Extracts publication date from snippet or metadata string.
     * Does NOT invent missing dates.
     */
    fun extractPublicationDate(snippet: String, extraMetadata: String? = null): String? {
        val combined = "$snippet ${extraMetadata.orEmpty()}"
        for (regex in DATE_REGEXES) {
            val match = regex.find(combined)
            if (match != null) {
                return match.value.trim()
            }
        }
        return null
    }

    /**
     * Generates descriptive quality signals for a given source without arbitrary numerical scores.
     */
    fun generateQualitySignals(
        sourceType: SourceType,
        isPrimary: Boolean,
        hasPublisher: Boolean,
        hasPublicationDate: Boolean,
        isCorroborated: Boolean
    ): List<SourceQualitySignal> {
        val signals = mutableListOf<SourceQualitySignal>()

        if (isPrimary) {
            signals.add(SourceQualitySignal.PRIMARY_SOURCE)
        }
        if (sourceType == SourceType.GOVERNMENT || sourceType == SourceType.OFFICIAL || sourceType == SourceType.DOCUMENTATION) {
            signals.add(SourceQualitySignal.OFFICIAL_SOURCE)
        }
        if (!isPrimary && (sourceType == SourceType.NEWS || sourceType == SourceType.COMMUNITY || sourceType == SourceType.BLOG)) {
            signals.add(SourceQualitySignal.SECONDARY_REPORTING)
        }
        if (isCorroborated) {
            signals.add(SourceQualitySignal.INDEPENDENT_CORROBORATION)
        }
        if (hasPublicationDate) {
            signals.add(SourceQualitySignal.RECENT_SOURCE)
        } else {
            signals.add(SourceQualitySignal.DATE_UNAVAILABLE)
        }
        if (!hasPublisher) {
            signals.add(SourceQualitySignal.PUBLISHER_UNAVAILABLE)
        }

        return signals
    }
}
