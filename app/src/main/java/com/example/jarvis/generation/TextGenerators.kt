package com.example.jarvis.generation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Generates clean, well-formatted plain text (.txt).
 */
class TxtFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.TXT

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        if (request.title.isNotBlank()) {
            sb.appendLine(request.title.uppercase())
            sb.appendLine("=".repeat(request.title.length.coerceAtLeast(10)))
            sb.appendLine()
        }

        if (request.sections.isNotEmpty()) {
            request.sections.forEach { section ->
                if (!section.title.isNullOrBlank()) {
                    sb.appendLine(section.title)
                    sb.appendLine("-".repeat(section.title.length.coerceAtLeast(6)))
                }
                if (section.content.isNotBlank()) {
                    sb.appendLine(section.content)
                }
                if (section.items.isNotEmpty()) {
                    section.items.forEach { item ->
                        sb.appendLine("  • $item")
                    }
                }
                sb.appendLine()
            }
        } else if (request.rawContent.isNotBlank()) {
            sb.appendLine(request.rawContent)
        }

        if (request.tables.isNotEmpty()) {
            request.tables.forEach { table ->
                if (!table.title.isNullOrBlank()) {
                    sb.appendLine("[Table: ${table.title}]")
                }
                if (table.headers.isNotEmpty()) {
                    sb.appendLine(table.headers.joinToString(" | "))
                    sb.appendLine(table.headers.joinToString(" | ") { "-".repeat(it.length.coerceAtLeast(3)) })
                }
                table.rows.forEach { row ->
                    sb.appendLine(row.joinToString(" | "))
                }
                sb.appendLine()
            }
        }

        sb.toString().trimEnd().toByteArray(Charsets.UTF_8)
    }
}

/**
 * Generates standard Markdown (.md) preserving headings, lists, code blocks, and tables.
 */
class MarkdownFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.MARKDOWN

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val sb = StringBuilder()

        if (request.title.isNotBlank()) {
            sb.appendLine("# ${request.title}")
            sb.appendLine()
        }

        if (request.sections.isNotEmpty()) {
            request.sections.forEach { section ->
                if (!section.title.isNullOrBlank()) {
                    val hashes = "#".repeat((section.level + 1).coerceIn(2, 6))
                    sb.appendLine("$hashes ${section.title}")
                    sb.appendLine()
                }
                if (section.isCodeBlock) {
                    val lang = section.codeLanguage ?: ""
                    sb.appendLine("```$lang")
                    sb.appendLine(section.content)
                    sb.appendLine("```")
                    sb.appendLine()
                } else if (section.content.isNotBlank()) {
                    sb.appendLine(section.content)
                    sb.appendLine()
                }
                if (section.items.isNotEmpty()) {
                    section.items.forEach { item ->
                        sb.appendLine("- $item")
                    }
                    sb.appendLine()
                }
            }
        } else if (request.rawContent.isNotBlank()) {
            sb.appendLine(request.rawContent)
            sb.appendLine()
        }

        if (request.tables.isNotEmpty()) {
            request.tables.forEach { table ->
                if (!table.title.isNullOrBlank()) {
                    sb.appendLine("### ${table.title}")
                    sb.appendLine()
                }
                if (table.headers.isNotEmpty()) {
                    sb.appendLine("| " + table.headers.joinToString(" | ") + " |")
                    sb.appendLine("| " + table.headers.joinToString(" | ") { "---" } + " |")
                    table.rows.forEach { row ->
                        val paddedRow = List(table.headers.size) { idx ->
                            row.getOrElse(idx) { "" }.replace("|", "\\|")
                        }
                        sb.appendLine("| " + paddedRow.joinToString(" | ") + " |")
                    }
                    sb.appendLine()
                }
            }
        }

        sb.toString().trimEnd().toByteArray(Charsets.UTF_8)
    }
}

/**
 * Generates valid JSON with strict UTF-8 escaping and pretty-print options.
 */
class JsonFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.JSON

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val indent = if (request.prettyPrintJson) 2 else 0

        val jsonString: String = when (val obj = request.jsonObject) {
            is JSONObject -> obj.toString(indent)
            is JSONArray -> obj.toString(indent)
            is Map<*, *> -> JSONObject(obj).toString(indent)
            is List<*> -> JSONArray(obj).toString(indent)
            is String -> {
                // Parse to validate and format
                val trimmed = obj.trim()
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    val tokener = JSONTokener(trimmed).nextValue()
                    when (tokener) {
                        is JSONObject -> tokener.toString(indent)
                        is JSONArray -> tokener.toString(indent)
                        else -> {
                            val wrapper = JSONObject().put("content", trimmed)
                            wrapper.toString(indent)
                        }
                    }
                } else {
                    // Structure the raw string
                    val wrapper = JSONObject()
                    if (request.title.isNotBlank()) wrapper.put("title", request.title)
                    wrapper.put("content", trimmed)
                    wrapper.toString(indent)
                }
            }
            null -> {
                // Build JSON from request sections and tables
                val root = JSONObject()
                if (request.title.isNotBlank()) root.put("title", request.title)
                if (request.rawContent.isNotBlank()) root.put("content", request.rawContent)
                if (request.sections.isNotEmpty()) {
                    val sectionsArray = JSONArray()
                    request.sections.forEach { s ->
                        val sObj = JSONObject()
                        if (s.title != null) sObj.put("title", s.title)
                        if (s.content.isNotBlank()) sObj.put("content", s.content)
                        if (s.items.isNotEmpty()) sObj.put("items", JSONArray(s.items))
                        sectionsArray.put(sObj)
                    }
                    root.put("sections", sectionsArray)
                }
                if (request.tables.isNotEmpty()) {
                    val tablesArray = JSONArray()
                    request.tables.forEach { t ->
                        val tObj = JSONObject()
                        if (t.title != null) tObj.put("title", t.title)
                        tObj.put("headers", JSONArray(t.headers))
                        val rowsArray = JSONArray()
                        t.rows.forEach { r -> rowsArray.put(JSONArray(r)) }
                        tObj.put("rows", rowsArray)
                        tablesArray.put(tObj)
                    }
                    root.put("tables", tablesArray)
                }
                root.toString(indent)
            }
            else -> {
                val wrapper = JSONObject().put("value", obj.toString())
                wrapper.toString(indent)
            }
        }

        jsonString.toByteArray(Charsets.UTF_8)
    }
}

/**
 * Generates RFC-4180 compliant CSV files with robust quoting and escaping.
 */
class CsvFileGenerator : FileGenerator {
    override val format: GeneratedFileFormat = GeneratedFileFormat.CSV

    override suspend fun generate(request: GenerationRequest): ByteArray = withContext(Dispatchers.Default) {
        val sb = StringBuilder()

        if (request.tables.isNotEmpty()) {
            val table = request.tables.first()
            val colCount = table.headers.size.coerceAtLeast(
                table.rows.maxOfOrNull { it.size } ?: 0
            )

            if (table.headers.isNotEmpty()) {
                val paddedHeaders = List(colCount) { idx -> table.headers.getOrElse(idx) { "Column_${idx + 1}" } }
                sb.appendLine(paddedHeaders.joinToString(",") { escapeCsvCell(it) })
            }

            table.rows.forEach { row ->
                val paddedRow = List(colCount) { idx -> row.getOrElse(idx) { "" } }
                sb.appendLine(paddedRow.joinToString(",") { escapeCsvCell(it) })
            }
        } else if (request.rawContent.isNotBlank()) {
            // Check if rawContent is already delimited, or format it line by line
            val lines = request.rawContent.lines().filter { it.isNotBlank() }
            lines.forEach { line ->
                if (line.contains(",") || line.contains("\t") || line.contains(";")) {
                    // Normalize delimiter
                    val cells = parseRawLineToCells(line)
                    sb.appendLine(cells.joinToString(",") { escapeCsvCell(it) })
                } else {
                    sb.appendLine(escapeCsvCell(line))
                }
            }
        } else if (request.sections.isNotEmpty()) {
            sb.appendLine("Section,Content")
            request.sections.forEach { s ->
                val title = s.title ?: "General"
                val body = s.content.ifBlank { s.items.joinToString("; ") }
                sb.appendLine("${escapeCsvCell(title)},${escapeCsvCell(body)}")
            }
        }

        sb.toString().trimEnd().toByteArray(Charsets.UTF_8)
    }

    private fun parseRawLineToCells(line: String): List<String> {
        val delim = when {
            line.contains("\t") -> "\t"
            line.contains(";") -> ";"
            line.contains("|") -> "|"
            else -> ","
        }
        return line.split(delim).map { it.trim() }
    }

    private fun escapeCsvCell(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
