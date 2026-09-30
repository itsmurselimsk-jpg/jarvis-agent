package com.example.jarvis.document

import com.example.jarvis.extraction.ExtractedCurrency
import com.example.jarvis.extraction.ExtractedDate
import com.example.jarvis.extraction.ExtractedEntity

/**
 * Status classification for advanced file analysis.
 */
enum class FileAnalysisStatus {
    SUCCESS,
    PARTIAL,
    EMPTY,
    OVERSIZED,
    UNSUPPORTED,
    FAILED
}

/**
 * Inferred semantic data types for tabular columns and structured nodes.
 */
enum class InferredDataType {
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    TEXT,
    OBJECT,
    ARRAY,
    NULL,
    MIXED
}

/**
 * Mathematical statistics computed strictly from valid numeric column values.
 */
data class NumericStats(
    val count: Int,
    val min: Double,
    val max: Double,
    val sum: Double,
    val average: Double
)

/**
 * Column-level analysis for tabular data (CSV, TSV, tables).
 */
data class ColumnAnalysis(
    val index: Int,
    val name: String,
    val inferredType: InferredDataType,
    val totalCount: Int,
    val nonNullCount: Int,
    val missingCount: Int,
    val uniqueCount: Int,
    val sampleValues: List<String> = emptyList(),
    val numericStats: NumericStats? = null
)

/**
 * Complete tabular analysis result.
 */
data class TabularAnalysis(
    val rowCount: Int,
    val columnCount: Int,
    val columns: List<ColumnAnalysis>,
    val totalMissingValues: Int,
    val duplicateRowCount: Int,
    val duplicateRowIndices: List<Int> = emptyList(),
    val previewRows: List<Map<String, String>> = emptyList()
)

/**
 * Structural breakdown for hierarchical JSON and XML files.
 */
data class StructuredDataAnalysis(
    val topLevelType: String, // "OBJECT", "ARRAY", "ELEMENT", etc.
    val maxDepth: Int,
    val totalKeyCount: Int,
    val totalElementCount: Int,
    val arrayCount: Int,
    val primitiveTypeCounts: Map<InferredDataType, Int> = emptyMap(),
    val topLevelKeys: List<String> = emptyList(),
    val keyPaths: List<String> = emptyList()
)

/**
 * Content analysis for unstructured or semi-structured text (TXT, Markdown, PDF text).
 */
data class TextAnalysis(
    val lineCount: Int,
    val wordCount: Int,
    val charCount: Int,
    val paragraphCount: Int,
    val headings: List<String> = emptyList(),
    val importantDates: List<ExtractedDate> = emptyList(),
    val importantAmounts: List<ExtractedCurrency> = emptyList(),
    val emails: List<String> = emptyList(),
    val phoneNumbers: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList()
)

/**
 * Structural summary information common across all files.
 */
data class FileStructureInfo(
    val lineCount: Int = 0,
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val sectionCount: Int = 0,
    val maxDepth: Int = 0,
    val topLevelContainer: String = "DOCUMENT",
    val primaryAttributes: Map<String, String> = emptyMap()
)

/**
 * Comprehensive structured result of Advanced File Analysis.
 */
data class FileAnalysisResult(
    val fileName: String,
    val detectedType: DocumentType,
    val mimeType: String,
    val fileSize: Long,
    val hasContent: Boolean,
    val status: FileAnalysisStatus,
    val structureInfo: FileStructureInfo,
    val extractedEntities: List<ExtractedEntity> = emptyList(),
    val tabularData: TabularAnalysis? = null,
    val structuredData: StructuredDataAnalysis? = null,
    val textData: TextAnalysis? = null,
    val statistics: Map<String, Any> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val containsSensitiveData: Boolean = false,
    val sensitiveDataTypes: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isSuccessful(): Boolean = status == FileAnalysisStatus.SUCCESS || status == FileAnalysisStatus.PARTIAL
}

/**
 * Specific value difference between two files.
 */
data class ValueDifference(
    val keyOrLocation: String,
    val originalValue: String?,
    val newValue: String?
)

/**
 * Comparison result between two files.
 */
data class FileComparisonResult(
    val fileA: String,
    val fileB: String,
    val areIdentical: Boolean,
    val typeMatch: Boolean,
    val sizeDifferenceBytes: Long,
    val structuralDifferences: List<String> = emptyList(),
    val addedKeys: List<String> = emptyList(),
    val removedKeys: List<String> = emptyList(),
    val changedValues: List<ValueDifference> = emptyList(),
    val addedRowsCount: Int = 0,
    val removedRowsCount: Int = 0,
    val modifiedRowsCount: Int = 0,
    val changedSections: List<String> = emptyList(),
    val summaryText: String
)
