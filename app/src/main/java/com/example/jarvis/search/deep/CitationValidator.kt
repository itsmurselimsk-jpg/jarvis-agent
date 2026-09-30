package com.example.jarvis.search.deep

import com.example.jarvis.search.research.VerificationStatus
import com.example.jarvis.search.research.WebSource

object CitationValidator {

    data class ValidationResult(
        val validClaims: List<ResearchClaim>,
        val unverifiedClaims: List<ResearchClaim>,
        val removedFabricatedCitations: Int,
        val isValid: Boolean,
        val auditTrail: List<String>
    )

    /**
     * Strictly verifies citation integrity against retrieved web sources.
     */
    fun validateCitations(
        claims: List<ResearchClaim>,
        sources: List<WebSource>
    ): ValidationResult {
        val validSourceIds = sources.map { it.id }.toSet()
        val validClaims = mutableListOf<ResearchClaim>()
        val unverifiedClaims = mutableListOf<ResearchClaim>()
        val auditTrail = mutableListOf<String>()
        var fabricatedCitationCount = 0

        for (claim in claims) {
            val validCitationsForClaim = mutableListOf<String>()
            var hadInvalidCitation = false

            for (citation in claim.citations) {
                val cleanId = citation.trim('[', ']', ' ')
                if (validSourceIds.contains(cleanId)) {
                    validCitationsForClaim.add("[$cleanId]")
                } else {
                    fabricatedCitationCount++
                    hadInvalidCitation = true
                    auditTrail.add("Flagged fabricated or unretrieved source citation [$cleanId] for claim '${claim.text.take(40)}...'")
                }
            }

            // Check if claim text has embedded hallucinated brackets like [99]
            val textCitations = Regex("\\[(\\d+)\\]").findAll(claim.text).map { it.groupValues[1] }.toList()
            for (id in textCitations) {
                if (!validSourceIds.contains(id)) {
                    fabricatedCitationCount++
                    hadInvalidCitation = true
                    auditTrail.add("Detected non-existent inline citation [$id] in claim text.")
                }
            }

            val sanitizedText = claim.text.replace(Regex("\\[(\\d+)\\]")) { matchResult ->
                val id = matchResult.groupValues[1]
                if (validSourceIds.contains(id)) "[$id]" else ""
            }.replace(Regex("\\s+"), " ").trim()

            if (validCitationsForClaim.isEmpty()) {
                val downgraded = claim.copy(
                    text = sanitizedText,
                    citations = emptyList(),
                    verificationState = VerificationStatus.UNVERIFIED,
                    confidence = 0.3f
                )
                unverifiedClaims.add(downgraded)
                auditTrail.add("Downgraded claim '${claim.text.take(40)}...' to UNVERIFIED due to zero valid source citations.")
            } else {
                val validated = claim.copy(
                    text = sanitizedText,
                    citations = validCitationsForClaim,
                    verificationState = if (hadInvalidCitation && claim.verificationState == VerificationStatus.SUPPORTED) {
                        VerificationStatus.PARTIALLY_SUPPORTED
                    } else {
                        claim.verificationState
                    }
                )
                validClaims.add(validated)
            }
        }

        return ValidationResult(
            validClaims = validClaims,
            unverifiedClaims = unverifiedClaims,
            removedFabricatedCitations = fabricatedCitationCount,
            isValid = fabricatedCitationCount == 0,
            auditTrail = auditTrail
        )
    }

    /**
     * Sanitizes markdown text by removing unindexed citation brackets.
     */
    fun sanitizeTextCitations(text: String, sources: List<WebSource>): String {
        val validIds = sources.map { it.id }.toSet()
        return text.replace(Regex("\\[(\\d+)\\]")) { match ->
            val id = match.groupValues[1]
            if (validIds.contains(id)) "[$id]" else ""
        }.replace(Regex("\\s+"), " ").trim()
    }
}
