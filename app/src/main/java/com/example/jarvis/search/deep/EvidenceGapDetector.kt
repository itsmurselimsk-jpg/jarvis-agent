package com.example.jarvis.search.deep

import java.util.UUID

object EvidenceGapDetector {

    data class GapAnalysisResult(
        val gapsIdentified: List<EvidenceGap>,
        val requiresNextRound: Boolean,
        val nextRoundQueries: List<String>,
        val reason: String
    )

    /**
     * Inspects sub-questions, claims, and evidence to identify missing evidence and contradictions.
     */
    fun analyzeGaps(
        plan: ResearchPlan,
        subQuestions: List<ResearchSubQuestion>,
        claims: List<ResearchClaim>,
        currentRound: Int,
        budget: ResearchBudget
    ): GapAnalysisResult {
        if (currentRound >= budget.maxResearchRounds) {
            return GapAnalysisResult(
                gapsIdentified = emptyList(),
                requiresNextRound = false,
                nextRoundQueries = emptyList(),
                reason = "Research round budget ($currentRound/${budget.maxResearchRounds}) reached."
            )
        }

        val gaps = mutableListOf<EvidenceGap>()
        val nextRoundQueries = mutableListOf<String>()

        // 1. Check for under-evidenced or empty sub-questions
        for (subQ in subQuestions) {
            val validSourcesCount = subQ.sources.size
            if (validSourcesCount == 0) {
                val gap = EvidenceGap(
                    gapId = "gap-${UUID.randomUUID().toString().take(6)}",
                    subQuestionId = subQ.id,
                    missingTopic = "No sources found for: ${subQ.question}",
                    suggestedQuery = "${plan.normalizedQuestion} ${subQ.purpose}"
                )
                gaps.add(gap)
                nextRoundQueries.add(gap.suggestedQuery)
            } else if (validSourcesCount == 1 && plan.researchDepth == ResearchDepth.DEEP) {
                val gap = EvidenceGap(
                    gapId = "gap-${UUID.randomUUID().toString().take(6)}",
                    subQuestionId = subQ.id,
                    missingTopic = "Insufficient multi-source corroboration for: ${subQ.question}",
                    suggestedQuery = "${subQ.searchQueries.firstOrNull() ?: plan.normalizedQuestion} independent analysis"
                )
                gaps.add(gap)
                nextRoundQueries.add(gap.suggestedQuery)
            }
        }

        // 2. Check for conflicting claims that can be investigated in Round 3
        val conflictingClaims = claims.filter { it.verificationState == com.example.jarvis.search.research.VerificationStatus.CONFLICTING }
        if (conflictingClaims.isNotEmpty() && currentRound >= 2) {
            for (conflict in conflictingClaims) {
                val gap = EvidenceGap(
                    gapId = "gap-conflict-${UUID.randomUUID().toString().take(6)}",
                    subQuestionId = "conflict-resolution",
                    missingTopic = "Contradiction in claim: ${conflict.text.take(60)}",
                    suggestedQuery = "${conflict.text.take(40)} official verification clarification"
                )
                gaps.add(gap)
                nextRoundQueries.add(gap.suggestedQuery)
            }
        }

        val boundedQueries = nextRoundQueries.distinct().take(budget.maxQueriesPerSubQuestion * 2)
        val shouldContinue = boundedQueries.isNotEmpty()

        return GapAnalysisResult(
            gapsIdentified = gaps,
            requiresNextRound = shouldContinue,
            nextRoundQueries = boundedQueries,
            reason = if (shouldContinue) "Identified ${gaps.size} evidence gap(s) requiring targeted investigation." else "All sub-questions satisfactorily corroborated."
        )
    }
}
