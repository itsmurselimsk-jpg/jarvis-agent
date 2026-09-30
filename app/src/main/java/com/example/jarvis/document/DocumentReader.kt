package com.example.jarvis.document

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.jarvis.security.SensitiveDataFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.Locale

/**
 * Clean reader abstraction for document types.
 */
interface DocumentReader {
    fun canRead(type: DocumentType): Boolean
    suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel
}

/**
 * TXT Document Reader preserving paragraphs and line counts.
 */
class TxtDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.TXT

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(inputStream, options.charset))
        val lines = mutableListOf<String>()
        var lineCount = 0
        var wordCount = 0
        var charCount = 0

        var currentLine: String?
        while (reader.readLine().also { currentLine = it } != null) {
            val l = currentLine ?: break
            lines.add(l)
            lineCount++
            charCount += l.length + 1
            wordCount += l.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
            if (lineCount >= options.maxLines) break
        }

        val fullText = lines.joinToString("\n")
        val sections = mutableListOf<DocumentSection>()

        // Split by empty lines into paragraphs
        val paragraphs = fullText.split(Regex("""\n\s*\n"""))
        paragraphs.forEachIndexed { index, para ->
            val trimmed = para.trim()
            if (trimmed.isNotBlank()) {
                val title = if (trimmed.lines().size > 1 && trimmed.lines().first().length < 60) {
                    trimmed.lines().first()
                } else "Paragraph ${index + 1}"
                sections.add(DocumentSection(title = title, content = trimmed, level = 1))
            }
        }

        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "text/plain",
            sizeBytes = sizeBytes,
            documentType = DocumentType.TXT,
            extractedText = fullText,
            sections = sections,
            pageOrSectionCount = maxOf(1, sections.size),
            metadata = DocumentMetadata(
                lineCount = lineCount,
                wordCount = wordCount,
                charCount = charCount,
                sectionCount = sections.size
            )
        )
    }
}

/**
 * CSV Document Reader preserving columns and tabular row associations.
 */
class CsvDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.CSV

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(inputStream, options.charset))
        val rows = mutableListOf<List<String>>()
        var lineCount = 0

        var currentLine: String?
        while (reader.readLine().also { currentLine = it } != null) {
            val l = currentLine ?: break
            if (l.isNotBlank()) {
                rows.add(parseCsvLine(l))
                lineCount++
            }
            if (lineCount >= options.maxLines) break
        }

        if (rows.isEmpty()) {
            return@withContext DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "text/csv",
                sizeBytes = sizeBytes,
                documentType = DocumentType.CSV,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.EMPTY
            )
        }

        val headers = rows.first()
        val dataRows = rows.drop(1)
        val textBuilder = StringBuilder()
        val sections = mutableListOf<DocumentSection>()

        textBuilder.appendLine("CSV Table: ${headers.joinToString(" | ")}")
        textBuilder.appendLine("Total Rows: ${dataRows.size}")

        dataRows.forEachIndexed { rowIndex, row ->
            val rowItems = mutableListOf<String>()
            headers.forEachIndexed { colIndex, header ->
                val value = row.getOrNull(colIndex)?.trim() ?: ""
                if (value.isNotBlank()) {
                    rowItems.add("$header: $value")
                }
            }
            val formattedRow = "[Row ${rowIndex + 1}] ${rowItems.joinToString(" | ")}"
            textBuilder.appendLine(formattedRow)
            sections.add(DocumentSection(title = "Row ${rowIndex + 1}", content = formattedRow))
        }

        val fullText = textBuilder.toString()
        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "text/csv",
            sizeBytes = sizeBytes,
            documentType = DocumentType.CSV,
            extractedText = fullText,
            sections = sections,
            pageOrSectionCount = maxOf(1, dataRows.size),
            metadata = DocumentMetadata(
                lineCount = lineCount,
                rowCount = dataRows.size,
                columnNames = headers,
                sectionCount = sections.size,
                charCount = fullText.length
            )
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var insideQuotes = false

        for (c in line) {
            when {
                c == '"' -> insideQuotes = !insideQuotes
                c == ',' && !insideQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}

/**
 * JSON Document Reader preserving hierarchical keys and values.
 */
class JsonDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.JSON

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val rawJson = inputStream.bufferedReader(options.charset).readText()
        if (rawJson.isBlank()) {
            return@withContext DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/json",
                sizeBytes = sizeBytes,
                documentType = DocumentType.JSON,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.EMPTY
            )
        }

        val textBuilder = StringBuilder()
        val sections = mutableListOf<DocumentSection>()
        var extractionStatus = DocumentExtractionStatus.SUCCESS
        val warnings = mutableListOf<String>()

        try {
            val tokener = JSONTokener(rawJson)
            val root = tokener.nextValue()

            when (root) {
                is JSONObject -> {
                    flattenJsonObject("", root, textBuilder, sections)
                }
                is JSONArray -> {
                    flattenJsonArray("items", root, textBuilder, sections)
                }
                else -> {
                    textBuilder.append(root.toString())
                }
            }
        } catch (e: Exception) {
            extractionStatus = DocumentExtractionStatus.PARTIAL
            warnings.add("Malformed JSON: ${e.message ?: "Syntax error"}. Preserving raw formatted text.")
            textBuilder.append(rawJson.take(options.maxLines * 100))
        }

        val fullText = textBuilder.toString()
        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "application/json",
            sizeBytes = sizeBytes,
            documentType = DocumentType.JSON,
            extractedText = fullText,
            rawContent = rawJson,
            sections = sections,
            pageOrSectionCount = maxOf(1, sections.size),
            metadata = DocumentMetadata(
                sectionCount = sections.size,
                charCount = fullText.length
            ),
            extractionStatus = extractionStatus,
            warnings = warnings
        )
    }

    private fun flattenJsonObject(
        prefix: String,
        obj: JSONObject,
        sb: StringBuilder,
        sections: MutableList<DocumentSection>
    ) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            val value = obj.get(key)
            when (value) {
                is JSONObject -> flattenJsonObject(fullKey, value, sb, sections)
                is JSONArray -> flattenJsonArray(fullKey, value, sb, sections)
                else -> {
                    val line = "$fullKey: $value"
                    sb.appendLine(line)
                    sections.add(DocumentSection(title = fullKey, content = line))
                }
            }
        }
    }

    private fun flattenJsonArray(
        prefix: String,
        arr: JSONArray,
        sb: StringBuilder,
        sections: MutableList<DocumentSection>
    ) {
        for (i in 0 until arr.length()) {
            val itemKey = "$prefix[$i]"
            val value = arr.get(i)
            when (value) {
                is JSONObject -> flattenJsonObject(itemKey, value, sb, sections)
                is JSONArray -> flattenJsonArray(itemKey, value, sb, sections)
                else -> {
                    val line = "$itemKey: $value"
                    sb.appendLine(line)
                    sections.add(DocumentSection(title = itemKey, content = line))
                }
            }
        }
    }
}

/**
 * XML Document Reader preserving elements, attributes, and text hierarchy.
 */
class XmlDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.XML

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val rawXml = inputStream.bufferedReader(options.charset).readText()
        if (rawXml.isBlank()) {
            return@withContext DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "text/xml",
                sizeBytes = sizeBytes,
                documentType = DocumentType.XML,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.EMPTY
            )
        }

        val textBuilder = StringBuilder()
        val sections = mutableListOf<DocumentSection>()
        var extractionStatus = DocumentExtractionStatus.SUCCESS
        val warnings = mutableListOf<String>()

        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(rawXml))

            var eventType = parser.eventType
            val tagStack = mutableListOf<String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        tagStack.add(tagName)
                        val attrCount = parser.attributeCount
                        val attrs = if (attrCount > 0) {
                            (0 until attrCount).joinToString(", ") { i ->
                                "${parser.getAttributeName(i)}=\"${parser.getAttributeValue(i)}\""
                            }
                        } else null

                        val indent = "  ".repeat(tagStack.size - 1)
                        if (attrs != null) {
                            textBuilder.appendLine("$indent<$tagName $attrs>")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotBlank()) {
                            val indent = "  ".repeat(tagStack.size)
                            val currentTag = tagStack.lastOrNull() ?: "tag"
                            val line = "$indent$currentTag: $text"
                            textBuilder.appendLine(line)
                            sections.add(DocumentSection(title = currentTag, content = line))
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagStack.isNotEmpty()) {
                            tagStack.removeAt(tagStack.size - 1)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            extractionStatus = DocumentExtractionStatus.PARTIAL
            warnings.add("Malformed XML: ${e.message ?: "Syntax error"}. Extracted readable text content.")
            val stripped = rawXml.replace(Regex("""<[^>]*>"""), " ").replace(Regex("""\s+"""), " ").trim()
            textBuilder.append(stripped)
        }

        val fullText = textBuilder.toString()
        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "text/xml",
            sizeBytes = sizeBytes,
            documentType = DocumentType.XML,
            extractedText = fullText,
            rawContent = rawXml,
            sections = sections,
            pageOrSectionCount = maxOf(1, sections.size),
            metadata = DocumentMetadata(
                sectionCount = sections.size,
                charCount = fullText.length
            ),
            extractionStatus = extractionStatus,
            warnings = warnings
        )
    }
}

/**
 * Markdown Document Reader preserving headings, lists, and hierarchy.
 */
class MarkdownDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.MARKDOWN

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(inputStream, options.charset))
        val textBuilder = StringBuilder()
        val sections = mutableListOf<DocumentSection>()

        var currentSectionTitle: String? = null
        var currentSectionLevel = 1
        val currentSectionLines = mutableListOf<String>()
        var lineCount = 0
        var wordCount = 0

        fun flushSection() {
            if (currentSectionLines.isNotEmpty()) {
                val content = currentSectionLines.joinToString("\n")
                sections.add(
                    DocumentSection(
                        title = currentSectionTitle ?: "Introduction",
                        content = content,
                        level = currentSectionLevel
                    )
                )
                currentSectionLines.clear()
            }
        }

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: break
            lineCount++
            wordCount += l.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
            textBuilder.appendLine(l)

            val headingMatch = Regex("""^(#{1,6})\s+(.*)$""").matchEntire(l.trim())
            if (headingMatch != null) {
                flushSection()
                currentSectionLevel = headingMatch.groupValues[1].length
                currentSectionTitle = headingMatch.groupValues[2].trim()
            } else {
                if (l.isNotBlank()) {
                    currentSectionLines.add(l)
                }
            }
            if (lineCount >= options.maxLines) break
        }
        flushSection()

        val fullText = textBuilder.toString()
        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "text/markdown",
            sizeBytes = sizeBytes,
            documentType = DocumentType.MARKDOWN,
            extractedText = fullText,
            sections = sections,
            pageOrSectionCount = maxOf(1, sections.size),
            metadata = DocumentMetadata(
                lineCount = lineCount,
                wordCount = wordCount,
                charCount = fullText.length,
                sectionCount = sections.size
            )
        )
    }
}

/**
 * Extensible PDF Document Reader abstraction.
 * Inspects PDF files safely, extracts text or metadata when readable without external binaries.
 */
class PdfDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.PDF

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        val header = String(bytes.take(10).toByteArray(), Charsets.US_ASCII)

        if (!header.startsWith("%PDF")) {
            return@withContext DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/pdf",
                sizeBytes = sizeBytes,
                documentType = DocumentType.PDF,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.FAILED,
                errors = listOf("Invalid PDF header signature.")
            )
        }

        // Basic structural text extraction for plain text streams in uncompressed PDF blocks
        val rawContent = String(bytes, Charsets.ISO_8859_1)
        val textRegex = Regex("""\(([^)]+)\)\s*Tj""")
        val matches = textRegex.findAll(rawContent).map { it.groupValues[1] }.toList()

        val extractedText = if (matches.isNotEmpty()) {
            matches.joinToString(" ")
        } else {
            "PDF Document: $fileName (Size: ${sizeBytes / 1024} KB). Embedded optical/vector content."
        }

        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "application/pdf",
            sizeBytes = sizeBytes,
            documentType = DocumentType.PDF,
            extractedText = extractedText,
            pageOrSectionCount = 1,
            metadata = DocumentMetadata(
                charCount = extractedText.length,
                custom = mapOf("pdf_version" to header.trim())
            ),
            warnings = if (matches.isEmpty()) listOf("No raw uncompressed text streams found; visual pages can be inspected via Vision OCR.") else emptyList()
        )
    }
}

/**
 * DOCX Document Reader reading XML from word/document.xml inside ZIP container.
 */
class DocxDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.DOCX

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val extractedTextBuilder = StringBuilder()
        val sections = mutableListOf<DocumentSection>()
        var wordDocumentFound = false

        try {
            java.util.zip.ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        wordDocumentFound = true
                        val xmlContent = zis.bufferedReader(Charsets.UTF_8).readText()
                        // Extract text nodes <w:t>...</w:t>
                        val tagRegex = Regex("""<w:t[^>]*>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL)
                        val matches = tagRegex.findAll(xmlContent).map { it.groupValues[1] }.toList()
                        extractedTextBuilder.append(matches.joinToString(" "))
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            return@withContext DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sizeBytes = sizeBytes,
                documentType = DocumentType.DOCX,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.FAILED,
                errors = listOf("Failed to parse DOCX: ${e.message}")
            )
        }

        val text = extractedTextBuilder.toString().trim()
        if (text.isNotBlank()) {
            sections.add(DocumentSection(title = "Document Body", content = text, level = 1))
        }

        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            sizeBytes = sizeBytes,
            documentType = DocumentType.DOCX,
            extractedText = text,
            sections = sections,
            pageOrSectionCount = maxOf(1, sections.size),
            metadata = DocumentMetadata(
                wordCount = text.split(Regex("""\s+""")).filter { it.isNotBlank() }.size,
                charCount = text.length
            )
        )
    }
}

/**
 * XLSX Document Reader reading cell values from xl/worksheets/sheet1.xml inside ZIP container.
 */
class XlsxDocumentReader : DocumentReader {
    override fun canRead(type: DocumentType): Boolean = type == DocumentType.XLSX

    override suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions
    ): DocumentModel = withContext(Dispatchers.IO) {
        val extractedTextBuilder = StringBuilder()
        val sections = mutableListOf<DocumentSection>()

        try {
            java.util.zip.ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/worksheets/sheet1.xml") {
                        val xmlContent = zis.bufferedReader(Charsets.UTF_8).readText()
                        // Extract inline strings <t>...</t> or numbers <v>...</v>
                        val textRegex = Regex("""<(?:t|v)[^>]*>(.*?)</(?:t|v)>""", RegexOption.DOT_MATCHES_ALL)
                        val cells = textRegex.findAll(xmlContent).map { it.groupValues[1] }.toList()
                        extractedTextBuilder.append(cells.joinToString(", "))
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            return@withContext DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                sizeBytes = sizeBytes,
                documentType = DocumentType.XLSX,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.FAILED,
                errors = listOf("Failed to parse XLSX: ${e.message}")
            )
        }

        val text = extractedTextBuilder.toString().trim()
        if (text.isNotBlank()) {
            sections.add(DocumentSection(title = "Sheet 1 Data", content = text, level = 1))
        }

        DocumentModel(
            fileName = fileName,
            mimeType = mimeType ?: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            sizeBytes = sizeBytes,
            documentType = DocumentType.XLSX,
            extractedText = text,
            sections = sections,
            pageOrSectionCount = 1,
            metadata = DocumentMetadata(
                charCount = text.length
            )
        )
    }
}

/**
 * Universal Document Reader: Validates sizes, types, dangerous extensions,
 * and delegates to the appropriate specialized reader.
 */
object UniversalDocumentReader {

    private val READERS = listOf(
        TxtDocumentReader(),
        CsvDocumentReader(),
        JsonDocumentReader(),
        XmlDocumentReader(),
        MarkdownDocumentReader(),
        PdfDocumentReader(),
        DocxDocumentReader(),
        XlsxDocumentReader()
    )

    private val DANGEROUS_EXTENSIONS = setOf(
        "apk", "dex", "so", "sh", "exe", "bat", "bin", "jar", "class", "cmd"
    )

    /**
     * Reads a document from an InputStream with safety checks.
     */
    suspend fun read(
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        options: DocumentReaderOptions = DocumentReaderOptions()
    ): DocumentModel {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

        // 1. Dangerous executable extension check
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            return DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                sizeBytes = sizeBytes,
                documentType = DocumentType.UNKNOWN,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.UNSUPPORTED,
                errors = listOf("File type .$ext is executable or binary and cannot be processed for safety reasons.")
            )
        }

        // 2. File size limit enforcement
        if (sizeBytes > options.maxSizeBytes) {
            return DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                sizeBytes = sizeBytes,
                documentType = DocumentType.fromFileNameAndMime(fileName, mimeType),
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.OVERSIZED,
                errors = listOf("Document size (${sizeBytes / 1024} KB) exceeds the maximum allowed limit of ${options.maxSizeBytes / 1024} KB.")
            )
        }

        val docType = DocumentType.fromFileNameAndMime(fileName, mimeType)
        if (docType == DocumentType.UNKNOWN) {
            return DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                sizeBytes = sizeBytes,
                documentType = DocumentType.UNKNOWN,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.UNSUPPORTED,
                errors = listOf("Unsupported document format: $fileName.")
            )
        }

        val reader = READERS.firstOrNull { it.canRead(docType) } ?: TxtDocumentReader()

        val model = try {
            reader.read(inputStream, fileName, mimeType, sizeBytes, options)
        } catch (e: Exception) {
            DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                sizeBytes = sizeBytes,
                documentType = docType,
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.FAILED,
                errors = listOf("Failed to read document: ${e.message ?: "Unknown I/O error"}")
            )
        }

        // Empty text check
        val finalModel = if (model.isSuccessful() && model.extractedText.isBlank()) {
            model.copy(extractionStatus = DocumentExtractionStatus.EMPTY)
        } else {
            model
        }

        // Sensitive data check
        val containsSensitive = SensitiveDataFilter.containsSensitiveData(finalModel.extractedText)
        val sensitiveTypes = mutableListOf<String>()
        if (containsSensitive) {
            if (finalModel.extractedText.contains(Regex("""(?i)\b(?:otp|code|verification)\b"""))) sensitiveTypes.add("OTP / Verification Code")
            if (finalModel.extractedText.contains(Regex("""(?i)\b(?:password|passwd|pin)\b"""))) sensitiveTypes.add("Password / PIN")
            if (finalModel.extractedText.contains(Regex("""\b(?:\d{4}[ -]?){3}\d{4}\b"""))) sensitiveTypes.add("Card Number")
            if (finalModel.extractedText.contains(Regex("""\b(?:AIza|sk-|Bearer)\b"""))) sensitiveTypes.add("API Key / Token")
        }

        return finalModel.copy(
            containsSensitiveData = containsSensitive,
            sensitiveDataTypes = sensitiveTypes
        )
    }

    /**
     * Reads a document from an Android content URI safely.
     */
    suspend fun readUri(
        context: Context,
        uri: Uri,
        options: DocumentReaderOptions = DocumentReaderOptions()
    ): DocumentModel {
        var fileName = "document"
        var mimeType: String? = null
        var sizeBytes = 0L

        try {
            mimeType = context.contentResolver.getType(uri)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            // Content provider query failed or restricted
        }

        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return DocumentModel(
                    fileName = fileName,
                    mimeType = mimeType ?: "application/octet-stream",
                    sizeBytes = sizeBytes,
                    documentType = DocumentType.UNKNOWN,
                    extractedText = "",
                    extractionStatus = DocumentExtractionStatus.FAILED,
                    errors = listOf("Unable to open input stream for URI: $uri")
                )

            stream.use { s ->
                read(s, fileName, mimeType, sizeBytes, options)
            }
        } catch (e: Exception) {
            DocumentModel(
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                sizeBytes = sizeBytes,
                documentType = DocumentType.fromFileNameAndMime(fileName, mimeType),
                extractedText = "",
                extractionStatus = DocumentExtractionStatus.FAILED,
                errors = listOf("Error opening document: ${e.message ?: "Access denied"}")
            )
        }
    }
}
