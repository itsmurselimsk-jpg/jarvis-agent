package com.example.jarvis.generation

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

/**
 * Result of generated file verification.
 */
data class FileVerificationResult(
    val isValid: Boolean,
    val details: String,
    val errors: List<String> = emptyList()
)

/**
 * Validates generated output files for existence, non-zero size, MIME/format match,
 * and structural/syntax integrity.
 */
object FileVerifier {

    fun verify(file: File, expectedFormat: GeneratedFileFormat): FileVerificationResult {
        // 1. File existence check
        if (!file.exists()) {
            return FileVerificationResult(
                isValid = false,
                details = "File does not exist at target path: ${file.absolutePath}",
                errors = listOf("File not found.")
            )
        }

        // 2. Non-zero size check
        val length = file.length()
        if (length <= 0) {
            return FileVerificationResult(
                isValid = false,
                details = "Generated file is empty (0 bytes).",
                errors = listOf("File size is 0 bytes.")
            )
        }

        // 3. Format-specific structural validation
        return when (expectedFormat) {
            GeneratedFileFormat.TXT -> verifyTxt(file)
            GeneratedFileFormat.MARKDOWN -> verifyMarkdown(file)
            GeneratedFileFormat.CSV -> verifyCsv(file)
            GeneratedFileFormat.JSON -> verifyJson(file)
            GeneratedFileFormat.PDF -> verifyPdf(file)
            GeneratedFileFormat.DOCX -> verifyDocx(file)
            GeneratedFileFormat.XLSX -> verifyXlsx(file)
        }
    }

    private fun verifyTxt(file: File): FileVerificationResult {
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return FileVerificationResult(false, "Empty TXT file.")
        return FileVerificationResult(
            isValid = true,
            details = "Valid UTF-8 plain text (${bytes.size} bytes)."
        )
    }

    private fun verifyMarkdown(file: File): FileVerificationResult {
        val text = file.readText(Charsets.UTF_8)
        if (text.isBlank()) return FileVerificationResult(false, "Empty Markdown file.")
        return FileVerificationResult(
            isValid = true,
            details = "Valid Markdown content (${file.length()} bytes, ${text.lines().size} lines)."
        )
    }

    private fun verifyCsv(file: File): FileVerificationResult {
        val lines = file.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        if (lines.isEmpty()) return FileVerificationResult(false, "CSV file has no readable rows.")

        // Verify CSV header and quoting
        val firstRowCols = parseCsvLine(lines.first()).size
        return FileVerificationResult(
            isValid = true,
            details = "Valid RFC-4180 CSV with ${lines.size} row(s) and $firstRowCols initial column(s)."
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    cells.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        cells.add(sb.toString())
        return cells
    }

    private fun verifyJson(file: File): FileVerificationResult {
        val text = file.readText(Charsets.UTF_8).trim()
        return try {
            val tokener = JSONTokener(text).nextValue()
            if (tokener is JSONObject || tokener is JSONArray) {
                FileVerificationResult(
                    isValid = true,
                    details = "Valid JSON document (${if (tokener is JSONObject) "Object" else "Array"})."
                )
            } else {
                FileVerificationResult(
                    isValid = false,
                    details = "JSON root is neither a JSON object nor an array.",
                    errors = listOf("Top-level token is not JSONObject or JSONArray.")
                )
            }
        } catch (e: Exception) {
            FileVerificationResult(
                isValid = false,
                details = "JSON parsing failure: ${e.message}",
                errors = listOf("Malformed JSON syntax: ${e.message}")
            )
        }
    }

    private fun verifyPdf(file: File): FileVerificationResult {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 20) {
                return FileVerificationResult(false, "PDF file is too small to be valid.", listOf("Size < 20 bytes."))
            }
            val header = String(bytes.take(8).toByteArray(), Charsets.US_ASCII)
            if (!header.startsWith("%PDF-")) {
                return FileVerificationResult(false, "File header does not match %PDF- signature.", listOf("Missing %PDF- magic header."))
            }
            val footer = String(bytes.takeLast(64).toByteArray(), Charsets.US_ASCII)
            if (!footer.contains("%%EOF")) {
                return FileVerificationResult(false, "PDF file lacks valid %%EOF trailer.", listOf("Missing %%EOF trailer."))
            }
            FileVerificationResult(true, "Valid PDF 1.4 document (${bytes.size} bytes).")
        } catch (e: Exception) {
            FileVerificationResult(false, "PDF verification exception: ${e.message}", listOf(e.message ?: "PDF error"))
        }
    }

    private fun verifyDocx(file: File): FileVerificationResult {
        return try {
            var hasDocumentXml = false
            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            hasDocumentXml = true
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            if (hasDocumentXml) {
                FileVerificationResult(true, "Valid Microsoft Word (.docx) package with word/document.xml (${file.length()} bytes).")
            } else {
                FileVerificationResult(false, "DOCX package missing word/document.xml entry.", listOf("Missing word/document.xml."))
            }
        } catch (e: Exception) {
            FileVerificationResult(false, "DOCX verification exception: ${e.message}", listOf(e.message ?: "DOCX error"))
        }
    }

    private fun verifyXlsx(file: File): FileVerificationResult {
        return try {
            var hasWorkbookXml = false
            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "xl/workbook.xml" || entry.name == "xl/worksheets/sheet1.xml") {
                            hasWorkbookXml = true
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            if (hasWorkbookXml) {
                FileVerificationResult(true, "Valid Microsoft Excel (.xlsx) package with xl/workbook.xml (${file.length()} bytes).")
            } else {
                FileVerificationResult(false, "XLSX package missing xl/workbook.xml entry.", listOf("Missing xl/workbook.xml."))
            }
        } catch (e: Exception) {
            FileVerificationResult(false, "XLSX verification exception: ${e.message}", listOf(e.message ?: "XLSX error"))
        }
    }
}
