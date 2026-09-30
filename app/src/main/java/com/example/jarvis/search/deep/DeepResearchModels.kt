package com.example.jarvis.search.deep

import com.example.jarvis.search.research.ConflictingClaim
import com.example.jarvis.search.research.VerificationStatus
import com.example.jarvis.search.research.WebSource

/**
 * Depth level for deep research planning.
 */
enum class ResearchDepth {
    QUICK,
    STANDARD,
    DEEP
}

/**
 * Lifecycle status of the overall research plan.
 */
enum class ResearchPlanStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    PARTIALLY_COMPLETED,
    CANCELLED,
    FAILED
}

/**
 * Lifecycle status of individual sub-questions.
 */
enum class SubQuestionStatus {
    PENDING,
    SEARCHING,
    ANALYZING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Classification of factual evidence.
 */
enum class EvidenceType {
    DIRECT,
    INDIRECT,
    CONTEXTUAL,
    CONTRADICTORY,
    INSUFFICIENT
}

/**
 * Fine-grained granular evidence unit mapped to a specific source and claim.
 */
data class ResearchEvidence(
    val evidenceId: String,
    val claim: String,
    val sourceId: String,
    val supportingText: String,
    val sourceUrl: String,
    val sourceDate: String? = null,
    val evidenceType: EvidenceType = EvidenceType.DIRECT,
    val relevance: String = "HIGH",
    val verificationState: VerificationStatus = VerificationStatus.SUPPORTED,
    val confidence: Float = 0.9f,
    val extractedAt: Long = System.currentTimeMillis()
)

/**
 * A discrete factual claim with supporting and contradicting evidence trails.
 */
data class ResearchClaim(
    val claimId: String,
    val text: String,
    val supportingEvidence: List<ResearchEvidence> = emptyList(),
    val contradictingEvidence: List<ResearchEvidence> = emptyList(),
    val verificationState: VerificationStatus = VerificationStatus.SUPPORTED,
    val confidence: Float = 0.9f,
    val citations: List<String> = emptyList()
)

/**
 * A focused sub-aspect of the overarching research inquiry.
 */
data class ResearchSubQuestion(
    val id: String,
    val question: String,
    val purpose: String,
    val searchQueries: List<String>,
    val status: SubQuestionStatus = SubQuestionStatus.PENDING,
    val sources: List<WebSource> = emptyList(),
    val findings: List<String> = emptyList(),
    val unresolvedIssues: List<String> = emptyList()
)

/**
 * Configurable budget limits to strictly prevent runaway or infinite research execution.
 */
data class ResearchBudget(
    val maxSubQuestions: Int = 4,
    val maxQueriesPerSubQuestion: Int = 2,
    val maxSources: Int = 12,
    val maxResearchRounds: Int = 3,
    val maxRetries: Int = 2,
    val maxContentSize: Int = 25000
)

/**
 * Identified gap in evidence during multi-round research.
 */
data class EvidenceGap(
    val gapId: String,
    val subQuestionId: String,
    val missingTopic: String,
    val suggestedQuery: String,
    val resolved: Boolean = false
)

/**
 * Structured research blueprint generated from a complex inquiry.
 */
data class ResearchPlan(
    val originalQuestion: String,
    val normalizedQuestion: String,
    val subQuestions: List<ResearchSubQuestion>,
    val searchQueries: List<String>,
    val requiredEvidence: List<String> = emptyList(),
    val researchDepth: ResearchDepth = ResearchDepth.STANDARD,
    val createdAt: Long = System.currentTimeMillis(),
    val status: ResearchPlanStatus = ResearchPlanStatus.PLANNED
)

/**
 * Comprehensive outcome of a Deep Research inquiry.
 */
data class DeepResearchResult(
    val plan: ResearchPlan,
    val directAnswer: String,
    val keyFindings: List<String>,
    val claims: List<ResearchClaim>,
    val evidence: List<ResearchEvidence>,
    val conflicts: List<ConflictingClaim> = emptyList(),
    val unresolvedGaps: List<EvidenceGap> = emptyList(),
    val limitations: List<String> = emptyList(),
    val sources: List<WebSource> = emptyList(),
    val roundsCompleted: Int = 1,
    val formattedReport: String = "",
    val isCancelled: Boolean = false,
    val isPartial: Boolean = false,
    val success: Boolean = true,
    val errorMessage: String? = null
)
