package com.example.jarvis.brain

import com.example.jarvis.code.AiCodeAnalyzer
import com.example.jarvis.code.CodeAnalysisResult
import com.example.jarvis.code.CodeExplanationEngine
import com.example.jarvis.code.CodeLanguageDetector
import com.example.jarvis.code.DeterministicCodeAnalyzer
import com.example.jarvis.code.StackTraceAnalyzer
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel

/**
 * Safe, read-only code analysis tool for AgentBrain.
 * Analyzes, explains, reviews, and diagnoses user-supplied source code, files, or stack traces.
 *
 * ANALYSIS-ONLY BOUNDARY:
 * - Does NOT execute code or run shell commands from user code.
 * - Does NOT automatically modify source files.
 * - Sanitizes all secrets before any AI analysis.
 */
class CodeAnalysisTool : Tool {
    override val name = "CodeAnalysis"
    override val description = "Inspects, reviews, explains, debugs, and analyzes source code snippets, loaded files, or stack traces safely."
    override val riskLevel = RiskLevel.SAFE
    override val permissions = emptyList<String>()

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        // 1. Extract code content to analyze (from input snippet or active attached document)
        val extractedCode = extractCodeSnippet(input, context)
        val fileName = context.activeDocument?.fileName

        if (extractedCode.isBlank()) {
            return ToolResult(
                success = false,
                output = "No source code or stack trace found to analyze. Please provide a code snippet, paste an error log, or attach a code document.",
                verified = true
            )
        }

        // 2. Check if input is a stack trace / error log
        if (StackTraceAnalyzer.isStackTraceOrErrorLog(extractedCode)) {
            val traceResult = StackTraceAnalyzer.analyze(extractedCode)
            val formatted = CodeExplanationEngine.formatExplanation(input, DeterministicCodeAnalyzer.analyze(extractedCode, fileName), traceResult)
            context.repository.logActivity("Code Error Diagnosed", "${traceResult.errorType}: ${traceResult.message.take(40)}", ActivityType.TOOL_EXECUTION)

            return ToolResult(
                success = true,
                output = formatted,
                verified = true,
                metadata = mapOf("errorType" to traceResult.errorType, "location" to (traceResult.likelySourceLocation ?: "unknown"))
            )
        }

        // 3. Deterministic static analysis pass
        val deterministicResult = DeterministicCodeAnalyzer.analyze(extractedCode, fileName)

        // 4. Check if AIProvider is available for semantic review
        // Note: Secrets are sanitized within AiCodeAnalyzer before sending
        val finalResult: CodeAnalysisResult = deterministicResult

        // 5. Format human-readable response matching user query intent
        val outputText = CodeExplanationEngine.formatExplanation(input, finalResult)

        context.repository.logActivity(
            "Code Analysis Completed",
            "${finalResult.language.displayName} (${finalResult.complexityIndicators.codeLines} LoC, ${finalResult.findings.size} findings)",
            ActivityType.TOOL_EXECUTION
        )

        return ToolResult(
            success = true,
            output = outputText,
            verified = true,
            metadata = mapOf(
                "language" to finalResult.language.name,
                "findingsCount" to "${finalResult.findings.size}",
                "complexity" to finalResult.complexityIndicators.estimatedComplexityScore,
                "secretsDetected" to "${finalResult.containsRedactedSecrets}"
            )
        )
    }

    private fun extractCodeSnippet(input: String, context: ToolContext): String {
        // A. Check if user provided markdown code blocks in prompt: ```...```
        val codeBlockRegex = Regex("""```(?:\w+)?\s*([\s\S]*?)```""")
        val match = codeBlockRegex.find(input)
        if (match != null) {
            val snippet = match.groups[1]?.value?.trim() ?: ""
            if (snippet.isNotBlank()) return snippet
        }

        // B. Check if active document exists and has code content
        val activeDoc = context.activeDocument
        if (activeDoc != null && activeDoc.extractedText.isNotBlank()) {
            return activeDoc.extractedText
        }

        // C. If query has multiple lines, use lines after the first query phrase
        val lines = input.lines()
        if (lines.size > 1) {
            val candidate = lines.drop(1).joinToString("\n").trim()
            if (candidate.isNotBlank()) return candidate
        }

        // D. Check if the input itself contains code syntax keywords
        if (input.contains("fun ") || input.contains("class ") || input.contains("def ") ||
            input.contains("import ") || input.contains("var ") || input.contains("val ") ||
            input.contains("public ") || input.contains("Exception") || input.contains("{")) {
            return input.trim()
        }

        return ""
    }
}
