package com.example.jarvis.code

import java.util.Locale

/**
 * Recognized programming and markup languages for safe static analysis.
 */
enum class CodeLanguage(val displayName: String, val extensions: List<String>) {
    KOTLIN("Kotlin", listOf("kt", "kts")),
    JAVA("Java", listOf("java")),
    PYTHON("Python", listOf("py", "pyw")),
    JAVASCRIPT("JavaScript", listOf("js", "mjs", "cjs", "jsx")),
    TYPESCRIPT("TypeScript", listOf("ts", "tsx")),
    JSON("JSON", listOf("json")),
    XML("XML", listOf("xml")),
    HTML("HTML", listOf("html", "htm")),
    CSS("CSS", listOf("css", "scss", "sass")),
    MARKDOWN("Markdown", listOf("md", "markdown")),
    SQL("SQL", listOf("sql")),
    SHELL("Shell / Bash", listOf("sh", "bash", "zsh", "env")),
    GRADLE("Gradle DSL", listOf("gradle", "gradle.kts")),
    UNKNOWN("Plain Text / Unknown", emptyList());

    companion object {
        fun fromExtension(ext: String): CodeLanguage {
            val clean = ext.lowercase(Locale.ROOT).removePrefix(".")
            return entries.firstOrNull { it.extensions.contains(clean) } ?: UNKNOWN
        }
    }
}

/**
 * Severity level of an individual code finding.
 */
enum class FindingSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Categorization of static and semantic code findings.
 */
enum class FindingCategory {
    SYNTAX,
    LOGIC,
    BUG,
    SECURITY,
    PERFORMANCE,
    STYLE,
    MAINTAINABILITY,
    API_USAGE,
    DEPENDENCY,
    NULLABILITY,
    CONCURRENCY,
    ERROR_HANDLING
}

/**
 * An individual finding from code analysis.
 */
data class CodeFinding(
    val severity: FindingSeverity,
    val category: FindingCategory,
    val file: String? = null,
    val line: Int? = null,
    val lineRange: Pair<Int, Int>? = null,
    val description: String,
    val explanation: String,
    val suggestedFix: String? = null
)

/**
 * Quantitative metrics computed during deterministic code inspection.
 */
data class ComplexityIndicators(
    val totalLines: Int = 0,
    val codeLines: Int = 0,
    val commentLines: Int = 0,
    val blankLines: Int = 0,
    val estimatedMaxNestingDepth: Int = 0,
    val functionCount: Int = 0,
    val classCount: Int = 0,
    val importCount: Int = 0,
    val estimatedComplexityScore: String = "Low"
)

/**
 * Comprehensive result of a code inspection / analysis pass.
 */
data class CodeAnalysisResult(
    val language: CodeLanguage,
    val filesAnalyzed: List<String> = emptyList(),
    val summary: String,
    val findings: List<CodeFinding> = emptyList(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val complexityIndicators: ComplexityIndicators = ComplexityIndicators(),
    val securityFindings: List<CodeFinding> = emptyList(),
    val confidence: Float = 1.0f,
    val isAiAssisted: Boolean = false,
    val containsRedactedSecrets: Boolean = false
)

/**
 * Parsed output from runtime crashes, compiler errors, and stack traces.
 */
data class StackTraceAnalysisResult(
    val errorType: String,
    val message: String,
    val likelySourceLocation: String? = null,
    val surroundingContext: String? = null,
    val detectedCauses: List<String> = emptyList(),
    val likelyCauses: List<String> = emptyList(),
    val suggestedFixes: List<String> = emptyList(),
    val rawStackTraceSnippet: String = ""
)
