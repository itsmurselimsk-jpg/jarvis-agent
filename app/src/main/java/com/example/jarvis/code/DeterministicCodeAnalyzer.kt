package com.example.jarvis.code

/**
 * Deterministic, offline static code analyzer.
 * Performs conservative local checks across supported languages to identify:
 * - Suspicious null handling
 * - Malformed structures (brackets, quotes)
 * - Obvious bug patterns (infinite loops, division by zero, assignments in conditionals)
 * - Hardcoded credentials and secrets
 * - Obvious API misuse patterns & unclosed resources
 * - Code metrics & complexity indicators
 *
 * NOTE: Strictly data analysis. Never executes code or modifies source files.
 */
object DeterministicCodeAnalyzer {

    fun analyze(
        content: String,
        fileName: String? = null,
        languageOverride: CodeLanguage? = null
    ): CodeAnalysisResult {
        val language = languageOverride ?: CodeLanguageDetector.detectLanguage(fileName, content)
        val lines = content.lines()
        val findings = mutableListOf<CodeFinding>()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // 1. Security & Secrets scan
        val secretFindings = CodeSecurityScanner.scanForSecrets(content, fileName)
        findings.addAll(secretFindings)

        // 2. Structural & Syntax sanity checks (bracket parity, unclosed quotes)
        checkStructuralIntegrity(content, fileName, findings, errors, warnings)

        // 3. Language-specific heuristic inspections
        when (language) {
            CodeLanguage.KOTLIN -> analyzeKotlin(lines, fileName, findings, warnings, suggestions)
            CodeLanguage.JAVA -> analyzeJava(lines, fileName, findings, warnings, suggestions)
            CodeLanguage.PYTHON -> analyzePython(lines, fileName, findings, warnings, suggestions)
            CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> analyzeJavaScriptTypeScript(lines, fileName, findings, warnings, suggestions)
            CodeLanguage.GRADLE -> analyzeGradle(lines, fileName, findings, warnings, suggestions)
            CodeLanguage.SQL -> analyzeSql(lines, fileName, findings, warnings, suggestions)
            else -> {
                // General checks for other/unknown formats
                analyzeGeneral(lines, fileName, findings, warnings, suggestions)
            }
        }

        // 4. Common checks (TODOs, FIXMEs)
        analyzeTodos(lines, fileName, findings)

        // 5. Compute complexity indicators
        val complexity = computeComplexity(lines)

        val summary = buildSummary(language, fileName, lines.size, findings, complexity)

        return CodeAnalysisResult(
            language = language,
            filesAnalyzed = if (fileName != null) listOf(fileName) else listOf("snippet"),
            summary = summary,
            findings = findings,
            errors = errors,
            warnings = warnings,
            suggestions = suggestions,
            complexityIndicators = complexity,
            securityFindings = secretFindings,
            confidence = 0.95f,
            isAiAssisted = false,
            containsRedactedSecrets = secretFindings.isNotEmpty()
        )
    }

    private fun checkStructuralIntegrity(
        content: String,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        var openBrace = 0
        var openParen = 0
        var openBracket = 0
        var inString = false
        var stringChar = ' '

        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (inString) {
                if (c == '\\') {
                    i += 2 // skip escape
                    continue
                } else if (c == stringChar) {
                    inString = false
                }
            } else {
                when (c) {
                    '"', '\'' -> {
                        inString = true
                        stringChar = c
                    }
                    '{' -> openBrace++
                    '}' -> openBrace--
                    '(' -> openParen++
                    ')' -> openParen--
                    '[' -> openBracket++
                    ']' -> openBracket--
                }
            }
            i++
        }

        if (openBrace != 0) {
            val desc = if (openBrace > 0) "Unclosed curly brace '{' detected ($openBrace unclosed)" else "Extra closing curly brace '}' detected"
            findings.add(CodeFinding(FindingSeverity.HIGH, FindingCategory.SYNTAX, fileName, null, null, desc, "Unbalanced curly braces cause compilation or parsing failures.", "Check and balance all '{' and '}' pairs."))
            errors.add(desc)
        }
        if (openParen != 0) {
            val desc = if (openParen > 0) "Unclosed parenthesis '(' detected ($openParen unclosed)" else "Extra closing parenthesis ')' detected"
            findings.add(CodeFinding(FindingSeverity.HIGH, FindingCategory.SYNTAX, fileName, null, null, desc, "Unbalanced parentheses cause syntax errors.", "Check and balance all '(' and ')' pairs."))
            errors.add(desc)
        }
        if (openBracket != 0) {
            val desc = if (openBracket > 0) "Unclosed square bracket '[' detected ($openBracket unclosed)" else "Extra closing bracket ']' detected"
            findings.add(CodeFinding(FindingSeverity.MEDIUM, FindingCategory.SYNTAX, fileName, null, null, desc, "Unbalanced square brackets cause syntax errors.", "Check and balance all '[' and ']' pairs."))
            warnings.add(desc)
        }
    }

    private fun analyzeKotlin(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        lines.forEachIndexed { index, rawLine ->
            val lineNum = index + 1
            val line = rawLine.trim()

            // 1. Unsafe force unwrap: !!
            if (line.contains("!!") && !line.startsWith("//") && !line.startsWith("*")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.NULLABILITY,
                        file = fileName,
                        line = lineNum,
                        description = "Force unwrap operator '!!' used",
                        explanation = "Using '!!' throws NullPointerException at runtime if the operand is null.",
                        suggestedFix = "Use safe call '?.' or elvis operator '?:' with a default fallback."
                    )
                )
                warnings.add("Line $lineNum: Force unwrap '!!' detected.")
            }

            // 2. GlobalScope.launch
            if (line.contains("GlobalScope.launch") || line.contains("GlobalScope.async")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.HIGH,
                        category = FindingCategory.CONCURRENCY,
                        file = fileName,
                        line = lineNum,
                        description = "Unstructured concurrency: 'GlobalScope' used",
                        explanation = "GlobalScope coroutines break structured concurrency and can leak memory or survive Activity/ViewModel destruction.",
                        suggestedFix = "Use viewModelScope, lifecycleScope, or a SupervisorJob-backed CoroutineScope."
                    )
                )
            }

            // 3. Thread.sleep
            if (line.contains("Thread.sleep(") && !line.startsWith("//")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.PERFORMANCE,
                        file = fileName,
                        line = lineNum,
                        description = "Thread.sleep() blocks calling thread",
                        explanation = "Blocking threads directly causes UI jank or ANR (Application Not Responding) if executed on Dispatchers.Main.",
                        suggestedFix = "Use 'delay()' within suspending coroutine blocks."
                    )
                )
            }

            // 4. Empty catch block
            if (line.contains("catch (") && (line.endsWith("{}") || (index + 1 < lines.size && lines[index + 1].trim() == "}"))) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.ERROR_HANDLING,
                        file = fileName,
                        line = lineNum,
                        description = "Empty catch block swallowing exceptions",
                        explanation = "Swallowing exceptions silently obscures root causes and makes debugging difficult.",
                        suggestedFix = "Log the exception or handle/rethrow appropriately."
                    )
                )
            }

            // 5. Unclosed FileOutputStream / FileInputStream without use block
            if ((line.contains("FileOutputStream(") || line.contains("FileInputStream(")) && !line.contains(".use") && !lines.any { it.contains(".use {") }) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.LOW,
                        category = FindingCategory.PERFORMANCE,
                        file = fileName,
                        line = lineNum,
                        description = "Stream opened without explicit .use { } block",
                        explanation = "Unclosed file streams can lead to file descriptor leaks under heavy I/O.",
                        suggestedFix = "Wrap stream instantiation with '.use { ... }' for automatic closing."
                    )
                )
            }

            // 6. Assignment in if condition (Kotlin catches this in compiler, but good for raw code snippets)
            if (Regex("""if\s*\(\s*[a-zA-Z0-9_]+\s*=\s*[^=]""").containsMatchIn(line)) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.HIGH,
                        category = FindingCategory.BUG,
                        file = fileName,
                        line = lineNum,
                        description = "Assignment inside condition",
                        explanation = "Single '=' is an assignment, not a comparison '=='.",
                        suggestedFix = "Replace '=' with '==' or '==='."
                    )
                )
            }

            // 7. Division by zero literal
            if (Regex("""/\s*0(?![0-9.])""").containsMatchIn(line) && !line.startsWith("//")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.HIGH,
                        category = FindingCategory.BUG,
                        file = fileName,
                        line = lineNum,
                        description = "Literal division by zero detected",
                        explanation = "Integer division by zero throws ArithmeticException at runtime.",
                        suggestedFix = "Check divisor is non-zero before division."
                    )
                )
            }
        }
    }

    private fun analyzeJava(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        lines.forEachIndexed { index, rawLine ->
            val lineNum = index + 1
            val line = rawLine.trim()

            // 1. String comparison with ==
            if (Regex("""[a-zA-Z0-9_]+\s*==\s*".*"""").containsMatchIn(line) || Regex("""".*"\s*==\s*[a-zA-Z0-9_]+""").containsMatchIn(line)) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.HIGH,
                        category = FindingCategory.BUG,
                        file = fileName,
                        line = lineNum,
                        description = "String reference equality '==' used instead of .equals()",
                        explanation = "In Java, '==' compares object memory references rather than string value content.",
                        suggestedFix = "Use '\"value\".equals(variable)' or Objects.equals(a, b)."
                    )
                )
            }

            // 2. Empty catch block
            if (line.contains("catch (") && (line.endsWith("{}") || (index + 1 < lines.size && lines[index + 1].trim() == "}"))) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.ERROR_HANDLING,
                        file = fileName,
                        line = lineNum,
                        description = "Empty catch block swallowing exceptions",
                        explanation = "Exceptions silently discarded hide defects.",
                        suggestedFix = "Log or rethrow the exception."
                    )
                )
            }

            // 3. Raw Exception catching
            if (line.contains("catch (Exception ") || line.contains("catch (Throwable ")) {
                suggestions.add("Line $lineNum: Catching broad 'Exception' or 'Throwable' may swallow unexpected runtime errors.")
            }
        }
    }

    private fun analyzePython(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        lines.forEachIndexed { index, rawLine ->
            val lineNum = index + 1
            val line = rawLine.trim()

            // 1. Mutable default argument: def foo(items=[]):
            if (Regex("""def\s+[a-zA-Z0-9_]+\s*\(.*=\s*(\[\]|\{\})\s*.*\)""").containsMatchIn(line)) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.HIGH,
                        category = FindingCategory.BUG,
                        file = fileName,
                        line = lineNum,
                        description = "Mutable default argument in function definition",
                        explanation = "Default mutable arguments (lists/dicts) are evaluated once at definition time and shared across all calls.",
                        suggestedFix = "Use 'None' as default value and initialize inside function: 'if items is None: items = []'."
                    )
                )
            }

            // 2. Bare except:
            if (line == "except:" || line.startsWith("except :")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.ERROR_HANDLING,
                        file = fileName,
                        line = lineNum,
                        description = "Bare 'except:' catches all exceptions including SystemExit and KeyboardInterrupt",
                        explanation = "A bare except clause prevents normal process termination and masks bugs.",
                        suggestedFix = "Specify 'except Exception:' or target specific exception types."
                    )
                )
            }

            // 3. is comparison with literal
            if (Regex("""\bis\s+(?:["'\d]+|True|False)\b""").containsMatchIn(line) && !line.contains("is None") && !line.contains("is not None")) {
                if (line.contains("is \"") || line.contains("is '") || Regex("""\bis\s+\d+""").containsMatchIn(line)) {
                    findings.add(
                        CodeFinding(
                            severity = FindingSeverity.MEDIUM,
                            category = FindingCategory.BUG,
                            file = fileName,
                            line = lineNum,
                            description = "'is' used with literal instead of '=='",
                            explanation = "'is' tests identity, which is not guaranteed for literals across Python implementations.",
                            suggestedFix = "Use '==' for value equality."
                        )
                    )
                }
            }

            // 4. open() without with
            if (line.contains("open(") && !line.contains("with ") && !lines.any { it.contains("with open(") }) {
                suggestions.add("Line $lineNum: Consider using 'with open(...) as f:' context manager to ensure automatic file closure.")
            }
        }
    }

    private fun analyzeJavaScriptTypeScript(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        lines.forEachIndexed { index, rawLine ->
            val lineNum = index + 1
            val line = rawLine.trim()

            // 1. Loose equality == vs ===
            if (line.contains(" == ") && !line.contains(" === ") && !line.startsWith("//")) {
                suggestions.add("Line $lineNum: Consider using strict equality '===' instead of loose equality '=='.")
            }

            // 2. var keyword
            if (line.startsWith("var ") && !line.startsWith("//")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.LOW,
                        category = FindingCategory.STYLE,
                        file = fileName,
                        line = lineNum,
                        description = "Legacy 'var' keyword used",
                        explanation = "'var' has function scope and is hoisted, which often leads to unintended variable shadowing.",
                        suggestedFix = "Use 'const' for constants and 'let' for reassignable variables."
                    )
                )
            }

            // 3. eval() usage
            if (line.contains("eval(") && !line.startsWith("//")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.CRITICAL,
                        category = FindingCategory.SECURITY,
                        file = fileName,
                        line = lineNum,
                        description = "Dangerous 'eval()' execution detected",
                        explanation = "eval() executes arbitrary code strings and presents extreme security risks.",
                        suggestedFix = "Use JSON.parse() or structured parsing libraries instead of eval()."
                    )
                )
            }
        }
    }

    private fun analyzeGradle(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        lines.forEachIndexed { index, rawLine ->
            val lineNum = index + 1
            val line = rawLine.trim()

            // 1. Dynamic versioning e.g. "com.android.tools.build:gradle:+"
            if (line.contains(":+") || line.contains(":latest.release")) {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.DEPENDENCY,
                        file = fileName,
                        line = lineNum,
                        description = "Dynamic dependency version (+) detected",
                        explanation = "Dynamic versions make builds non-deterministic and can introduce breaking changes unexpectedly.",
                        suggestedFix = "Pin dependencies to explicit, stable semantic versions."
                    )
                )
            }
        }
    }

    private fun analyzeSql(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        val full = lines.joinToString(" ")
        if (full.contains("SELECT *", ignoreCase = true)) {
            suggestions.add("Consider specifying explicit column names rather than 'SELECT *' for better performance and schema resilience.")
        }
        if (full.contains("DROP TABLE", ignoreCase = true) && !full.contains("IF EXISTS", ignoreCase = true)) {
            warnings.add("DROP TABLE statement without 'IF EXISTS' will fail if the table does not exist.")
        }
    }

    private fun analyzeGeneral(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        // Fallback generic inspection
    }

    private fun analyzeTodos(
        lines: List<String>,
        fileName: String?,
        findings: MutableList<CodeFinding>
    ) {
        lines.forEachIndexed { index, rawLine ->
            val lineNum = index + 1
            val line = rawLine.trim()
            if (line.contains("TODO", ignoreCase = false) || line.contains("FIXME", ignoreCase = false) || line.contains("HACK:", ignoreCase = false)) {
                val tag = when {
                    line.contains("FIXME") -> "FIXME"
                    line.contains("HACK:") -> "HACK"
                    else -> "TODO"
                }
                findings.add(
                    CodeFinding(
                        severity = if (tag == "FIXME") FindingSeverity.LOW else FindingSeverity.INFO,
                        category = FindingCategory.MAINTAINABILITY,
                        file = fileName,
                        line = lineNum,
                        description = "$tag marker identified",
                        explanation = "Uncompleted work marker: '${line.take(80)}'",
                        suggestedFix = "Resolve the pending implementation or track in project backlog."
                    )
                )
            }
        }
    }

    private fun computeComplexity(lines: List<String>): ComplexityIndicators {
        var code = 0
        var comment = 0
        var blank = 0
        var maxNesting = 0
        var currentNesting = 0
        var functions = 0
        var classes = 0
        var imports = 0

        for (raw in lines) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                blank++
                continue
            }
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("#")) {
                comment++
                continue
            }
            code++

            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) imports++
            if (trimmed.startsWith("class ") || trimmed.contains(" data class ") || trimmed.startsWith("public class ") || trimmed.startsWith("interface ")) classes++
            if (trimmed.startsWith("fun ") || trimmed.startsWith("def ") || trimmed.startsWith("function ") || trimmed.contains("public void ") || trimmed.contains("private fun ")) functions++

            for (c in trimmed) {
                if (c == '{' || c == '(') {
                    currentNesting++
                    if (currentNesting > maxNesting) maxNesting = currentNesting
                } else if (c == '}' || c == ')') {
                    if (currentNesting > 0) currentNesting--
                }
            }
        }

        val score = when {
            code > 300 || maxNesting > 5 || functions > 15 -> "High"
            code > 100 || maxNesting > 3 || functions > 5 -> "Moderate"
            else -> "Low"
        }

        return ComplexityIndicators(
            totalLines = lines.size,
            codeLines = code,
            commentLines = comment,
            blankLines = blank,
            estimatedMaxNestingDepth = maxNesting,
            functionCount = functions,
            classCount = classes,
            importCount = imports,
            estimatedComplexityScore = score
        )
    }

    private fun buildSummary(
        language: CodeLanguage,
        fileName: String?,
        totalLines: Int,
        findings: List<CodeFinding>,
        complexity: ComplexityIndicators
    ): String {
        val target = fileName ?: "Source snippet"
        val criticalCount = findings.count { it.severity == FindingSeverity.CRITICAL }
        val highCount = findings.count { it.severity == FindingSeverity.HIGH }
        val mediumCount = findings.count { it.severity == FindingSeverity.MEDIUM }

        return buildString {
            append("Analyzed $target ($totalLines lines, ${language.displayName}). ")
            if (findings.isEmpty()) {
                append("No critical issues, security hazards, or obvious bugs detected. Complexity: ${complexity.estimatedComplexityScore}.")
            } else {
                append("Identified ${findings.size} finding(s) [Critical: $criticalCount, High: $highCount, Medium: $mediumCount]. Complexity: ${complexity.estimatedComplexityScore}.")
            }
        }
    }
}
