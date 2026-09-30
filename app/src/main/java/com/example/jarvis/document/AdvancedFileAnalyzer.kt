package com.example.jarvis.document

import com.example.jarvis.extraction.ExtractionSource
import com.example.jarvis.extraction.InformationExtractionEngine
import com.example.jarvis.security.SensitiveDataFilter
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * High-performance, zero-trust analyzer for document structure and data.
 * Adheres strictly to security boundaries: treats file contents purely as passive data,
 * never executes embedded commands, uses existing InformationExtractionEngine and SensitiveDataFilter,
 * and handles malformed or oversized inputs gracefully.
 */
object AdvancedFileAnalyzer {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
        SimpleDateFormat("yyyy/MM/dd", Locale.ROOT),
        SimpleDateFormat("MM/dd/yyyy", Locale.ROOT),
        SimpleDateFormat("dd-MM-yyyy", Locale.ROOT),
        SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    ).onEach { it.isLenient = false }

    /**
     * Primary entry point: converts a DocumentModel into an in-depth FileAnalysisResult.
     */
    fun analyze(document: DocumentModel): FileAnalysisResult {
        val detectedType = document.documentType
        val fileSize = document.sizeBytes
        val hasContent = document.extractedText.isNotBlank()

        // 1. Handle error or non-content statuses immediately
        when (document.extractionStatus) {
            DocumentExtractionStatus.OVERSIZED -> {
                return FileAnalysisResult(
                    fileName = document.fileName,
                    detectedType = detectedType,
                    mimeType = document.mimeType,
                    fileSize = fileSize,
                    hasContent = false,
                    status = FileAnalysisStatus.OVERSIZED,
                    structureInfo = FileStructureInfo(),
                    errors = document.errors.ifEmpty { listOf("File exceeds safe processing size threshold.") }
                )
            }
            DocumentExtractionStatus.UNSUPPORTED -> {
                return FileAnalysisResult(
                    fileName = document.fileName,
                    detectedType = detectedType,
                    mimeType = document.mimeType,
                    fileSize = fileSize,
                    hasContent = false,
                    status = FileAnalysisStatus.UNSUPPORTED,
                    structureInfo = FileStructureInfo(),
                    errors = document.errors.ifEmpty { listOf("Unsupported file format.") }
                )
            }
            DocumentExtractionStatus.FAILED -> {
                return FileAnalysisResult(
                    fileName = document.fileName,
                    detectedType = detectedType,
                    mimeType = document.mimeType,
                    fileSize = fileSize,
                    hasContent = false,
                    status = FileAnalysisStatus.FAILED,
                    structureInfo = FileStructureInfo(),
                    errors = document.errors.ifEmpty { listOf("Failed to read file content.") }
                )
            }
            DocumentExtractionStatus.EMPTY -> {
                return FileAnalysisResult(
                    fileName = document.fileName,
                    detectedType = detectedType,
                    mimeType = document.mimeType,
                    fileSize = fileSize,
                    hasContent = false,
                    status = FileAnalysisStatus.EMPTY,
                    structureInfo = FileStructureInfo()
                )
            }
            else -> {}
        }

        if (!hasContent) {
            return FileAnalysisResult(
                fileName = document.fileName,
                detectedType = detectedType,
                mimeType = document.mimeType,
                fileSize = fileSize,
                hasContent = false,
                status = FileAnalysisStatus.EMPTY,
                structureInfo = FileStructureInfo()
            )
        }

        // 2. Sensitive data detection via SensitiveDataFilter
        val text = document.extractedText
        val containsSensitive = document.containsSensitiveData || SensitiveDataFilter.containsSensitiveData(text)
        val sensitiveTypes = (document.sensitiveDataTypes + detectSensitiveCategories(text)).distinct()

        // 3. Information Extraction integration
        val extractedData = InformationExtractionEngine.extract(
            text = text,
            source = ExtractionSource.DOCUMENT
        )

        val warnings = mutableListOf<String>().apply { addAll(document.warnings) }
        val errors = mutableListOf<String>().apply { addAll(document.errors) }

        var tabularAnalysis: TabularAnalysis? = null
        var structuredAnalysis: StructuredDataAnalysis? = null
        var textAnalysis: TextAnalysis? = null
        val statistics = mutableMapOf<String, Any>()

        // 4. Format-specific deep inspection
        when (detectedType) {
            DocumentType.CSV -> {
                val csvResult = analyzeCsv(document, warnings)
                tabularAnalysis = csvResult
                statistics["rowCount"] = csvResult.rowCount
                statistics["columnCount"] = csvResult.columnCount
                statistics["missingValues"] = csvResult.totalMissingValues
                statistics["duplicateRows"] = csvResult.duplicateRowCount
            }
            DocumentType.JSON -> {
                structuredAnalysis = analyzeJson(document.rawContent ?: text, warnings, errors)
                if (structuredAnalysis != null) {
                    statistics["topLevelType"] = structuredAnalysis.topLevelType
                    statistics["maxDepth"] = structuredAnalysis.maxDepth
                    statistics["totalKeys"] = structuredAnalysis.totalKeyCount
                    statistics["totalElements"] = structuredAnalysis.totalElementCount
                }
            }
            DocumentType.XML -> {
                structuredAnalysis = analyzeXml(document.rawContent ?: text, warnings, errors)
                if (structuredAnalysis != null) {
                    statistics["topLevelType"] = structuredAnalysis.topLevelType
                    statistics["maxDepth"] = structuredAnalysis.maxDepth
                    statistics["totalElements"] = structuredAnalysis.totalElementCount
                }
            }
            DocumentType.MARKDOWN, DocumentType.TXT, DocumentType.PDF, DocumentType.DOCX, DocumentType.XLSX -> {
                textAnalysis = analyzeTextContent(document, extractedData)
                statistics["wordCount"] = textAnalysis.wordCount
                statistics["charCount"] = textAnalysis.charCount
                statistics["paragraphCount"] = textAnalysis.paragraphCount
                statistics["dateCount"] = textAnalysis.importantDates.size
                statistics["amountCount"] = textAnalysis.importantAmounts.size
                statistics["emailCount"] = textAnalysis.emails.size
            }
            DocumentType.UNKNOWN -> {
                warnings.add("Unknown document format; performed general textual analysis.")
                textAnalysis = analyzeTextContent(document, extractedData)
            }
        }

        // 5. Structure Info
        val lineCount = maxOf(document.metadata.lineCount, text.lines().size)
        val wordCount = if (textAnalysis != null) textAnalysis.wordCount else text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }
        val charCount = text.length
        val sectionCount = maxOf(document.sections.size, tabularAnalysis?.rowCount ?: 0)

        val structureInfo = FileStructureInfo(
            lineCount = lineCount,
            charCount = charCount,
            wordCount = wordCount,
            sectionCount = sectionCount,
            maxDepth = structuredAnalysis?.maxDepth ?: if (tabularAnalysis != null) 2 else 1,
            topLevelContainer = when (detectedType) {
                DocumentType.CSV -> "TABLE"
                DocumentType.JSON -> structuredAnalysis?.topLevelType ?: "JSON"
                DocumentType.XML -> "XML_DOCUMENT"
                DocumentType.MARKDOWN -> "MARKDOWN_DOCUMENT"
                DocumentType.PDF -> "PDF_DOCUMENT"
                else -> "TEXT_DOCUMENT"
            },
            primaryAttributes = mapOf(
                "fileName" to document.fileName,
                "mimeType" to document.mimeType,
                "type" to detectedType.name
            )
        )

        val status = if (errors.isNotEmpty()) {
            FileAnalysisStatus.PARTIAL
        } else {
            FileAnalysisStatus.SUCCESS
        }

        return FileAnalysisResult(
            fileName = document.fileName,
            detectedType = detectedType,
            mimeType = document.mimeType,
            fileSize = fileSize,
            hasContent = true,
            status = status,
            structureInfo = structureInfo,
            extractedEntities = extractedData.entities,
            tabularData = tabularAnalysis,
            structuredData = structuredAnalysis,
            textData = textAnalysis,
            statistics = statistics,
            warnings = warnings,
            errors = errors,
            containsSensitiveData = containsSensitive,
            sensitiveDataTypes = sensitiveTypes
        )
    }

    // ==========================================
    // CSV / TABULAR ANALYSIS
    // ==========================================

    private fun analyzeCsv(document: DocumentModel, warnings: MutableList<String>): TabularAnalysis {
        val lines = document.extractedText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return TabularAnalysis(0, 0, emptyList(), 0, 0)
        }

        // If headers are already in metadata
        val columnNames = if (document.metadata.columnNames.isNotEmpty()) {
            document.metadata.columnNames
        } else {
            parseCsvLine(lines.first())
        }

        val columnCount = columnNames.size
        val dataRows = mutableListOf<List<String>>()

        // Extract tabular rows
        val rawDataLines: List<List<String>> = if (document.metadata.columnNames.isNotEmpty() && !lines.first().startsWith("CSV Table:")) {
            lines.map { parseCsvLine(it) }
        } else {
            // Check if document was serialized with "CSV Table:" or "[Row X]"
            val rowLines = lines.filter { it.startsWith("[Row ") }
            if (rowLines.isNotEmpty()) {
                rowLines.map { rowLine ->
                    val payload = rowLine.substringAfter("] ")
                    val parts = payload.split(" | ")
                    val map = parts.associate {
                        val k = it.substringBefore(": ").trim()
                        val v = it.substringAfter(": ").trim()
                        k to v
                    }
                    columnNames.map { col -> map[col] ?: "" }
                }
            } else {
                lines.drop(1).map { parseCsvLine(it) }
            }
        }

        // Bound to safety limits (avoid OOM on massive tables)
        val boundedRows = rawDataLines.take(5000)
        if (rawDataLines.size > 5000) {
            warnings.add("Table exceeds 5,000 rows; statistics bounded to the first 5,000 rows.")
        }

        var totalMissingValues = 0
        val duplicateRowIndices = mutableListOf<Int>()
        val seenRowSignatures = mutableMapOf<String, Int>()

        // Check duplicate rows
        boundedRows.forEachIndexed { index, row ->
            val signature = row.joinToString("||") { it.trim().lowercase(Locale.ROOT) }
            if (seenRowSignatures.containsKey(signature)) {
                duplicateRowIndices.add(index + 1)
            } else {
                seenRowSignatures[signature] = index + 1
            }
        }

        // Column-by-column statistics and type inference
        val columnAnalyses = columnNames.mapIndexed { colIndex, colName ->
            val values = boundedRows.map { row ->
                row.getOrNull(colIndex)?.trim() ?: ""
            }

            var missingCount = 0
            val nonNullValues = mutableListOf<String>()
            val sampleValues = mutableListOf<String>()

            values.forEach { v ->
                if (isMissingValue(v)) {
                    missingCount++
                    totalMissingValues++
                } else {
                    nonNullValues.add(v)
                    if (sampleValues.size < 5) sampleValues.add(v)
                }
            }

            val uniqueCount = nonNullValues.distinct().size
            val inferredType = inferColumnDataType(nonNullValues)
            val numericStats = if (inferredType == InferredDataType.INTEGER || inferredType == InferredDataType.DECIMAL) {
                calculateNumericStats(nonNullValues)
            } else null

            ColumnAnalysis(
                index = colIndex,
                name = colName,
                inferredType = inferredType,
                totalCount = values.size,
                nonNullCount = nonNullValues.size,
                missingCount = missingCount,
                uniqueCount = uniqueCount,
                sampleValues = sampleValues,
                numericStats = numericStats
            )
        }

        val previewRows = boundedRows.take(10).map { row ->
            columnNames.mapIndexed { i, col ->
                col to (row.getOrNull(i)?.trim() ?: "")
            }.toMap()
        }

        return TabularAnalysis(
            rowCount = boundedRows.size,
            columnCount = columnCount,
            columns = columnAnalyses,
            totalMissingValues = totalMissingValues,
            duplicateRowCount = duplicateRowIndices.size,
            duplicateRowIndices = duplicateRowIndices,
            previewRows = previewRows
        )
    }

    private fun isMissingValue(value: String): Boolean {
        if (value.isBlank()) return true
        val lower = value.lowercase(Locale.ROOT)
        return lower == "null" || lower == "n/a" || lower == "na" || lower == "none" || lower == "nan" || lower == "-"
    }

    private fun inferColumnDataType(values: List<String>): InferredDataType {
        if (values.isEmpty()) return InferredDataType.TEXT

        var intCount = 0
        var decimalCount = 0
        var boolCount = 0
        var dateCount = 0

        values.forEach { v ->
            val clean = v.replace(",", "").replace("$", "").replace("€", "").replace("£", "").trim()
            if (clean.toLongOrNull() != null) {
                intCount++
            } else if (clean.toDoubleOrNull() != null) {
                decimalCount++
            } else if (v.equals("true", true) || v.equals("false", true) || v.equals("yes", true) || v.equals("no", true)) {
                boolCount++
            } else if (isDate(v)) {
                dateCount++
            }
        }

        val total = values.size
        return when {
            intCount == total -> InferredDataType.INTEGER
            (intCount + decimalCount) == total -> InferredDataType.DECIMAL
            boolCount == total -> InferredDataType.BOOLEAN
            dateCount == total -> InferredDataType.DATE
            (intCount + decimalCount) > (total * 0.8) -> InferredDataType.DECIMAL
            else -> InferredDataType.TEXT
        }
    }

    private fun isDate(value: String): Boolean {
        if (value.length < 8 || value.length > 30) return false
        for (format in DATE_FORMATS) {
            try {
                format.parse(value)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    private fun calculateNumericStats(values: List<String>): NumericStats? {
        val numbers = values.mapNotNull { v ->
            val clean = v.replace(",", "").replace("$", "").replace("€", "").replace("£", "").trim()
            clean.toDoubleOrNull()
        }
        if (numbers.isEmpty()) return null

        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        var sum = 0.0

        for (n in numbers) {
            if (n < min) min = n
            if (n > max) max = n
            sum += n
        }

        val average = sum / numbers.size
        return NumericStats(
            count = numbers.size,
            min = min,
            max = max,
            sum = sum,
            average = average
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
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

    // ==========================================
    // JSON STRUCTURE ANALYSIS
    // ==========================================

    private fun analyzeJson(
        rawJson: String,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ): StructuredDataAnalysis? {
        return try {
            val tokener = JSONTokener(rawJson)
            val root = tokener.nextValue()

            var maxDepth = 0
            var totalKeyCount = 0
            var totalElementCount = 0
            var arrayCount = 0
            val primitiveTypeCounts = mutableMapOf<InferredDataType, Int>()
            val topLevelKeys = mutableListOf<String>()
            val keyPaths = mutableListOf<String>()

            fun traverse(node: Any?, depth: Int, path: String) {
                if (depth > maxDepth) maxDepth = depth
                totalElementCount++

                when (node) {
                    is JSONObject -> {
                        val keys = node.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            totalKeyCount++
                            if (depth == 1) topLevelKeys.add(key)
                            val subPath = if (path.isEmpty()) key else "$path.$key"
                            if (keyPaths.size < 100) keyPaths.add(subPath)
                            traverse(node.opt(key), depth + 1, subPath)
                        }
                    }
                    is JSONArray -> {
                        arrayCount++
                        for (i in 0 until node.length()) {
                            val subPath = "$path[$i]"
                            traverse(node.opt(i), depth + 1, subPath)
                        }
                    }
                    is String -> {
                        primitiveTypeCounts[InferredDataType.TEXT] = (primitiveTypeCounts[InferredDataType.TEXT] ?: 0) + 1
                    }
                    is Number -> {
                        val type = if (node is Int || node is Long) InferredDataType.INTEGER else InferredDataType.DECIMAL
                        primitiveTypeCounts[type] = (primitiveTypeCounts[type] ?: 0) + 1
                    }
                    is Boolean -> {
                        primitiveTypeCounts[InferredDataType.BOOLEAN] = (primitiveTypeCounts[InferredDataType.BOOLEAN] ?: 0) + 1
                    }
                    null, JSONObject.NULL -> {
                        primitiveTypeCounts[InferredDataType.NULL] = (primitiveTypeCounts[InferredDataType.NULL] ?: 0) + 1
                    }
                    else -> {
                        primitiveTypeCounts[InferredDataType.TEXT] = (primitiveTypeCounts[InferredDataType.TEXT] ?: 0) + 1
                    }
                }
            }

            val topLevelType = when (root) {
                is JSONObject -> "OBJECT"
                is JSONArray -> "ARRAY"
                else -> "PRIMITIVE"
            }

            traverse(root, 1, "")

            StructuredDataAnalysis(
                topLevelType = topLevelType,
                maxDepth = maxDepth,
                totalKeyCount = totalKeyCount,
                totalElementCount = totalElementCount,
                arrayCount = arrayCount,
                primitiveTypeCounts = primitiveTypeCounts,
                topLevelKeys = topLevelKeys,
                keyPaths = keyPaths
            )
        } catch (e: Exception) {
            warnings.add("JSON is partially malformed: ${e.message}")
            StructuredDataAnalysis(
                topLevelType = "MALFORMED_JSON",
                maxDepth = 0,
                totalKeyCount = 0,
                totalElementCount = 0,
                arrayCount = 0
            )
        }
    }

    // ==========================================
    // XML STRUCTURE ANALYSIS
    // ==========================================

    private fun analyzeXml(
        rawXml: String,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ): StructuredDataAnalysis? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(rawXml))

            var maxDepth = 0
            var currentDepth = 0
            var elementCount = 0
            var totalKeyCount = 0
            var rootTag: String? = null
            val topLevelKeys = mutableListOf<String>()
            val keyPaths = mutableListOf<String>()
            val tagStack = ArrayDeque<String>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentDepth++
                        if (currentDepth > maxDepth) maxDepth = currentDepth
                        elementCount++
                        val tagName = parser.name ?: "tag"
                        tagStack.addLast(tagName)

                        if (rootTag == null) {
                            rootTag = tagName
                        } else if (currentDepth == 2) {
                            topLevelKeys.add(tagName)
                        }

                        val path = tagStack.joinToString("/")
                        if (keyPaths.size < 100) keyPaths.add(path)

                        // Inspect attributes as keys
                        val attrCount = parser.attributeCount
                        totalKeyCount += attrCount + 1
                    }
                    XmlPullParser.END_TAG -> {
                        currentDepth--
                        if (tagStack.isNotEmpty()) tagStack.removeLast()
                    }
                }
                eventType = parser.next()
            }

            StructuredDataAnalysis(
                topLevelType = rootTag ?: "XML",
                maxDepth = maxDepth,
                totalKeyCount = totalKeyCount,
                totalElementCount = elementCount,
                arrayCount = 0,
                topLevelKeys = topLevelKeys.distinct(),
                keyPaths = keyPaths
            )
        } catch (e: Exception) {
            warnings.add("XML parsing warning: ${e.message}")
            StructuredDataAnalysis(
                topLevelType = "MALFORMED_XML",
                maxDepth = 0,
                totalKeyCount = 0,
                totalElementCount = 0,
                arrayCount = 0
            )
        }
    }

    // ==========================================
    // TEXT & DOCUMENT CONTENT ANALYSIS
    // ==========================================

    private fun analyzeTextContent(
        document: DocumentModel,
        extractedData: com.example.jarvis.extraction.ExtractedData
    ): TextAnalysis {
        val text = document.extractedText
        val lines = text.lines()
        val lineCount = lines.size
        val charCount = text.length
        val wordCount = text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }

        // Paragraph detection: blocks separated by empty lines
        val paragraphCount = text.split(Regex("""\n\s*\n""")).count { it.isNotBlank() }

        // Headings detection (from Markdown '#' or document sections)
        val headings = mutableListOf<String>()
        document.sections.mapNotNull { it.title }.forEach { headings.add(it) }
        lines.filter { it.trim().startsWith("#") }.forEach { line ->
            val h = line.trim().removePrefix("#").trim()
            if (h.isNotBlank() && !headings.contains(h)) {
                headings.add(h)
            }
        }

        val emails = extractedData.emails.map { it.email }.distinct()
        val phoneNumbers = extractedData.phoneNumbers.map { it.phoneNumber }.distinct()
        val urls = extractedData.urls.map { it.url }.distinct()

        val keyPoints = mutableListOf<String>()
        if (headings.isNotEmpty()) {
            keyPoints.add("Headings: ${headings.take(4).joinToString(", ")}")
        }
        if (extractedData.people.isNotEmpty()) {
            keyPoints.add("Referenced People: ${extractedData.people.take(4).joinToString { it.name }}")
        }
        if (extractedData.organizations.isNotEmpty()) {
            keyPoints.add("Organizations: ${extractedData.organizations.take(4).joinToString { it.name }}")
        }
        if (extractedData.currencies.isNotEmpty()) {
            keyPoints.add("Amounts: ${extractedData.currencies.take(4).joinToString { "${it.currencySymbol}${it.amount}" }}")
        }
        if (extractedData.dates.isNotEmpty()) {
            keyPoints.add("Dates: ${extractedData.dates.take(4).joinToString { it.normalizedIso ?: it.rawText }}")
        }

        return TextAnalysis(
            lineCount = lineCount,
            wordCount = wordCount,
            charCount = charCount,
            paragraphCount = paragraphCount,
            headings = headings,
            importantDates = extractedData.dates,
            importantAmounts = extractedData.currencies,
            emails = emails,
            phoneNumbers = phoneNumbers,
            urls = urls,
            keyPoints = keyPoints
        )
    }

    private fun detectSensitiveCategories(text: String): List<String> {
        val types = mutableListOf<String>()
        if (text.contains(Regex("""(?i)\b(?:otp|verification\s*code|security\s*code)\b"""))) types.add("OTP / Verification Code")
        if (text.contains(Regex("""(?i)\b(?:password|passwd|pin|secret\s*key)\b"""))) types.add("Password / Credential")
        if (text.contains(Regex("""\b(?:\d{4}[ -]?){3}\d{4}\b"""))) types.add("Card Number")
        if (text.contains(Regex("""\b(?:sk-[a-zA-Z0-9_-]{16,}|AIza[a-zA-Z0-9_-]{20,})\b"""))) types.add("API Key")
        return types
    }
}
