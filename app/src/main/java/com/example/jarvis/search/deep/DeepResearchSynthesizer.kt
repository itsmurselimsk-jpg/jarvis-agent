package com.example.jarvis.search.deep

import com.example.jarvis.search.research.VerificationStatus
import com.example.jarvis.search.research.WebSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeepResearchSynthesizer {

    /**
     * Synthesizes the deep research findings into an executive report with validated citations.
     */
    fun formatReport(result: DeepResearchResult): String {
        return buildString {
            appendLine("### 🔬 DEEP RESEARCH REPORT")
            appendLine("**Topic:** ${result.plan.normalizedQuestion}")
            appendLine("**Research Depth:** ${result.plan.researchDepth.name} | **Rounds:** ${result.roundsCompleted} | **Sources Evaluated:** ${result.sources.size}")
            if (result.isPartial) {
                appendLine("> ⚠️ **Notice:** This inquiry yielded partial results (${if (result.isCancelled) "User Cancelled" else "Budget Reached"}).")
            }
            appendLine()

            // 1. DIRECT ANSWER
            appendLine("#### DIRECT ANSWER")
            appendLine(result.directAnswer.trim())
            appendLine()

            // 2. KEY FINDINGS
            if (result.keyFindings.isNotEmpty()) {
                appendLine("#### KEY FINDINGS")
                result.keyFindings.forEach { finding ->
                    val cleanFinding = finding.trimStart('•', '-', '*').trim()
                    appendLine("• $cleanFinding")
                }
                appendLine()
            }

            // 3. EVIDENCE BY SUB-QUESTION
            if (result.plan.subQuestions.isNotEmpty()) {
                appendLine("#### EVIDENCE & SUB-ANALYSIS")
                for (subQ in result.plan.subQuestions) {
                    appendLine("**${subQ.question}**")
                    appendLine("*Purpose:* ${subQ.purpose}")

                    val relevantClaims = result.claims.filter { it.claimId.contains(subQ.id) }
                    if (relevantClaims.isNotEmpty()) {
                        for (claim in relevantClaims) {
                            val badge = claim.verificationState.badge
                            val cites = if (claim.citations.isNotEmpty()) " ${claim.citations.joinToString(" ")}" else ""
                            appendLine("  • $badge ${claim.text}$cites")
                        }
                    } else if (subQ.findings.isNotEmpty()) {
                        for (finding in subQ.findings) {
                            appendLine("  • $finding")
                        }
                    } else {
                        appendLine("  • ℹ️ *Evidence collected from retrieved sources.*")
                    }
                    appendLine()
                }
            }

            // 4. CONFLICTS / UNCERTAINTIES
            if (result.conflicts.isNotEmpty() || result.claims.any { it.verificationState == VerificationStatus.CONFLICTING || it.verificationState == VerificationStatus.UNVERIFIED }) {
                appendLine("#### CONFLICTS & UNCERTAINTIES")
                for (conflict in result.conflicts) {
                    appendLine("• **Contradiction on ${conflict.topic}:**")
                    appendLine("  - *Assertion A:* ${conflict.claimA} ${conflict.sourcesA.joinToString(" ")}")
                    appendLine("  - *Assertion B:* ${conflict.claimB} ${conflict.sourcesB.joinToString(" ")}")
                    appendLine("  - *Reason:* ${conflict.conflictReason}")
                }
                val unverified = result.claims.filter { it.verificationState == VerificationStatus.UNVERIFIED }
                for (unv in unverified) {
                    appendLine("• ❓ **Unverified Claim:** ${unv.text} *(Insufficient independent corroboration)*")
                }
                appendLine()
            }

            // 5. LIMITATIONS
            if (result.limitations.isNotEmpty() || result.unresolvedGaps.isNotEmpty()) {
                appendLine("#### LIMITATIONS")
                result.limitations.forEach { lim ->
                    appendLine("• $lim")
                }
                result.unresolvedGaps.forEach { gap ->
                    appendLine("• Unresolved aspect: ${gap.missingTopic}")
                }
                appendLine()
            }

            // 6. SOURCES & PROVENANCE
            if (result.sources.isNotEmpty()) {
                appendLine("#### SOURCES")
                for (source in result.sources) {
                    val pubStr = source.publisher?.let { " — *$it*" } ?: ""
                    val dateStr = source.publicationDate?.let { " (Published: $it)" } ?: " (Date: unverified)"
                    val signals = if (source.credibilitySignals.isNotEmpty()) {
                        " `[${source.credibilitySignals.joinToString { it.label }}]`"
                    } else ""

                    appendLine("${source.citationIdentifier} **${source.title}**$pubStr$dateStr")
                    appendLine("   🔗 ${source.url}$signals")
                }
            }

            appendLine()
            val timestampFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(result.plan.createdAt))
            appendLine("---")
            appendLine("*JARVIS Deep Research Engine • Verified at $timestampFmt*")
        }
    }
}
