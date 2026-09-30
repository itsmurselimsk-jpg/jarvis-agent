package com.example.jarvis.search.research

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResearchResponseFormatter {

    /**
     * Formats the complete WebResearchResult into a structured Markdown citation document.
     */
    fun format(result: WebResearchResult): String {
        return buildString {
            appendLine("### WEB RESEARCH DOSSIER: ${result.normalizedQuery.uppercase(Locale.ROOT)}")
            appendLine()

            // 1. Answer Synthesis
            if (result.answerSummary.isNotBlank()) {
                appendLine("#### Executive Synthesis")
                appendLine(result.answerSummary)
                appendLine()
            }

            // 2. Key Findings with In-line Citations
            if (result.keyFacts.isNotEmpty()) {
                appendLine("#### Key Verified Findings")
                result.keyFacts.forEach { fact ->
                    val citations = fact.supportingSources.joinToString(" ")
                    val badge = fact.verificationStatus.badge
                    appendLine("• $badge ${fact.claim} $citations")
                }
                appendLine()
            } else if (result.extractedFindings.isNotEmpty()) {
                appendLine("#### Extracted Findings")
                result.extractedFindings.forEachIndexed { idx, finding ->
                    appendLine("• $finding [${idx + 1}]")
                }
                appendLine()
            }

            // 3. Conflicting Claims (if any detected)
            if (result.conflictingClaims.isNotEmpty()) {
                appendLine("#### ⚠️ Conflicting Claims Detected")
                result.conflictingClaims.forEach { conflict ->
                    appendLine("• **${conflict.topic}**:")
                    appendLine("  - Claim A: \"${conflict.claimA}\" ${conflict.sourcesA.joinToString()}")
                    appendLine("  - Claim B: \"${conflict.claimB}\" ${conflict.sourcesB.joinToString()}")
                    appendLine("  - *Divergence Reason*: ${conflict.conflictReason}")
                }
                appendLine()
            }

            // 4. Sources Section
            appendLine("#### Sources & Evidence")
            if (result.sources.isEmpty()) {
                appendLine("*(No sources retrieved)*")
            } else {
                result.sources.forEach { src ->
                    val typeStr = src.sourceType.displayName
                    val pub = src.publisher ?: "Unknown Publisher"
                    val date = src.publicationDate ?: "Date Unknown"
                    val primaryBadge = if (src.isPrimary) "⭐ Primary" else "Secondary"
                    val signals = if (src.credibilitySignals.isNotEmpty()) {
                        " | " + src.credibilitySignals.joinToString(", ") { it.label }
                    } else ""

                    appendLine("${src.citationIdentifier} **${src.title}**")
                    appendLine("   - Publisher / Domain: $pub ($src.domain) [$typeStr | $primaryBadge$signals]")
                    appendLine("   - Published: $date | Retrieved: ${src.retrievedDate}")
                    appendLine("   - URL: ${src.url}")
                    appendLine("   - Snippet: \"${src.snippet.take(160)}...\"")
                    appendLine()
                }
            }

            // 5. Verification Status & Integrity Audit
            appendLine("#### Verification & Integrity Audit")
            appendLine("• **Status**: ${result.verificationStatus.badge} ${result.verificationStatus.displayName}")
            val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(result.researchTimestamp))
            appendLine("• **Audit Timestamp**: $timeFmt")

            if (result.limitations.isNotEmpty()) {
                appendLine("• **Methodological Limitations**:")
                result.limitations.forEach { lim ->
                    appendLine("  - $lim")
                }
            }
        }
    }
}
