package com.example.jarvis.generation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin PDF generator producing standard PDF 1.4 documents.
 * Supports headings, paragraphs, bullet lists, tables, multi-page layout, and UTF-8 / ASCII escaping.
 */
class PdfFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.PDF

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val pdfEngine = SimplePdfDocument()

        val title = request.title.ifBlank { request.fileName.substringBeforeLast(".") }
        pdfEngine.addTitle(title)

        if (request.sections.isNotEmpty()) {
            request.sections.forEach { section ->
                if (!section.title.isNullOrBlank()) {
                    pdfEngine.addHeading(section.title, level = section.level)
                }
                if (section.content.isNotBlank()) {
                    pdfEngine.addParagraph(section.content)
                }
                if (section.items.isNotEmpty()) {
                    section.items.forEach { item ->
                        pdfEngine.addBulletItem(item)
                    }
                }
            }
        } else if (request.rawContent.isNotBlank()) {
            pdfEngine.addParagraph(request.rawContent)
        }

        if (request.tables.isNotEmpty()) {
            request.tables.forEach { table ->
                if (!table.title.isNullOrBlank()) {
                    pdfEngine.addHeading(table.title, level = 2)
                }
                pdfEngine.addTable(table.headers, table.rows)
            }
        }

        pdfEngine.buildPdfBytes()
    }
}

/**
 * Internal helper for constructing multi-page PDF 1.4 binary documents.
 */
private class SimplePdfDocument {
    private val pageWidth = 595 // A4 width
    private val pageHeight = 842 // A4 height
    private val margin = 50
    private val contentWidth = pageWidth - 2 * margin

    private val pages = mutableListOf<PdfPageStream>()
    private var currentPage = PdfPageStream()

    private var currentY = pageHeight - margin

    init {
        pages.add(currentPage)
    }

    fun addTitle(title: String) {
        ensureSpace(36)
        currentPage.addText(
            text = escapePdfText(title),
            x = margin,
            y = currentY,
            fontSize = 18,
            isBold = true
        )
        currentY -= 28
    }

    fun addHeading(heading: String, level: Int) {
        val fontSize = if (level == 1) 14 else 12
        ensureSpace(28)
        currentY -= 6
        currentPage.addText(
            text = escapePdfText(heading),
            x = margin,
            y = currentY,
            fontSize = fontSize,
            isBold = true
        )
        currentY -= (fontSize + 6)
    }

    fun addParagraph(text: String) {
        val lines = wrapText(text, maxCharsPerLine = 75)
        lines.forEach { line ->
            ensureSpace(16)
            currentPage.addText(
                text = escapePdfText(line),
                x = margin,
                y = currentY,
                fontSize = 10,
                isBold = false
            )
            currentY -= 14
        }
        currentY -= 6
    }

    fun addBulletItem(item: String) {
        val lines = wrapText(item, maxCharsPerLine = 70)
        lines.forEachIndexed { idx, line ->
            ensureSpace(16)
            val prefix = if (idx == 0) "• " else "  "
            currentPage.addText(
                text = escapePdfText(prefix + line),
                x = margin + 10,
                y = currentY,
                fontSize = 10,
                isBold = false
            )
            currentY -= 14
        }
    }

    fun addTable(headers: List<String>, rows: List<List<String>>) {
        if (headers.isEmpty() && rows.isEmpty()) return

        val colCount = headers.size.coerceAtLeast(rows.maxOfOrNull { it.size } ?: 1)
        val colWidth = contentWidth / colCount
        val rowHeight = 18

        ensureSpace(rowHeight * (rows.size + 2))

        // Draw headers
        if (headers.isNotEmpty()) {
            currentPage.drawBox(margin, currentY - rowHeight, contentWidth, rowHeight, isFill = true)
            headers.forEachIndexed { idx, header ->
                val cellX = margin + (idx * colWidth) + 4
                currentPage.addText(
                    text = escapePdfText(header.take(20)),
                    x = cellX,
                    y = currentY - 13,
                    fontSize = 9,
                    isBold = true,
                    isWhiteText = true
                )
            }
            currentY -= rowHeight
        }

        // Draw rows
        rows.forEach { row ->
            ensureSpace(rowHeight)
            currentPage.drawBox(margin, currentY - rowHeight, contentWidth, rowHeight, isFill = false)
            for (colIdx in 0 until colCount) {
                val cellText = row.getOrElse(colIdx) { "" }
                val cellX = margin + (colIdx * colWidth) + 4
                currentPage.addText(
                    text = escapePdfText(cellText.take(20)),
                    x = cellX,
                    y = currentY - 13,
                    fontSize = 9,
                    isBold = false
                )
            }
            currentY -= rowHeight
        }
        currentY -= 10
    }

    private fun ensureSpace(neededHeight: Int) {
        if (currentY - neededHeight < margin) {
            currentPage = PdfPageStream()
            pages.add(currentPage)
            currentY = pageHeight - margin
        }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.length <= maxCharsPerLine) return listOf(text)
        val words = text.split(" ")
        val result = mutableListOf<String>()
        var currentLine = StringBuilder()

        words.forEach { word ->
            if (currentLine.length + word.length + 1 > maxCharsPerLine) {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
            }
            if (currentLine.isNotEmpty()) currentLine.append(" ")
            currentLine.append(word)
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }

    private fun escapePdfText(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\r", "")
            .replace("\n", " ")
            .filter { it.code in 32..126 || it == '•' }
            .replace("•", "*")
    }

    fun buildPdfBytes(): ByteArray {
        val bos = ByteArrayOutputStream()

        fun write(str: String) {
            bos.write(str.toByteArray(StandardCharsets.US_ASCII))
        }

        write("%PDF-1.4\n")
        write("%\u00e2\u00e3\u00cf\u00d3\n")

        val offsets = mutableListOf<Long>()

        // Object 1: Catalog
        offsets.add(bos.size().toLong())
        write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // Object 2: Pages Tree
        offsets.add(bos.size().toLong())
        val pageObjIds = pages.indices.map { 5 + it * 2 }
        val kidsArray = pageObjIds.joinToString(" ") { "$it 0 R" }
        write("2 0 obj\n<< /Type /Pages /Kids [ $kidsArray ] /Count ${pages.size} >>\nendobj\n")

        // Object 3: Font Regular (Helvetica)
        offsets.add(bos.size().toLong())
        write("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")

        // Object 4: Font Bold (Helvetica-Bold)
        offsets.add(bos.size().toLong())
        write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")

        // Objects for Pages & Page Streams
        pages.forEachIndexed { index, pageStream ->
            val pageObjId = 5 + index * 2
            val streamObjId = pageObjId + 1

            // Page Object
            offsets.add(bos.size().toLong())
            write("$pageObjId 0 obj\n")
            write("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] ")
            write("/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> ")
            write("/Contents $streamObjId 0 R >>\nendobj\n")

            // Stream Object
            val contentBytes = pageStream.getContentBytes()
            offsets.add(bos.size().toLong())
            write("$streamObjId 0 obj\n")
            write("<< /Length ${contentBytes.size} >>\nstream\n")
            bos.write(contentBytes)
            write("\nendstream\nendobj\n")
        }

        // Xref Table
        val startXref = bos.size().toLong()
        val totalObjs = 5 + pages.size * 2
        write("xref\n0 $totalObjs\n")
        write("0000000000 65535 f \n")
        offsets.forEach { offset ->
            write(String.format("%010d 00000 n \n", offset))
        }

        // Trailer
        write("trailer\n<< /Size $totalObjs /Root 1 0 R >>\n")
        write("startxref\n$startXref\n%%EOF\n")

        return bos.toByteArray()
    }
}

private class PdfPageStream {
    private val sb = StringBuilder()

    fun addText(text: String, x: Int, y: Int, fontSize: Int, isBold: Boolean, isWhiteText: Boolean = false) {
        val fontRef = if (isBold) "/F2" else "/F1"
        sb.append("BT\n")
        sb.append("$fontRef $fontSize Tf\n")
        if (isWhiteText) {
            sb.append("1 1 1 rg\n")
        } else {
            sb.append("0 0 0 rg\n")
        }
        sb.append("$x $y Td\n")
        sb.append("($text) Tj\n")
        sb.append("ET\n")
    }

    fun drawBox(x: Int, y: Int, width: Int, height: Int, isFill: Boolean) {
        if (isFill) {
            sb.append("q 0.2 0.3 0.5 rg $x $y $width $height re f Q\n")
        } else {
            sb.append("q 0.7 0.7 0.7 RG 0.5 w $x $y $width $height re S Q\n")
        }
    }

    fun getContentBytes(): ByteArray {
        return sb.toString().toByteArray(StandardCharsets.US_ASCII)
    }
}

/**
 * Pure Kotlin DOCX generator producing Microsoft Word (.docx) OpenXML documents.
 */
class DocxFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.DOCX

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(buildContentTypesXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(buildRootRelsXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(buildDocumentRelsXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(buildDocumentXml(request).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        bos.toByteArray()
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
    }

    private fun buildDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>""".trimIndent()
    }

    private fun buildDocumentXml(request: GenerationRequest): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        val title = request.title.ifBlank { request.fileName.substringBeforeLast(".") }
        sb.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr><w:t>")
        sb.append(escapeXml(title))
        sb.append("</w:t></w:r></w:p>")

        if (request.sections.isNotEmpty()) {
            request.sections.forEach { section ->
                if (!section.title.isNullOrBlank()) {
                    val sz = if (section.level == 1) "28" else "24"
                    sb.append("<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"$sz\"/></w:rPr><w:t>")
                    sb.append(escapeXml(section.title))
                    sb.append("</w:t></w:r></w:p>")
                }
                if (section.content.isNotBlank()) {
                    section.content.lines().forEach { line ->
                        sb.append("<w:p><w:r><w:t>")
                        sb.append(escapeXml(line))
                        sb.append("</w:t></w:r></w:p>")
                    }
                }
                if (section.items.isNotEmpty()) {
                    section.items.forEach { item ->
                        sb.append("<w:p><w:r><w:t>• ")
                        sb.append(escapeXml(item))
                        sb.append("</w:t></w:r></w:p>")
                    }
                }
            }
        } else if (request.rawContent.isNotBlank()) {
            request.rawContent.lines().forEach { line ->
                sb.append("<w:p><w:r><w:t>")
                sb.append(escapeXml(line))
                sb.append("</w:t></w:r></w:p>")
            }
        }

        if (request.tables.isNotEmpty()) {
            request.tables.forEach { table ->
                if (!table.title.isNullOrBlank()) {
                    sb.append("<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"24\"/></w:rPr><w:t>")
                    sb.append(escapeXml(table.title))
                    sb.append("</w:t></w:r></w:p>")
                }
                sb.append("<w:tbl>")
                if (table.headers.isNotEmpty()) {
                    sb.append("<w:tr>")
                    table.headers.forEach { h ->
                        sb.append("<w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>")
                        sb.append(escapeXml(h))
                        sb.append("</w:t></w:r></w:p></w:tc>")
                    }
                    sb.append("</w:tr>")
                }
                table.rows.forEach { row ->
                    sb.append("<w:tr>")
                    row.forEach { cell ->
                        sb.append("<w:tc><w:p><w:r><w:t>")
                        sb.append(escapeXml(cell))
                        sb.append("</w:t></w:r></w:p></w:tc>")
                    }
                    sb.append("</w:tr>")
                }
                sb.append("</w:tbl>")
            }
        }

        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

/**
 * Pure Kotlin XLSX generator producing Microsoft Excel (.xlsx) OpenXML spreadsheets.
 */
class XlsxFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.XLSX

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(buildContentTypesXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(buildRootRelsXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write(buildWorkbookRelsXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write(buildWorkbookXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. xl/worksheets/sheet1.xml
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(buildSheetXml(request).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        bos.toByteArray()
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".trimIndent()
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
    }

    private fun buildWorkbookRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".trimIndent()
    }

    private fun buildWorkbookXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Sheet1" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent()
    }

    private fun buildSheetXml(request: GenerationRequest): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")

        var rowIndex = 1

        if (request.tables.isNotEmpty()) {
            val table = request.tables.first()
            if (!table.title.isNullOrBlank()) {
                sb.append("<row r=\"$rowIndex\">")
                sb.append("<c r=\"A$rowIndex\" t=\"inlineStr\"><is><t>")
                sb.append(escapeXml(table.title))
                sb.append("</t></is></c></row>")
                rowIndex++
            }

            if (table.headers.isNotEmpty()) {
                sb.append("<row r=\"$rowIndex\">")
                table.headers.forEachIndexed { colIdx, header ->
                    val colRef = getColumnRef(colIdx)
                    sb.append("<c r=\"$colRef$rowIndex\" t=\"inlineStr\"><is><t>")
                    sb.append(escapeXml(header))
                    sb.append("</t></is></c>")
                }
                sb.append("</row>")
                rowIndex++
            }

            table.rows.forEach { row ->
                sb.append("<row r=\"$rowIndex\">")
                row.forEachIndexed { colIdx, cell ->
                    val colRef = getColumnRef(colIdx)
                    if (cell.toDoubleOrNull() != null) {
                        sb.append("<c r=\"$colRef$rowIndex\"><v>$cell</v></c>")
                    } else {
                        sb.append("<c r=\"$colRef$rowIndex\" t=\"inlineStr\"><is><t>")
                        sb.append(escapeXml(cell))
                        sb.append("</t></is></c>")
                    }
                }
                sb.append("</row>")
                rowIndex++
            }
        } else if (request.rawContent.isNotBlank()) {
            request.rawContent.lines().forEach { line ->
                sb.append("<row r=\"$rowIndex\">")
                val cells = line.split(",", "\t", ";")
                cells.forEachIndexed { colIdx, cell ->
                    val colRef = getColumnRef(colIdx)
                    sb.append("<c r=\"$colRef$rowIndex\" t=\"inlineStr\"><is><t>")
                    sb.append(escapeXml(cell.trim()))
                    sb.append("</t></is></c>")
                }
                sb.append("</row>")
                rowIndex++
            }
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun getColumnRef(colIdx: Int): String {
        var n = colIdx
        val result = StringBuilder()
        while (n >= 0) {
            result.insert(0, ('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return result.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
