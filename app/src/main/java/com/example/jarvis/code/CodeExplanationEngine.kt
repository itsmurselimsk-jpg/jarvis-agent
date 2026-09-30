package com.example.jarvis.code

/**
 * Generates clear, human-readable structured responses for user code queries:
 * - "Explain this code"
 * - "What does this function do?"
 * - "Find the bug"
 * - "Review this code"
 * - "How can I improve this?"
 * - "Why is this error happening?"
 */
object CodeExplanationEngine {

    enum class CodeIntent {
        EXPLAIN,
        FIND_BUG,
        REVIEW,
        IMPROVE,
        DEBUG_ERROR,
        GENERAL_ANALYSIS
    }

    fun detectIntent(query: String): CodeIntent {
        val lower = query.lowercase()
        return when {
            lower.contains("why is this error") || lower.contains("fix this error") || lower.contains("stack trace") || lower.contains("crash") || lower.contains("exception") -> CodeIntent.DEBUG_ERROR
            lower.contains("find the bug") || lower.contains("find bug") || lower.contains("what is the bug") || lower.contains("is there a bug") || lower.contains("debug this") -> CodeIntent.FIND_BUG
            lower.contains("review this code") || lower.contains("code review") || lower.contains("audit this code") -> CodeIntent.REVIEW
            lower.contains("how can i improve") || lower.contains("improve this") || lower.contains("refactor") || lower.contains("optimize") -> CodeIntent.IMPROVE
            lower.contains("explain this code") || lower.contains("what does this") || lower.contains("explain function") || lower.contains("understand this") -> CodeIntent.EXPLAIN
            else -> CodeIntent.GENERAL_ANALYSIS
        }
    }

    fun formatExplanation(
        query: String,
        result: CodeAnalysisResult,
        stackTrace: StackTraceAnalysisResult? = null
    ): String {
        val intent = detectIntent(query)
        val sb = StringBuilder()

        if (stackTrace != null) {
            sb.appendLine("JARVIS CODE DIAGNOSTIC — ERROR ANALYSIS")
            sb.appendLine("=========================================")
            sb.appendLine("• Error Type: ${stackTrace.errorType}")
            sb.appendLine("• Message: ${stackTrace.message}")
            if (stackTrace.likelySourceLocation != null) {
                sb.appendLine("• Origin Location: ${stackTrace.likelySourceLocation}")
            }
            if (stackTrace.detectedCauses.isNotEmpty()) {
                sb.appendLine("\nDetected Root Cause:")
                stackTrace.detectedCauses.forEach { sb.appendLine("  - $it") }
            }
            if (stackTrace.likelyCauses.isNotEmpty()) {
                sb.appendLine("\nLikely Explanation:")
                stackTrace.likelyCauses.forEach { sb.appendLine("  - $it") }
            }
            if (stackTrace.suggestedFixes.isNotEmpty()) {
                sb.appendLine("\nSuggested Fixes:")
                stackTrace.suggestedFixes.forEach { sb.appendLine("  ✔ $it") }
            }
            return sb.toString().trimEnd()
        }

        when (intent) {
            CodeIntent.EXPLAIN -> {
                sb.appendLine("CODE EXPLANATION — ${result.language.displayName.uppercase()}")
                sb.appendLine("=========================================")
                sb.appendLine(result.summary)
                sb.appendLine()
                sb.appendLine("Structure & Scope:")
                sb.appendLine("• Total Lines: ${result.complexityIndicators.totalLines} (${result.complexityIndicators.codeLines} code lines, ${result.complexityIndicators.commentLines} comments)")
                sb.appendLine("• Functions: ${result.complexityIndicators.functionCount} | Classes: ${result.complexityIndicators.classCount} | Imports: ${result.complexityIndicators.importCount}")
                sb.appendLine("• Complexity Rating: ${result.complexityIndicators.estimatedComplexityScore} (Nesting depth: ${result.complexityIndicators.estimatedMaxNestingDepth})")

                if (result.findings.isNotEmpty()) {
                    sb.appendLine("\nKey Structural Notes:")
                    result.findings.take(4).forEach { f ->
                        val loc = if (f.line != null) " [Line ${f.line}]" else ""
                        sb.appendLine("• ${f.category}$loc: ${f.description}")
                    }
                }
            }

            CodeIntent.FIND_BUG -> {
                sb.appendLine("BUG INSPECTION REPORT — ${result.language.displayName.uppercase()}")
                sb.appendLine("=========================================")
                val bugFindings = result.findings.filter { it.category == FindingCategory.BUG || it.category == FindingCategory.LOGIC || it.category == FindingCategory.NULLABILITY || it.severity >= FindingSeverity.HIGH }

                if (bugFindings.isEmpty()) {
                    sb.appendLine("No overt bugs, crash patterns, or critical logic defects detected in deterministic analysis.")
                } else {
                    sb.appendLine("Identified ${bugFindings.size} potential bug(s) / defect pattern(s):")
                    bugFindings.forEachIndexed { i, f ->
                        val loc = if (f.line != null) " [Line ${f.line}]" else ""
                        sb.appendLine("\n${i + 1}. [${f.severity}] ${f.description}$loc")
                        sb.appendLine("   Explanation: ${f.explanation}")
                        if (f.suggestedFix != null) {
                            sb.appendLine("   Suggested Fix: ${f.suggestedFix}")
                        }
                    }
                }
            }

            CodeIntent.REVIEW -> {
                sb.appendLine("CODE REVIEW AUDIT — ${result.language.displayName.uppercase()}")
                sb.appendLine("=========================================")
                sb.appendLine(result.summary)
                sb.appendLine()
                sb.appendLine("Metrics: ${result.complexityIndicators.codeLines} LoC | Nesting: ${result.complexityIndicators.estimatedMaxNestingDepth} | Complexity: ${result.complexityIndicators.estimatedComplexityScore}")

                if (result.securityFindings.isNotEmpty()) {
                    sb.appendLine("\nSECURITY ALERTS (${result.securityFindings.size}):")
                    result.securityFindings.forEach { sf ->
                        sb.appendLine("• [${sf.severity}] ${sf.description}${if (sf.line != null) " (Line ${sf.line})" else ""}")
                    }
                }

                if (result.findings.isNotEmpty()) {
                    sb.appendLine("\nDetailed Review Findings (${result.findings.size}):")
                    result.findings.forEach { f ->
                        val loc = if (f.line != null) " (Line ${f.line})" else ""
                        sb.appendLine("• [${f.category} / ${f.severity}]$loc: ${f.description}")
                        if (f.suggestedFix != null) {
                            sb.appendLine("  ↳ Fix: ${f.suggestedFix}")
                        }
                    }
                }

                if (result.suggestions.isNotEmpty()) {
                    sb.appendLine("\nRecommendations:")
                    result.suggestions.forEach { sb.appendLine("• $it") }
                }
            }

            CodeIntent.IMPROVE -> {
                sb.appendLine("CODE REFACTORING & IMPROVEMENT SUGGESTIONS")
                sb.appendLine("=========================================")
                sb.appendLine("Language: ${result.language.displayName} | Complexity: ${result.complexityIndicators.estimatedComplexityScore}")
                sb.appendLine()

                if (result.suggestions.isNotEmpty()) {
                    sb.appendLine("Refactoring Recommendations:")
                    result.suggestions.forEach { sb.appendLine("✔ $it") }
                    sb.appendLine()
                }

                val actionable = result.findings.filter { it.suggestedFix != null }
                if (actionable.isNotEmpty()) {
                    sb.appendLine("Targeted Code Improvements:")
                    actionable.forEach { f ->
                        val loc = if (f.line != null) "Line ${f.line}: " else ""
                        sb.appendLine("• $loc${f.description}")
                        sb.appendLine("  Fix: ${f.suggestedFix}")
                    }
                }
            }

            else -> {
                sb.appendLine("CODE ANALYSIS REPORT — ${result.language.displayName.uppercase()}")
                sb.appendLine("=========================================")
                sb.appendLine(result.summary)
                sb.appendLine()
                sb.appendLine("Complexity: ${result.complexityIndicators.estimatedComplexityScore} (${result.complexityIndicators.codeLines} code lines, ${result.complexityIndicators.functionCount} functions)")

                if (result.findings.isNotEmpty()) {
                    sb.appendLine("\nFindings (${result.findings.size}):")
                    result.findings.forEach { f ->
                        val loc = if (f.line != null) " [Line ${f.line}]" else ""
                        sb.appendLine("• [${f.category} - ${f.severity}]$loc ${f.description}")
                        if (f.suggestedFix != null) {
                            sb.appendLine("  Fix: ${f.suggestedFix}")
                        }
                    }
                }

                if (result.suggestions.isNotEmpty()) {
                    sb.appendLine("\nSuggestions:")
                    result.suggestions.forEach { sb.appendLine("• $it") }
                }
            }
        }

        return sb.toString().trimEnd()
    }
}
