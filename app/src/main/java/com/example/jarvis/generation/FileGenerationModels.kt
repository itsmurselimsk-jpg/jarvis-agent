package com.example.jarvis.generation

import java.io.File
import java.util.Locale

/**
 * Standard supported output formats for the File Generation Pipeline.
 */
enum class GeneratedFileFormat(
    val extension: String,
    val mimeType: String,
    val isBinary: Boolean
) {
    TXT("txt", "text/plain", false),
    MARKDOWN("md", "text/markdown", false),
    CSV("csv", "text/csv", false),
    JSON("json", "application/json", false),
    PDF("pdf", "application/pdf", true),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", true),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true);

    companion object {
        fun fromExtension(ext: String): GeneratedFileFormat? {
            val cleanExt = ext.trimStart('.').lowercase(Locale.ROOT)
            return values().firstOrNull { it.extension.equals(cleanExt, ignoreCase = true) }
        }

        fun fromFileName(fileName: String): GeneratedFileFormat? {
            val ext = fileName.substringAfterLast('.', "")
            return fromExtension(ext)
        }

        fun fromMimeType(mime: String): GeneratedFileFormat? {
            val lower = mime.lowercase(Locale.ROOT)
            return values().firstOrNull { it.mimeType.equals(lower, ignoreCase = true) }
        }
    }
}

/**
 * Encapsulates tabular data rows for CSV / XLSX generation.
 */
data class GenerationTable(
    val title: String? = null,
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
)

/**
 * Encapsulates a structured document section for TXT, Markdown, DOCX, and PDF generators.
 */
data class GenerationSection(
    val title: String? = null,
    val content: String = "",
    val level: Int = 1,
    val items: List<String> = emptyList(),
    val isCodeBlock: Boolean = false,
    val codeLanguage: String? = null
)

/**
 * Request payload detailing what file to generate and its configuration.
 */
data class GenerationRequest(
    val fileName: String,
    val format: GeneratedFileFormat,
    val title: String = "",
    val rawContent: String = "",
    val sections: List<GenerationSection> = emptyList(),
    val tables: List<GenerationTable> = emptyList(),
    val jsonObject: Any? = null,
    val prettyPrintJson: Boolean = true,
    val allowOverwrite: Boolean = false,
    val targetDirectory: File? = null,
    val author: String = "JARVIS AI Assistant"
)

/**
 * Result returned by the File Generation Pipeline.
 * Must include verification status before being considered successful.
 */
data class GenerationResult(
    val success: Boolean,
    val fileName: String,
    val format: GeneratedFileFormat,
    val mimeType: String,
    val file: File? = null,
    val absolutePath: String? = null,
    val contentUri: String? = null,
    val sizeBytes: Long = 0L,
    val verified: Boolean = false,
    val verificationDetails: String? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val message: String = ""
)

/**
 * Contract for all format-specific generators.
 */
interface FileGenerator {
    val format: GeneratedFileFormat

    /**
     * Generates the raw bytes for the requested file format.
     */
    suspend fun generate(request: GenerationRequest): ByteArray
}
