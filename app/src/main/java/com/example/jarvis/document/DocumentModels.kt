package com.example.jarvis.document

import com.example.jarvis.extraction.ExtractedCurrency
import com.example.jarvis.extraction.ExtractedData
import com.example.jarvis.extraction.ExtractedDate
import com.example.jarvis.extraction.ExtractedEntity
import java.nio.charset.Charset
import java.util.Locale

/**
 * Standard classification of supported document types.
 */
enum class DocumentType {
    TXT,
    CSV,
    JSON,
    XML,
    MARKDOWN,
    PDF,
    DOCX,
    XLSX,
    UNKNOWN;

    companion object {
        fun fromFileNameAndMime(fileName: String, mimeType: String?): DocumentType {
            val lowerName = fileName.lowercase(Locale.ROOT)
            val lowerMime = mimeType?.lowercase(Locale.ROOT) ?: ""

            return when {
                lowerName.endsWith(".txt") || lowerMime.contains("text/plain") -> TXT
                lowerName.endsWith(".csv") || lowerMime.contains("text/csv") || lowerMime.contains("text/comma-separated-values") -> CSV
                lowerName.endsWith(".json") || lowerMime.contains("application/json") -> JSON
                lowerName.endsWith(".xml") || lowerMime.contains("text/xml") || lowerMime.contains("application/xml") -> XML
                lowerName.endsWith(".md") || lowerName.endsWith(".markdown") || lowerMime.contains("text/markdown") -> MARKDOWN
                lowerName.endsWith(".pdf") || lowerMime.contains("application/pdf") -> PDF
                lowerName.endsWith(".docx") || lowerMime.contains("wordprocessingml") -> DOCX
                lowerName.endsWith(".xlsx") || lowerMime.contains("spreadsheetml") -> XLSX
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Extraction status of the document reader.
 */
enum class DocumentExtractionStatus {
    SUCCESS,
    PARTIAL,
    EMPTY,
    OVERSIZED,
    UNSUPPORTED,
    FAILED
}

/**
 * Structured document section preserving headings and hierarchy.
 */
data class DocumentSection(
    val title: String?,
    val content: String,
    val level: Int = 1
)

/**
 * Metadata about document structure.
 */
data class DocumentMetadata(
    val lineCount: Int = 0,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val sectionCount: Int = 0,
    val rowCount: Int = 0,
    val columnNames: List<String> = emptyList(),
    val custom: Map<String, String> = emptyMap()
)

/**
 * Complete structured document model.
 */
data class DocumentModel(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val documentType: DocumentType,
    val extractedText: String,
    val sections: List<DocumentSection> = emptyList(),
    val pageOrSectionCount: Int = 1,
    val metadata: DocumentMetadata = DocumentMetadata(),
    val extractionStatus: DocumentExtractionStatus = DocumentExtractionStatus.SUCCESS,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val containsSensitiveData: Boolean = false,
    val sensitiveDataTypes: List<String> = emptyList(),
    val rawContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isSuccessful(): Boolean = extractionStatus == DocumentExtractionStatus.SUCCESS ||
            extractionStatus == DocumentExtractionStatus.PARTIAL
}

/**
 * High-level summary of the document for AgentBrain, tools, and UI.
 */
data class DocumentSummary(
    val fileName: String,
    val documentType: DocumentType,
    val summaryText: String,
    val keyPoints: List<String> = emptyList(),
    val extractedData: ExtractedData,
    val importantEntities: List<ExtractedEntity> = emptyList(),
    val importantDates: List<ExtractedDate> = emptyList(),
    val importantAmounts: List<ExtractedCurrency> = emptyList(),
    val extractionStatus: DocumentExtractionStatus = DocumentExtractionStatus.SUCCESS,
    val containsSensitiveData: Boolean = false,
    val sensitiveDataTypes: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Configuration options for document readers.
 */
data class DocumentReaderOptions(
    val maxSizeBytes: Long = 5 * 1024 * 1024L, // 5MB limit
    val maxLines: Int = 10000,
    val charset: Charset = Charsets.UTF_8
)
