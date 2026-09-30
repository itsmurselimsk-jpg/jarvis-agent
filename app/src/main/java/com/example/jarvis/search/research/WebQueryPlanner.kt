package com.example.jarvis.search.research

import com.example.jarvis.vision.SensitiveDataFilter
import java.util.Locale

object WebQueryPlanner {

    private val PREFIX_CLEANERS = listOf(
        "search the web for latest",
        "search the web for recent",
        "search the web for",
        "search the web about",
        "search the web",
        "search for latest",
        "search for recent",
        "search for",
        "search about",
        "search on the web for",
        "search web for",
        "find the latest",
        "find recent",
        "find official information about",
        "find official info about",
        "find information about",
        "find info about",
        "research about",
        "research on",
        "research",
        "look up",
        "what do reliable sources say about",
        "what do sources say about",
        "what is known about",
        "check whether this is true:",
        "check whether this is true",
        "verify this claim:",
        "verify this claim",
        "verify whether",
        "verify that",
        "verify if",
        "compare these sources:",
        "compare sources on",
        "fact check:",
        "fact check",
        "google "
    )

    private val FRESHNESS_PATTERNS = listOf(
        Regex("""\b(today|tonight)\b""", RegexOption.IGNORE_CASE) to "today",
        Regex("""\b(latest|current|newest|recent|recently)\b""", RegexOption.IGNORE_CASE) to "latest",
        Regex("""\b(this\s+week)\b""", RegexOption.IGNORE_CASE) to "this week",
        Regex("""\b(this\s+month)\b""", RegexOption.IGNORE_CASE) to "this month",
        Regex("""\b(this\s+year)\b""", RegexOption.IGNORE_CASE) to "this year",
        Regex("""\bas\s+of\s+([a-zA-Z0-9\s,]+)\b""", RegexOption.IGNORE_CASE) to "as of date"
    )

    private val SITE_PATTERN = Regex("""(?i)\bsite:([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b""")
    private val EXCLUDE_SITE_PATTERN = Regex("""(?i)-site:([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b""")

    /**
     * Plans and normalizes a natural language research query safely.
     * Redacts any detected sensitive credentials before planning.
     */
    fun planQuery(rawInput: String): WebResearchRequest {
        // 1. Sensitive Data Protection: Never leak credentials to search queries
        val sanitized = SensitiveDataFilter.redactSensitiveData(rawInput).trim()

        var workingQuery = sanitized
        val preferredDomains = mutableListOf<String>()
        val excludedDomains = mutableListOf<String>()

        // 2. Extract site: filters if specified in query
        SITE_PATTERN.findAll(workingQuery).forEach { match ->
            preferredDomains.add(match.groupValues[1].lowercase(Locale.ROOT))
        }
        workingQuery = SITE_PATTERN.replace(workingQuery, "").trim()

        EXCLUDE_SITE_PATTERN.findAll(workingQuery).forEach { match ->
            excludedDomains.add(match.groupValues[1].lowercase(Locale.ROOT))
        }
        workingQuery = EXCLUDE_SITE_PATTERN.replace(workingQuery, "").trim()

        // 3. Detect freshness intent
        var detectedFreshness: String? = null
        for ((pattern, label) in FRESHNESS_PATTERNS) {
            val match = pattern.find(workingQuery)
            if (match != null) {
                detectedFreshness = if (label == "as of date") match.value else label
                break
            }
        }

        // 4. Strip natural language query prefixes
        var normalized = workingQuery
        for (prefix in PREFIX_CLEANERS) {
            if (normalized.startsWith(prefix, ignoreCase = true)) {
                normalized = normalized.substring(prefix.length).trim()
                break
            }
        }

        // Additional cleanup: remove trailing question marks or punctuation if appropriate
        normalized = normalized.replace(Regex("""^[,\s:;]+"""), "").trim()
        if (normalized.isBlank()) {
            normalized = sanitized
        }

        return WebResearchRequest(
            query = normalized,
            normalizedQuery = normalized,
            preferredDomains = preferredDomains,
            excludedDomains = excludedDomains,
            freshnessRequirement = detectedFreshness
        )
    }

    private fun WebResearchRequest(
        query: String,
        normalizedQuery: String,
        preferredDomains: List<String>,
        excludedDomains: List<String>,
        freshnessRequirement: String?
    ): WebResearchRequest {
        return WebResearchRequest(
            query = normalizedQuery.ifBlank { query },
            preferredDomains = preferredDomains,
            excludedDomains = excludedDomains,
            maxSources = 5,
            freshnessRequirement = freshnessRequirement
        )
    }
}
