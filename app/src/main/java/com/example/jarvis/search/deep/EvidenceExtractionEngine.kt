package com.example.jarvis.search.deep

import com.example.jarvis.search.research.SearchContentSafety
import com.example.jarvis.search.research.VerificationStatus
import com.example.jarvis.search.research.WebSource
import java.util.Locale
import java.util.UUID

object EvidenceExtractionEngine {

    /**
     * Extracts granular evidence units and maps them to research claims.
     */
    fun extractEvidenceFromSources(
        subQuestion: ResearchSubQuestion,
        sources: List<WebSource>
    ): List<ResearchEvidence> {
        val evidenceList = mutableListOf<ResearchEvidence>()

        for (source in sources) {
            // Neutralize prompt injection attempts in web snippets
            val sanitizedSnippet = SearchContentSafety.sanitizeWebContent(source.snippet)
            val sentences = splitIntoSentences(sanitizedSnippet)

            for (sentence in sentences) {
                if (sentence.length < 20) continue

                val evidenceType = classifyEvidenceType(sentence, subQuestion.question)
                val relevance = if (isHighlyRelevant(sentence, subQuestion.question)) "HIGH" else "MEDIUM"
                val confidence = calculateConfidence(source, evidenceType)

                val evidence = ResearchEvidence(
                    evidenceId = "ev-${UUID.randomUUID().toString().take(8)}",
                    claim = sentence,
                    sourceId = source.id,
                    supportingText = sentence,
                    sourceUrl = source.url,
                    sourceDate = source.publicationDate,
                    evidenceType = evidenceType,
                    relevance = relevance,
                    verificationState = if (evidenceType == EvidenceType.CONTRADICTORY) {
                        VerificationStatus.CONFLICTING
                    } else {
                        VerificationStatus.SUPPORTED
                    },
                    confidence = confidence,
                    extractedAt = System.currentTimeMillis()
                )
                evidenceList.add(evidence)
            }
        }

        return evidenceList
    }

    /**
     * Groups evidence units into synthesized claims with full provenance.
     */
    fun groupEvidenceIntoClaims(
        subQuestion: ResearchSubQuestion,
        evidenceList: List<ResearchEvidence>
    ): List<ResearchClaim> {
        if (evidenceList.isEmpty()) {
            return emptyList()
        }

        val claims = mutableListOf<ResearchClaim>()
        val groupedBySimilarity = mutableListOf<MutableList<ResearchEvidence>>()

        for (evidence in evidenceList) {
            var addedToGroup = false
            for (group in groupedBySimilarity) {
                val representative = group.first()
                if (areSemanticallyRelated(representative.claim, evidence.claim)) {
                    group.add(evidence)
                    addedToGroup = true
                    break
                }
            }
            if (!addedToGroup) {
                groupedBySimilarity.add(mutableListOf(evidence))
            }
        }

        for ((index, group) in groupedBySimilarity.withIndex()) {
            val supporting = group.filter { it.evidenceType != EvidenceType.CONTRADICTORY }
            val contradicting = group.filter { it.evidenceType == EvidenceType.CONTRADICTORY }

            val citations = group.map { "[${it.sourceId}]" }.distinct()
            val hasConflicts = contradicting.isNotEmpty() && supporting.isNotEmpty()

            val state = when {
                hasConflicts -> VerificationStatus.CONFLICTING
                supporting.size >= 2 -> VerificationStatus.SUPPORTED
                supporting.isNotEmpty() -> VerificationStatus.PARTIALLY_SUPPORTED
                else -> VerificationStatus.UNVERIFIED
            }

            val avgConfidence = if (group.isNotEmpty()) group.map { it.confidence }.average().toFloat() else 0.5f

            val claimText = group.maxByOrNull { it.claim.length }?.claim ?: group.first().claim

            claims.add(
                ResearchClaim(
                    claimId = "claim-${subQuestion.id}-$index",
                    text = claimText,
                    supportingEvidence = supporting,
                    contradictingEvidence = contradicting,
                    verificationState = state,
                    confidence = avgConfidence,
                    citations = citations
                )
            )
        }

        return claims
    }

    private fun classifyEvidenceType(sentence: String, targetQuestion: String): EvidenceType {
        val lowerSentence = sentence.lowercase(Locale.ROOT)
        val lowerQuestion = targetQuestion.lowercase(Locale.ROOT)

        val contradictionMarkers = listOf(
            "however", "on the contrary", "disputes this", "refuted", "cancelled",
            "delayed indefinitely", "untrue", "false", "contradicts", "no evidence of"
        )
        if (contradictionMarkers.any { lowerSentence.contains(it) }) {
            return EvidenceType.CONTRADICTORY
        }

        val qTokens = lowerQuestion.split(Regex("\\W+")).filter { it.length > 3 }
        val matchCount = qTokens.count { lowerSentence.contains(it) }

        return when {
            matchCount >= 2 -> EvidenceType.DIRECT
            matchCount == 1 -> EvidenceType.INDIRECT
            lowerSentence.contains("defined as") || lowerSentence.contains("released on") || lowerSentence.contains("overview") -> EvidenceType.CONTEXTUAL
            else -> EvidenceType.CONTEXTUAL
        }
    }

    private fun isHighlyRelevant(sentence: String, question: String): Boolean {
        val sWords = sentence.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val qWords = question.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        return sWords.intersect(qWords).size >= 2
    }

    private fun calculateConfidence(source: WebSource, evidenceType: EvidenceType): Float {
        var score = 0.70f
        if (source.isPrimary) score += 0.15f
        if (source.publicationDate != null) score += 0.05f
        if (source.publisher != null) score += 0.05f
        if (evidenceType == EvidenceType.DIRECT) score += 0.05f
        return minOf(0.99f, score)
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 15 }
    }

    private fun areSemanticallyRelated(textA: String, textB: String): Boolean {
        val wordsA = textA.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val wordsB = textB.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false
        val overlap = wordsA.intersect(wordsB).size
        return overlap >= 2 || (overlap.toFloat() / minOf(wordsA.size, wordsB.size).toFloat() >= 0.4f)
    }
}
