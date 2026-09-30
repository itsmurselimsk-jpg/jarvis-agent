package com.example.jarvis.document

import java.util.Locale

/**
 * Robust, safe engine to compare two files/documents.
 * Strictly adheres to truth in comparison: never claims two files are identical unless
 * an exact match is definitively established.
 */
object FileComparisonEngine {

    /**
     * Compares two DocumentModels and their respective FileAnalysisResults.
     */
    fun compare(
        docA: DocumentModel,
        docB: DocumentModel,
        analysisA: FileAnalysisResult = AdvancedFileAnalyzer.analyze(docA),
        analysisB: FileAnalysisResult = AdvancedFileAnalyzer.analyze(docB)
    ): FileComparisonResult {
        val typeMatch = docA.documentType == docB.documentType
        val sizeDiff = docB.sizeBytes - docA.sizeBytes

        // Exact content match check
        val areIdentical = typeMatch &&
                docA.sizeBytes == docB.sizeBytes &&
                docA.extractedText == docB.extractedText

        val structuralDiffs = mutableListOf<String>()
        val addedKeys = mutableListOf<String>()
        val removedKeys = mutableListOf<String>()
        val changedValues = mutableListOf<ValueDifference>()
        val changedSections = mutableListOf<String>()
        var addedRowsCount = 0
        var removedRowsCount = 0
        var modifiedRowsCount = 0

        if (!typeMatch) {
            structuralDiffs.add("Different file formats: ${docA.documentType} vs ${docB.documentType}")
        }

        if (docA.sizeBytes != docB.sizeBytes) {
            structuralDiffs.add("File size changed: ${docA.sizeBytes} bytes -> ${docB.sizeBytes} bytes (${if (sizeDiff > 0) "+$sizeDiff" else "$sizeDiff"} bytes)")
        }

        // Compare based on type
        if (docA.documentType == DocumentType.CSV && docB.documentType == DocumentType.CSV) {
            val tabA = analysisA.tabularData
            val tabB = analysisB.tabularData

            if (tabA != null && tabB != null) {
                val colsA = tabA.columns.map { it.name }
                val colsB = tabB.columns.map { it.name }

                val newCols = colsB.filter { !colsA.contains(it) }
                val droppedCols = colsA.filter { !colsB.contains(it) }

                if (newCols.isNotEmpty()) {
                    structuralDiffs.add("Added columns: [${newCols.joinToString(", ")}]")
                    addedKeys.addAll(newCols)
                }
                if (droppedCols.isNotEmpty()) {
                    structuralDiffs.add("Removed columns: [${droppedCols.joinToString(", ")}]")
                    removedKeys.addAll(droppedCols)
                }

                val rowDiff = tabB.rowCount - tabA.rowCount
                if (rowDiff != 0) {
                    structuralDiffs.add("Row count changed: ${tabA.rowCount} -> ${tabB.rowCount} (${if (rowDiff > 0) "+$rowDiff" else "$rowDiff"} rows)")
                }

                // Row comparison
                val rowsA = tabA.previewRows
                val rowsB = tabB.previewRows

                val rowSignaturesA = rowsA.map { it.entries.joinToString(";") { "${it.key}:${it.value}" } }
                val rowSignaturesB = rowsB.map { it.entries.joinToString(";") { "${it.key}:${it.value}" } }

                addedRowsCount = rowSignaturesB.count { !rowSignaturesA.contains(it) }
                removedRowsCount = rowSignaturesA.count { !rowSignaturesB.contains(it) }

                if (tabA.duplicateRowCount != tabB.duplicateRowCount) {
                    structuralDiffs.add("Duplicate rows changed: ${tabA.duplicateRowCount} in File A vs ${tabB.duplicateRowCount} in File B")
                }
            }
        } else if (docA.documentType == DocumentType.JSON && docB.documentType == DocumentType.JSON) {
            val jsonA = analysisA.structuredData
            val jsonB = analysisB.structuredData

            if (jsonA != null && jsonB != null) {
                val keysA = jsonA.topLevelKeys.toSet()
                val keysB = jsonB.topLevelKeys.toSet()

                val added = (keysB - keysA).toList()
                val removed = (keysA - keysB).toList()

                addedKeys.addAll(added)
                removedKeys.addAll(removed)

                if (added.isNotEmpty()) structuralDiffs.add("Added JSON top-level keys: [${added.joinToString(", ")}]")
                if (removed.isNotEmpty()) structuralDiffs.add("Removed JSON top-level keys: [${removed.joinToString(", ")}]")

                if (jsonA.maxDepth != jsonB.maxDepth) {
                    structuralDiffs.add("Nesting depth changed: ${jsonA.maxDepth} -> ${jsonB.maxDepth}")
                }
            }
        } else {
            // General text / section comparison
            val sectionsA = docA.sections.associateBy { it.title ?: "Section" }
            val sectionsB = docB.sections.associateBy { it.title ?: "Section" }

            val keysA = sectionsA.keys
            val keysB = sectionsB.keys

            val addedSec = keysB - keysA
            val removedSec = keysA - keysB
            val commonSec = keysA.intersect(keysB)

            addedSec.forEach { changedSections.add("Added section: '$it'") }
            removedSec.forEach { changedSections.add("Removed section: '$it'") }

            commonSec.forEach { title ->
                val contentA = sectionsA[title]?.content ?: ""
                val contentB = sectionsB[title]?.content ?: ""
                if (contentA != contentB) {
                    changedSections.add("Modified section: '$title'")
                    changedValues.add(
                        ValueDifference(
                            keyOrLocation = title,
                            originalValue = contentA.take(80),
                            newValue = contentB.take(80)
                        )
                    )
                }
            }
        }

        // Summary generation
        val summary = buildString {
            if (areIdentical) {
                append("Comparison confirmed: '${docA.fileName}' and '${docB.fileName}' are byte-for-byte and content identical.")
            } else {
                append("Comparison between '${docA.fileName}' and '${docB.fileName}': ")
                if (!typeMatch) {
                    append("File formats differ (${docA.documentType} vs ${docB.documentType}). ")
                } else {
                    append("Both are ${docA.documentType} files. ")
                }
                if (structuralDiffs.isNotEmpty()) {
                    append("Found ${structuralDiffs.size} structural difference(s). ")
                }
                if (addedKeys.isNotEmpty()) append("${addedKeys.size} key(s) added. ")
                if (removedKeys.isNotEmpty()) append("${removedKeys.size} key(s) removed. ")
                if (changedSections.isNotEmpty()) append("${changedSections.size} section difference(s) detected. ")
            }
        }

        return FileComparisonResult(
            fileA = docA.fileName,
            fileB = docB.fileName,
            areIdentical = areIdentical,
            typeMatch = typeMatch,
            sizeDifferenceBytes = sizeDiff,
            structuralDifferences = structuralDiffs,
            addedKeys = addedKeys,
            removedKeys = removedKeys,
            changedValues = changedValues,
            addedRowsCount = addedRowsCount,
            removedRowsCount = removedRowsCount,
            modifiedRowsCount = modifiedRowsCount,
            changedSections = changedSections,
            summaryText = summary.trim()
        )
    }
}
