package com.example.jarvis.brain

import com.example.jarvis.generation.FileGenerationPipeline
import com.example.jarvis.generation.FileVerifier
import com.example.jarvis.generation.GeneratedFileFormat
import com.example.jarvis.generation.GenerationRequest
import com.example.jarvis.generation.GenerationSection
import com.example.jarvis.generation.GenerationTable
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.security.SensitiveDataFilter
import java.util.Locale

/**
 * File Generation Tool for JARVIS.
 * Safely generates TXT, Markdown, CSV, JSON, PDF, DOCX, and XLSX files
 * from user requests, active document summaries, or analytical contexts.
 * Enforces:
 * - Format-specific generation and strict verification
 * - Overwrite protection
 * - Safe scoped storage
 * - Never executes generated files or scripts
 */
class FileGenerationTool : Tool {
    override val name = "FileGeneration"
    override val description = "Generates verified, safe files in PDF, DOCX, XLSX, CSV, JSON, TXT, and Markdown (.md) formats."
    override val riskLevel = RiskLevel.SAFE
    override val permissions = emptyList<String>()

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val pipeline = context.fileGenerationPipeline
            ?: FileGenerationPipeline(context.bridge.getApplicationContext())

        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Detect requested format
        val targetFormat = when {
            lower.contains("pdf") -> GeneratedFileFormat.PDF
            lower.contains("docx") || lower.contains("word") || lower.contains("doc") -> GeneratedFileFormat.DOCX
            lower.contains("xlsx") || lower.contains("excel") || lower.contains("spreadsheet") || lower.contains("sheet") -> GeneratedFileFormat.XLSX
            lower.contains("csv") -> GeneratedFileFormat.CSV
            lower.contains("json") -> GeneratedFileFormat.JSON
            lower.contains("markdown") || lower.contains(".md") || lower.contains(" md ") -> GeneratedFileFormat.MARKDOWN
            lower.contains("txt") || lower.contains("text file") || lower.contains("plain text") -> GeneratedFileFormat.TXT
            else -> {
                // Heuristic based on file extension in query if present
                val matchedFormat = GeneratedFileFormat.values().firstOrNull { format ->
                    lower.contains(".${format.extension}")
                }
                matchedFormat ?: GeneratedFileFormat.TXT
            }
        }

        // 2. Determine file name
        val requestedFileName = extractFileName(input, targetFormat)

        // 3. Prepare content from active document or user prompt
        val activeDoc = context.activeDocument
        val docSummary = context.activeDocumentSummary
        val analysis = context.activeFileAnalysis

        val isOverwriteAllowed = lower.contains("overwrite") || lower.contains("force") || lower.contains("replace")

        // Overwrite protection check: If target file exists and user did not specify overwrite, request confirmation
        if (pipeline.fileExists(requestedFileName) && !isOverwriteAllowed) {
            return ToolResult(
                success = false,
                output = "A file named '$requestedFileName' already exists in JARVIS storage. To prevent data loss, JARVIS will not overwrite existing files without explicit authorization. Please confirm if you wish to overwrite this file.",
                verified = false,
                requiresUserAction = true,
                metadata = mapOf(
                    "fileName" to requestedFileName,
                    "format" to targetFormat.name,
                    "overwriteProtection" to "active"
                )
            )
        }

        val request = buildGenerationRequest(
            input = input,
            targetFormat = targetFormat,
            fileName = requestedFileName,
            activeDoc = activeDoc,
            docSummary = docSummary,
            analysis = analysis,
            allowOverwrite = isOverwriteAllowed
        )

        // 4. Generate and verify file
        val result = pipeline.generateFile(request)

        if (!result.success || !result.verified) {
            val errorMsg = result.errors.firstOrNull() ?: result.message
            return ToolResult(
                success = false,
                output = "File generation could not be completed: $errorMsg",
                verified = false,
                metadata = mapOf(
                    "fileName" to result.fileName,
                    "format" to targetFormat.name,
                    "error" to errorMsg
                )
            )
        }

        // 5. Log activity with sensitive data masked
        val sanitizedLogName = SensitiveDataFilter.sanitizeForDisplay(result.fileName)
        context.repository.logActivity(
            "File Generated",
            "$sanitizedLogName (${result.format.name}, ${result.sizeBytes} bytes)",
            ActivityType.TOOL_EXECUTION
        )

        val successMessage = buildString {
            appendLine("FILE GENERATION COMPLETED:")
            appendLine("• File Name: ${result.fileName}")
            appendLine("• Format: ${result.format.name} (.${result.format.extension})")
            appendLine("• File Size: ${result.sizeBytes} bytes")
            appendLine("• MIME Type: ${result.mimeType}")
            appendLine("• Integrity Verification: Passed (${result.verificationDetails})")
            if (result.warnings.isNotEmpty()) {
                appendLine("• Security Notice: ${result.warnings.joinToString("; ")}")
            }
            appendLine("• Status: Stored securely in application sandbox.")
        }.trimEnd()

        return ToolResult(
            success = true,
            output = successMessage,
            verified = true,
            metadata = mapOf(
                "fileName" to result.fileName,
                "format" to result.format.name,
                "sizeBytes" to result.sizeBytes.toString(),
                "mimeType" to result.mimeType,
                "absolutePath" to (result.absolutePath ?: "")
            )
        )
    }

    private fun extractFileName(input: String, format: GeneratedFileFormat): String {
        // Try matching explicit names like name="test.pdf" or file "report.csv" or as report.pdf
        val quotesRegex = Regex("""(?:named?|file|as)\s+["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val quotesMatch = quotesRegex.find(input)
        if (quotesMatch != null) {
            val name = quotesMatch.groupValues[1].trim()
            return if (!name.contains(".")) "$name.${format.extension}" else name
        }

        val asFileRegex = Regex("""\b(?:save|create|export|write|make)\s+(?:this\s+)?(?:as\s+|into\s+)?([a-zA-Z0-9_\-]+\.[a-zA-Z0-9]{2,4})\b""", RegexOption.IGNORE_CASE)
        val asFileMatch = asFileRegex.find(input)
        if (asFileMatch != null) {
            return asFileMatch.groupValues[1].trim()
        }

        val namedRegex = Regex("""\b(?:named?|call(?:ed)?)\s+([a-zA-Z0-9_\-]+)\b""", RegexOption.IGNORE_CASE)
        val namedMatch = namedRegex.find(input)
        if (namedMatch != null) {
            return "${namedMatch.groupValues[1].trim()}.${format.extension}"
        }

        return "jarvis_export_${System.currentTimeMillis()}.${format.extension}"
    }

    private fun buildGenerationRequest(
        input: String,
        targetFormat: GeneratedFileFormat,
        fileName: String,
        activeDoc: com.example.jarvis.document.DocumentModel?,
        docSummary: com.example.jarvis.document.DocumentSummary?,
        analysis: com.example.jarvis.document.FileAnalysisResult?,
        allowOverwrite: Boolean
    ): GenerationRequest {
        val title = if (activeDoc != null) {
            "Executive Report: ${activeDoc.fileName}"
        } else {
            "JARVIS Generated Document"
        }

        val sections = mutableListOf<GenerationSection>()
        val tables = mutableListOf<GenerationTable>()

        if (activeDoc != null && activeDoc.isSuccessful()) {
            // Overview section
            val overview = docSummary?.summaryText ?: activeDoc.extractedText.take(500)
            sections.add(
                GenerationSection(
                    title = "Executive Summary",
                    content = overview,
                    level = 1,
                    items = docSummary?.keyPoints ?: emptyList()
                )
            )

            // Extracted Entities
            val entities = mutableListOf<String>()
            if (docSummary != null) {
                if (docSummary.extractedData.emails.isNotEmpty()) {
                    entities.add("Emails: ${docSummary.extractedData.emails.joinToString { it.email }}")
                }
                if (docSummary.extractedData.phoneNumbers.isNotEmpty()) {
                    entities.add("Phone Numbers: ${docSummary.extractedData.phoneNumbers.joinToString { it.phoneNumber }}")
                }
                if (docSummary.extractedData.urls.isNotEmpty()) {
                    entities.add("Links / URLs: ${docSummary.extractedData.urls.joinToString { it.url }}")
                }
                if (docSummary.importantAmounts.isNotEmpty()) {
                    entities.add("Financial Amounts: ${docSummary.importantAmounts.joinToString { "${it.currencySymbol}${it.amount}" }}")
                }
            }
            if (entities.isNotEmpty()) {
                sections.add(
                    GenerationSection(
                        title = "Key Identifiers & Contacts",
                        items = entities,
                        level = 2
                    )
                )
            }

            // Tabular section if available
            val tab = analysis?.tabularData
            if (tab != null && tab.columns.isNotEmpty()) {
                val headers = tab.columns.map { it.name }
                val rows = tab.previewRows.map { rowMap ->
                    headers.map { col -> rowMap[col] ?: "" }
                }
                tables.add(
                    GenerationTable(
                        title = "Tabular Data Preview (${tab.rowCount} rows)",
                        headers = headers,
                        rows = rows
                    )
                )
            }
        } else {
            // Standalone user request content
            val cleanContent = sanitizeUserPromptContent(input)
            sections.add(
                GenerationSection(
                    title = "Report Content",
                    content = cleanContent,
                    level = 1
                )
            )
        }

        return GenerationRequest(
            fileName = fileName,
            format = targetFormat,
            title = title,
            sections = sections,
            tables = tables,
            rawContent = if (sections.isEmpty()) input else "",
            allowOverwrite = allowOverwrite
        )
    }

    private fun sanitizeUserPromptContent(input: String): String {
        // Strip out instructions like "generate a pdf called test.pdf with text hello world" -> "hello world"
        var text = input
        val contentRegex = Regex("""(?:with\s+(?:the\s+)?(?:text|content|body)|containing)\s+["']?([^"']+)["']?""", RegexOption.IGNORE_CASE)
        val match = contentRegex.find(input)
        if (match != null) {
            text = match.groupValues[1].trim()
        }
        return text
    }
}
