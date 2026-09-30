package com.example.jarvis.code

/**
 * Intelligent parser and diagnostic engine for compiler errors, runtime exceptions, and stack traces.
 * Clearly separates detected factual error details from heuristic suggested solutions.
 */
object StackTraceAnalyzer {

    private val KOTLIN_COMPILER_ERROR_REGEX = Regex("""(?:e|w):\s*([^\n:]+):\s*\((\d+),\s*(\d+)\):\s*([^\n]+)""")
    private val JAVA_EXCEPTION_HEADER_REGEX = Regex("""(?m)^([a-zA-Z0-9_.]+(?:Exception|Error|Throwable|Failure)):?\s*(.*)$""")
    private val STACK_FRAME_REGEX = Regex("""\s*at\s+([a-zA-Z0-9_.$]+)\.([a-zA-Z0-9_<>]+)\(([^:]+?):(\d+)\)""")
    private val ANDROID_FATAL_REGEX = Regex("""FATAL EXCEPTION:\s*([^\n]+)""")
    private val GRADLE_TASK_FAILURE_REGEX = Regex("""Execution failed for task '([^']+)'.\s*> ([^\n]+)""")

    /**
     * Checks if a snippet appears to be a stack trace or compiler error log.
     */
    fun isStackTraceOrErrorLog(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.contains("Exception") ||
                trimmed.contains("Error") ||
                trimmed.contains("\tat ") ||
                trimmed.contains("e: ") ||
                trimmed.contains("Execution failed for task") ||
                trimmed.contains("FATAL EXCEPTION") ||
                trimmed.contains("Build failed with an exception")
    }

    /**
     * Analyzes error logs, compiler messages, or stack traces and produces structured diagnostics.
     */
    fun analyze(text: String): StackTraceAnalysisResult {
        val trimmed = text.trim()

        // 1. Check for Kotlin compiler error: e: file.kt: (line, col): message
        val ktMatch = KOTLIN_COMPILER_ERROR_REGEX.find(trimmed)
        if (ktMatch != null) {
            val file = ktMatch.groupValues[1].trim()
            val line = ktMatch.groupValues[2]
            val col = ktMatch.groupValues[3]
            val message = ktMatch.groupValues[4].trim()

            val detectedCauses = mutableListOf<String>()
            val likelyCauses = mutableListOf<String>()
            val suggestedFixes = mutableListOf<String>()

            detectedCauses.add("Kotlin compiler error at $file:$line:$col")

            when {
                message.contains("Unresolved reference", ignoreCase = true) -> {
                    val symbol = message.substringAfter("Unresolved reference:").trim()
                    likelyCauses.add("Symbol '$symbol' is not imported, misspelled, or missing from dependencies.")
                    suggestedFixes.add("Import the symbol '$symbol' or verify its package/dependency declaration in build.gradle.kts.")
                }
                message.contains("Type mismatch", ignoreCase = true) -> {
                    likelyCauses.add("Expression type does not match the expected parameter or assignment type.")
                    suggestedFixes.add("Check type signatures or apply explicit type casting / mapping.")
                }
                message.contains("Cannot find a parameter with this name", ignoreCase = true) -> {
                    likelyCauses.add("Named parameter does not exist in the referenced constructor or function signature.")
                    suggestedFixes.add("Verify function signature parameters or library version changes.")
                }
                message.contains("Null can not be a value of a non-null type", ignoreCase = true) -> {
                    likelyCauses.add("Attempted to assign null to a non-nullable type.")
                    suggestedFixes.add("Mark the type as nullable (Type?) or supply a non-null fallback value.")
                }
                else -> {
                    likelyCauses.add("Compilation failed due to syntax or type mismatch in Kotlin source.")
                    suggestedFixes.add("Inspect line $line in $file and correct the code to align with the type system.")
                }
            }

            return StackTraceAnalysisResult(
                errorType = "KotlinCompilationError",
                message = message,
                likelySourceLocation = "$file:$line:$col",
                surroundingContext = "Line $line in $file",
                detectedCauses = detectedCauses,
                likelyCauses = likelyCauses,
                suggestedFixes = suggestedFixes,
                rawStackTraceSnippet = trimmed.take(500)
            )
        }

        // 2. Check for Gradle Task Failure
        val gradleMatch = GRADLE_TASK_FAILURE_REGEX.find(trimmed)
        if (gradleMatch != null) {
            val task = gradleMatch.groupValues[1]
            val message = gradleMatch.groupValues[2]

            val detectedCauses = listOf("Gradle build task '$task' failed during execution.")
            val likelyCauses = mutableListOf<String>()
            val suggestedFixes = mutableListOf<String>()

            when {
                task.contains("compileDebugKotlin") -> {
                    likelyCauses.add("Kotlin source compilation errors prevented bytecode generation.")
                    suggestedFixes.add("Review compiler error logs above the task summary and resolve syntax/import errors.")
                }
                task.contains("testDebugUnitTest") -> {
                    likelyCauses.add("One or more unit tests failed assertion checks.")
                    suggestedFixes.add("Inspect failed test assertions in build/reports/tests/testDebugUnitTest/index.html.")
                }
                task.contains("ksp") -> {
                    likelyCauses.add("KSP annotation processor (Room/Serialization) encountered schema or version mismatch.")
                    suggestedFixes.add("Verify Room entity annotations, type converters, and KSP-Kotlin version compatibility.")
                }
                else -> {
                    likelyCauses.add("Gradle task execution failed: $message")
                    suggestedFixes.add("Check task input parameters, dependency versions, or run with --stacktrace for full diagnostics.")
                }
            }

            return StackTraceAnalysisResult(
                errorType = "GradleBuildFailure",
                message = "$task: $message",
                likelySourceLocation = task,
                surroundingContext = "Gradle Task: $task",
                detectedCauses = detectedCauses,
                likelyCauses = likelyCauses,
                suggestedFixes = suggestedFixes,
                rawStackTraceSnippet = trimmed.take(500)
            )
        }

        // 3. Check for Standard Java / Android Exception Stack Trace
        val exceptionMatch = JAVA_EXCEPTION_HEADER_REGEX.find(trimmed)
        val frames = STACK_FRAME_REGEX.findAll(trimmed).toList()

        val exceptionName = exceptionMatch?.groupValues?.get(1) ?: "Exception"
        val exceptionMsg = exceptionMatch?.groupValues?.get(2)?.ifBlank { "No detailed message provided" } ?: "Crash trace detected"

        val firstAppFrame = frames.firstOrNull { !it.value.contains("android.os.") && !it.value.contains("java.lang.") && !it.value.contains("kotlinx.coroutines.") }
            ?: frames.firstOrNull()

        val sourceLoc = if (firstAppFrame != null) {
            val className = firstAppFrame.groupValues[1]
            val methodName = firstAppFrame.groupValues[2]
            val file = firstAppFrame.groupValues[3]
            val line = firstAppFrame.groupValues[4]
            "$file:$line ($className.$methodName)"
        } else null

        val detectedCauses = mutableListOf<String>()
        val likelyCauses = mutableListOf<String>()
        val suggestedFixes = mutableListOf<String>()

        detectedCauses.add("Raised $exceptionName: $exceptionMsg")
        if (sourceLoc != null) {
            detectedCauses.add("Top origin stack frame: $sourceLoc")
        }

        // Categorize common Android / JVM exceptions
        when (exceptionName.substringAfterLast('.')) {
            "NullPointerException" -> {
                likelyCauses.add("Attempted to access a method or property on a null object reference.")
                suggestedFixes.add("Add safe calls (?.), elvis fallbacks (?:), or null-checks before invoking methods.")
            }
            "IndexOutOfBoundsException", "ArrayIndexOutOfBoundsException" -> {
                likelyCauses.add("Accessed list or array element outside bounds (size exceeded or empty collection).")
                suggestedFixes.add("Use getOrNull() or check collection.isNotEmpty() before accessing elements by index.")
            }
            "IllegalArgumentException" -> {
                likelyCauses.add("A method received an argument that violates its precondition contract.")
                suggestedFixes.add("Validate parameter values before method invocation.")
            }
            "IllegalStateException" -> {
                likelyCauses.add("Method invoked while the target object or Android Lifecycle is in an incompatible state.")
                suggestedFixes.add("Verify lifecycle state or initialization order before calling this operation.")
            }
            "SecurityException" -> {
                likelyCauses.add("Attempted to access protected Android API without holding the required runtime or manifest permission.")
                suggestedFixes.add("Declare the required permission in AndroidManifest.xml and request runtime permission if required.")
            }
            "ClassCastException" -> {
                likelyCauses.add("Attempted to cast an object to an incompatible type.")
                suggestedFixes.add("Use safe cast 'as?' instead of hard cast 'as' to prevent crash.")
            }
            "NetworkOnMainThreadException" -> {
                likelyCauses.add("Attempted synchronous network I/O on Android UI / main thread.")
                suggestedFixes.add("Dispatch network calls onto Dispatchers.IO or run inside a background coroutine.")
            }
            "ActivityNotFoundException" -> {
                likelyCauses.add("No installed application matches the target Intent.")
                suggestedFixes.add("Check Intent resolution with packageManager.resolveActivity() before calling startActivity.")
            }
            else -> {
                likelyCauses.add("Runtime exception occurred during application execution.")
                suggestedFixes.add("Inspect stack frame origin at $sourceLoc and review surrounding logic.")
            }
        }

        return StackTraceAnalysisResult(
            errorType = exceptionName,
            message = exceptionMsg,
            likelySourceLocation = sourceLoc,
            surroundingContext = frames.take(3).joinToString("\n") { it.value.trim() },
            detectedCauses = detectedCauses,
            likelyCauses = likelyCauses,
            suggestedFixes = suggestedFixes,
            rawStackTraceSnippet = trimmed.take(500)
        )
    }
}
