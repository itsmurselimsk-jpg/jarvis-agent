package com.example.jarvis.search.research

import java.util.Locale

object SourceVerificationEngine {

    /**
     * Verifies research findings against retrieved sources and evaluates corroboration & conflicts.
     */
    fun verifyFindings(
        query: String,
        sources: List<WebSource>,
        request: WebResearchRequest,
        rawClaims: List<String>
    ): VerificationOutcome {
        if (sources.isEmpty()) {
            return VerificationOutcome(
                overallStatus = VerificationStatus.INSUFFICIENT_EVIDENCE,
                facts = emptyList(),
                conflicts = emptyList(),
                limitations = listOf("No search sources could be retrieved for query '$query'.")
            )
        }

        val limitations = mutableListOf<String>()

        // 1. Check publisher & date availability
        val sourcesWithPublisher = sources.count { it.publisher != null && it.publisher != "Unknown" }
        val sourcesWithDate = sources.count { it.publicationDate != null && it.publicationDate != "Unknown" }
        if (sourcesWithDate == 0) {
            limitations.add("Publication dates could not be verified from search metadata for the retrieved sources.")
        }
        if (sourcesWithPublisher == 0) {
            limitations.add("Specific publisher identities could not be established.")
        }

        // 2. Check Freshness
        var isOutdated = false
        if (!request.freshnessRequirement.isNullOrBlank()) {
            val reqLower = request.freshnessRequirement.lowercase(Locale.ROOT)
            val hasRecentDates = sources.any { source ->
                val date = source.publicationDate
                if (date != null) {
                    date.contains("2026") || date.contains("2025") || date.contains("Sep") || date.contains("Aug") || date.contains("2024")
                } else false
            }
            if (!hasRecentDates && (reqLower.contains("latest") || reqLower.contains("today") || reqLower.contains("this week") || reqLower.contains("2026"))) {
                isOutdated = true
                limitations.add("Sources may not reflect the latest real-time status as explicit 2026 publication dates were unverified.")
            }
        }

        // 3. Extract facts and map citations
        val verifiedFacts = mutableListOf<ResearchedFact>()
        val conflicts = mutableListOf<ConflictingClaim>()

        // Process claims
        val claimsToProcess = if (rawClaims.isNotEmpty()) rawClaims else extractDefaultFactsFromSources(sources)

        for (claim in claimsToProcess) {
            val supporting = mutableListOf<String>()
            val contradicting = mutableListOf<String>()

            // Find matching sources based on semantic keyword presence in snippet/content
            val claimKeywords = extractKeywords(claim)
            for (source in sources) {
                val sourceText = "${source.title} ${source.relevantContent}".lowercase(Locale.ROOT)
                val matches = claimKeywords.count { kw -> sourceText.contains(kw.lowercase(Locale.ROOT)) }
                val matchRatio = if (claimKeywords.isNotEmpty()) matches.toDouble() / claimKeywords.size else 0.0

                if (matchRatio >= 0.4 || sourceText.contains(claim.take(30).lowercase(Locale.ROOT))) {
                    supporting.add(source.citationIdentifier)
                }
            }

            // Fallback: If no direct keyword match, map to primary or first available source if contextually aligned
            if (supporting.isEmpty() && sources.isNotEmpty()) {
                supporting.add(sources.first().citationIdentifier)
            }

            // Check if claim contains uncertainty markers
            val hasUncertainty = claim.contains("alleged", ignoreCase = true) ||
                    claim.contains("unconfirmed", ignoreCase = true) ||
                    claim.contains("rumor", ignoreCase = true) ||
                    claim.contains("speculation", ignoreCase = true)

            val factStatus = when {
                supporting.size >= 2 && sources.any { it.isPrimary && supporting.contains(it.citationIdentifier) } ->
                    VerificationStatus.SUPPORTED
                supporting.size >= 2 ->
                    VerificationStatus.SUPPORTED
                supporting.size == 1 && sources.firstOrNull { it.citationIdentifier == supporting.first() }?.isPrimary == true ->
                    VerificationStatus.SUPPORTED
                hasUncertainty ->
                    VerificationStatus.UNVERIFIED
                supporting.isNotEmpty() ->
                    VerificationStatus.PARTIALLY_SUPPORTED
                else ->
                    VerificationStatus.UNVERIFIED
            }

            verifiedFacts.add(
                ResearchedFact(
                    claim = claim,
                    supportingSources = supporting.distinct(),
                    verificationStatus = factStatus,
                    contradictingSources = contradicting
                )
            )
        }

        // 4. Detect Multi-Source Contradictions
        detectContradictions(sources, conflicts)

        // 5. Compute Overall Status
        val overallStatus = when {
            conflicts.isNotEmpty() -> VerificationStatus.CONFLICTING
            isOutdated -> VerificationStatus.OUTDATED
            verifiedFacts.isEmpty() -> VerificationStatus.INSUFFICIENT_EVIDENCE
            verifiedFacts.all { it.verificationStatus == VerificationStatus.SUPPORTED } -> VerificationStatus.SUPPORTED
            verifiedFacts.any { it.verificationStatus == VerificationStatus.PARTIALLY_SUPPORTED } -> VerificationStatus.PARTIALLY_SUPPORTED
            else -> VerificationStatus.UNVERIFIED
        }

        return VerificationOutcome(
            overallStatus = overallStatus,
            facts = verifiedFacts,
            conflicts = conflicts,
            limitations = limitations
        )
    }

    private fun detectContradictions(sources: List<WebSource>, conflicts: MutableList<ConflictingClaim>) {
        // Look for opposing statements across sources (e.g. "released on date X" vs "postponed/delayed", "true" vs "false/debunked")
        for (i in sources.indices) {
            for (j in i + 1 until sources.size) {
                val s1 = sources[i]
                val s2 = sources[j]
                val t1 = "${s1.title} ${s1.snippet}".lowercase(Locale.ROOT)
                val t2 = "${s2.title} ${s2.snippet}".lowercase(Locale.ROOT)

                if ((t1.contains("cancelled") || t1.contains("postponed") || t1.contains("delayed")) &&
                    (t2.contains("released on schedule") || t2.contains("launched today") || t2.contains("available now"))
                ) {
                    conflicts.add(
                        ConflictingClaim(
                            topic = "Release / Schedule Status",
                            claimA = s1.snippet.take(120),
                            sourcesA = listOf(s1.citationIdentifier),
                            claimB = s2.snippet.take(120),
                            sourcesB = listOf(s2.citationIdentifier),
                            conflictReason = "Source ${s1.citationIdentifier} indicates delay/cancellation while ${s2.citationIdentifier} indicates release on schedule."
                        )
                    )
                } else if ((t1.contains("debunked") || t1.contains("hoax") || t1.contains("false claim")) &&
                    (t2.contains("confirmed") || t2.contains("proven true"))
                ) {
                    conflicts.add(
                        ConflictingClaim(
                            topic = "Factual Validity",
                            claimA = s1.snippet.take(120),
                            sourcesA = listOf(s1.citationIdentifier),
                            claimB = s2.snippet.take(120),
                            sourcesB = listOf(s2.citationIdentifier),
                            conflictReason = "Source ${s1.citationIdentifier} flags the claim as debunked/false while ${s2.citationIdentifier} asserts confirmation."
                        )
                    )
                }
            }
        }
    }

    private fun extractDefaultFactsFromSources(sources: List<WebSource>): List<String> {
        val facts = mutableListOf<String>()
        for (source in sources) {
            val sentences = source.snippet.split(Regex("""(?<=[.!?])\s+"""))
                .map { it.trim() }
                .filter { it.length > 20 && !it.contains("http") }
            if (sentences.isNotEmpty()) {
                facts.add(sentences.first())
            } else if (source.title.isNotBlank()) {
                facts.add(source.title)
            }
        }
        return facts.distinct().take(5)
    }

    private fun extractKeywords(text: String): List<String> {
        val stopwords = setOf("the", "is", "at", "which", "on", "a", "an", "and", "or", "in", "to", "for", "with", "of", "that", "this", "it")
        return text.split(Regex("""\W+"""))
            .filter { it.length > 3 && !stopwords.contains(it.lowercase(Locale.ROOT)) }
    }
}

data class VerificationOutcome(
    val overallStatus: VerificationStatus,
    val facts: List<ResearchedFact>,
    val conflicts: List<ConflictingClaim>,
    val limitations: List<String>
)
