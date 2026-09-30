package com.example.jarvis.code

import java.util.Locale

/**
 * Conservative static language recognition without claiming compiler-level validation.
 */
object CodeLanguageDetector {

    /**
     * Detects code language from file name or raw code snippet content.
     */
    fun detectLanguage(fileName: String?, content: String): CodeLanguage {
        if (!fileName.isNullOrBlank()) {
            val lowerName = fileName.lowercase(Locale.ROOT)
            if (lowerName.endsWith(".gradle.kts") || lowerName.endsWith(".gradle")) {
                return CodeLanguage.GRADLE
            }
            val fromExt = CodeLanguage.fromExtension(fileName.substringAfterLast('.', ""))
            if (fromExt != CodeLanguage.UNKNOWN) {
                return fromExt
            }
        }

        val text = content.trim()
        if (text.isEmpty()) return CodeLanguage.UNKNOWN

        // Shebang check
        if (text.startsWith("#!/bin/bash") || text.startsWith("#!/bin/sh") || text.startsWith("#!/usr/bin/env bash")) {
            return CodeLanguage.SHELL
        }
        if (text.startsWith("#!/usr/bin/env python") || text.startsWith("#!/usr/bin/python")) {
            return CodeLanguage.PYTHON
        }

        // Structural formats: JSON, XML, HTML
        if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
            if (looksLikeJson(text)) return CodeLanguage.JSON
        }
        if (text.startsWith("<?xml") || (text.startsWith("<") && text.contains("</") && text.contains("xmlns"))) {
            return CodeLanguage.XML
        }
        if (text.contains("<!DOCTYPE html", ignoreCase = true) || (text.contains("<html", ignoreCase = true) && text.contains("</html>", ignoreCase = true))) {
            return CodeLanguage.HTML
        }

        // SQL recognition
        val sqlKeywords = listOf("SELECT ", "INSERT INTO ", "UPDATE ", "DELETE FROM ", "CREATE TABLE ", "ALTER TABLE ", "DROP TABLE ")
        if (sqlKeywords.any { text.startsWith(it, ignoreCase = true) || text.contains("\n$it", ignoreCase = true) }) {
            return CodeLanguage.SQL
        }

        // Gradle DSL recognition
        if (text.contains("plugins {") || text.contains("dependencies {") || text.contains("android {") || text.contains("implementation(")) {
            return CodeLanguage.GRADLE
        }

        // Markdown recognition
        if (text.startsWith("# ") || text.contains("\n## ") || (text.contains("```") && text.contains("\n- "))) {
            val mdIndicators = listOf("# ", "## ", "### ", "```", "* ", "- [ ]", "| --- |")
            val mdCount = mdIndicators.count { text.contains(it) }
            if (mdCount >= 2) return CodeLanguage.MARKDOWN
        }

        // Kotlin recognition: fun, val, var, data class, suspend, companion object, package
        val kotlinKeywords = listOf("fun ", "val ", "var ", "data class ", "suspend fun ", "companion object", "package com.", "override fun ")
        val kotlinMatches = kotlinKeywords.count { text.contains(it) }

        // Java recognition: public class, private void, System.out.println, implements, extends
        val javaKeywords = listOf("public class ", "public static void main", "System.out.print", "private final ", "protected ", "throws Exception")
        val javaMatches = javaKeywords.count { text.contains(it) }

        // Python recognition: def , elif , import numpy, if __name__ == '__main__':
        val pythonKeywords = listOf("def ", "elif ", "import ", "from ", "if __name__ == '__main__':", "self.", "print(", ":\n")
        val pythonMatches = pythonKeywords.count { text.contains(it) }

        // TypeScript / JavaScript recognition: const , let , function , console.log, export default, =>
        val jsKeywords = listOf("const ", "let ", "function ", "console.log(", "export default", "=>", "import {", "interface ", ": string", ": number")
        val jsMatches = jsKeywords.count { text.contains(it) }

        // CSS recognition
        if (text.contains("{") && text.contains("}") && (text.contains("margin:") || text.contains("padding:") || text.contains("color:") || text.contains("display:"))) {
            return CodeLanguage.CSS
        }

        // Compare match counts
        val scores = mapOf(
            CodeLanguage.KOTLIN to kotlinMatches,
            CodeLanguage.JAVA to javaMatches,
            CodeLanguage.PYTHON to pythonMatches,
            CodeLanguage.TYPESCRIPT to (if (text.contains("interface ") || text.contains(": string") || text.contains(": number")) jsMatches + 2 else 0),
            CodeLanguage.JAVASCRIPT to jsMatches
        )

        val best = scores.maxByOrNull { it.value }
        if (best != null && best.value > 0) {
            return best.key
        }

        return CodeLanguage.UNKNOWN
    }

    private fun looksLikeJson(text: String): Boolean {
        return (text.contains("\"") && (text.contains(":") || text.contains(",")))
    }
}
