package com.example.jarvis.code

import com.example.jarvis.provider.AIProvider
import com.example.jarvis.recovery.ResponseRepair
import org.json.JSONArray
import org.json.JSONObject

/**
 * Safe AI-Assisted semantic code review engine.
 * Mandatory rules:
 * - Code is sanitized before sending (secrets NEVER sent to AIProvider).
 * - Enforces strict JSON response schema.
 * - Validates AI response; falls back to deterministic analysis if parsing or network fails.
 * - Never fabricates findings.
 */
object AiCodeAnalyzer {

    suspend fun analyzeWithAi(
        rawContent: String,
        fileName: String?,
        language: CodeLanguage,
        aiProvider: AIProvider,
        deterministicResult: CodeAnalysisResult
    ): CodeAnalysisResult {
        // 1. Mandatory Sanitization: Secrets are replaced with safe redactions
        val (sanitizedCode, secretFindings) = CodeSecurityScanner.sanitizeForAiTransmission(rawContent)

        // 2. Structured Prompt with JSON Schema
        val systemInstruction = """
            You are a secure, expert static code analysis assistant.
            Review the supplied source code for bugs, logic errors, security issues, nullability problems, and performance bottlenecks.
            Respond ONLY with a valid JSON object adhering strictly to this format:
            {
              "summary": "Brief executive summary of findings",
              "findings": [
                {
                  "severity": "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO",
                  "category": "SYNTAX" | "LOGIC" | "BUG" | "SECURITY" | "PERFORMANCE" | "STYLE" | "MAINTAINABILITY" | "API_USAGE" | "DEPENDENCY" | "NULLABILITY" | "CONCURRENCY" | "ERROR_HANDLING",
                  "line": 10,
                  "description": "Short issue description",
                  "explanation": "Why this is an issue",
                  "suggestedFix": "Concrete code suggestion or fix"
                }
              ],
              "suggestions": ["Suggestion 1", "Suggestion 2"]
            }
            Do NOT include conversational preambles outside the JSON.
        """.trimIndent()

        val prompt = """
            Language: ${language.displayName}
            File Name: ${fileName ?: "code_snippet"}
            Source Code:
            ```
            $sanitizedCode
            ```
        """.trimIndent()

        return try {
            val response = aiProvider.generateResponse(
                prompt = prompt,
                systemInstruction = systemInstruction,
                onChunkReceived = {}
            )

            // 3. Structured parsing with ResponseRepair
            val json = ResponseRepair.extractCleanJson(response)
            if (json != null) {
                parseAiJsonResponse(json, fileName, language, deterministicResult, secretFindings)
            } else {
                // Fallback to deterministic result on JSON extraction failure
                deterministicResult.copy(
                    summary = "${deterministicResult.summary} (AI semantic parsing unparseable; retained deterministic analysis.)"
                )
            }
        } catch (_: Exception) {
            // Safe fallback on provider timeout/rate limit/error
            deterministicResult.copy(
                summary = "${deterministicResult.summary} (AI provider offline; retained deterministic analysis.)"
            )
        }
    }

    private fun parseAiJsonResponse(
        json: JSONObject,
        fileName: String?,
        language: CodeLanguage,
        deterministicResult: CodeAnalysisResult,
        secretFindings: List<CodeFinding>
    ): CodeAnalysisResult {
        val aiSummary = json.optString("summary", deterministicResult.summary)
        val findingsList = mutableListOf<CodeFinding>()

        // Retain all secret findings (from deterministic scanner)
        findingsList.addAll(secretFindings)

        // Parse AI findings
        val findingsArray = json.optJSONArray("findings") ?: JSONArray()
        for (i in 0 until findingsArray.length()) {
            val item = findingsArray.optJSONObject(i) ?: continue
            val sevStr = item.optString("severity", "LOW")
            val catStr = item.optString("category", "LOGIC")
            val line = if (item.has("line") && !item.isNull("line")) item.optInt("line") else null
            val desc = item.optString("description", "")
            val exp = item.optString("explanation", "")
            val fix = if (item.has("suggestedFix")) item.optString("suggestedFix") else null

            if (desc.isNotBlank()) {
                val severity = try { FindingSeverity.valueOf(sevStr.uppercase()) } catch (_: Exception) { FindingSeverity.LOW }
                val category = try { FindingCategory.valueOf(catStr.uppercase()) } catch (_: Exception) { FindingCategory.LOGIC }

                findingsList.add(
                    CodeFinding(
                        severity = severity,
                        category = category,
                        file = fileName,
                        line = if (line != null && line > 0) line else null,
                        description = desc,
                        explanation = exp.ifBlank { desc },
                        suggestedFix = fix
                    )
                )
            }
        }

        // Merge deterministic structural findings that AI might have missed
        for (df in deterministicResult.findings) {
            if (df.category != FindingCategory.SECURITY && findingsList.none { it.description.equals(df.description, ignoreCase = true) }) {
                findingsList.add(df)
            }
        }

        val suggestionsList = mutableListOf<String>()
        val sugArray = json.optJSONArray("suggestions")
        if (sugArray != null) {
            for (i in 0 until sugArray.length()) {
                val s = sugArray.optString(i)
                if (s.isNotBlank()) suggestionsList.add(s)
            }
        }
        suggestionsList.addAll(deterministicResult.suggestions)

        return deterministicResult.copy(
            summary = aiSummary.ifBlank { deterministicResult.summary },
            findings = findingsList.distinctBy { "${it.category}-${it.line}-${it.description}" },
            suggestions = suggestionsList.distinct(),
            isAiAssisted = true,
            containsRedactedSecrets = secretFindings.isNotEmpty()
        )
    }
}
