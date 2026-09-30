package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.CodeAnalysisTool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.code.AiCodeAnalyzer
import com.example.jarvis.code.CodeAnalysisResult
import com.example.jarvis.code.CodeExplanationEngine
import com.example.jarvis.code.CodeFinding
import com.example.jarvis.code.CodeLanguage
import com.example.jarvis.code.CodeLanguageDetector
import com.example.jarvis.code.CodeSecurityScanner
import com.example.jarvis.code.DeterministicCodeAnalyzer
import com.example.jarvis.code.FindingCategory
import com.example.jarvis.code.FindingSeverity
import com.example.jarvis.code.ProjectCodeAnalyzer
import com.example.jarvis.code.StackTraceAnalyzer
import com.example.jarvis.provider.AIProvider
import com.example.jarvis.provider.ToolDecision
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisCodeAnalysisUnitTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        bridge = AndroidBridge(context)
    }

    // 1. KOTLIN ANALYSIS
    @Test
    fun testKotlinAnalysis_detectsSuspiciousPatterns() {
        val code = """
            package com.example
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch
            
            class TestService {
                fun process(data: String?) {
                    val length = data!!.length
                    Thread.sleep(1000)
                    GlobalScope.launch {
                        try {
                            println(length / 0)
                        } catch (e: Exception) {}
                    }
                }
            }
        """.trimIndent()

        val result = DeterministicCodeAnalyzer.analyze(code, "TestService.kt")

        assertEquals(CodeLanguage.KOTLIN, result.language)
        assertTrue("Should detect force unwrap !!", result.findings.any { it.category == FindingCategory.NULLABILITY })
        assertTrue("Should detect GlobalScope", result.findings.any { it.category == FindingCategory.CONCURRENCY })
        assertTrue("Should detect Thread.sleep", result.findings.any { it.category == FindingCategory.PERFORMANCE })
        assertTrue("Should detect empty catch", result.findings.any { it.category == FindingCategory.ERROR_HANDLING })
        assertTrue("Should detect division by zero", result.findings.any { it.category == FindingCategory.BUG })
    }

    // 2. JAVA ANALYSIS
    @Test
    fun testJavaAnalysis_detectsStringEqualityAndSwallowedExceptions() {
        val code = """
            package com.example;
            public class AuthManager {
                public boolean authenticate(String inputPassword) {
                    if (inputPassword == "secret123") {
                        try {
                            performLogin();
                        } catch (Exception e) {
                        }
                        return true;
                    }
                    return false;
                }
                private void performLogin() {}
            }
        """.trimIndent()

        val result = DeterministicCodeAnalyzer.analyze(code, "AuthManager.java")

        assertEquals(CodeLanguage.JAVA, result.language)
        assertTrue("Should detect string equality with ==", result.findings.any { it.category == FindingCategory.BUG && it.description.contains("==") })
        assertTrue("Should detect empty catch", result.findings.any { it.category == FindingCategory.ERROR_HANDLING })
    }

    // 3. PYTHON ANALYSIS
    @Test
    fun testPythonAnalysis_detectsMutableDefaultsAndBareExcept() {
        val code = """
            #!/usr/bin/env python3
            def process_items(items=[]):
                try:
                    if len(items) is 0:
                        return None
                    return [x * 2 for x in items]
                except:
                    pass
        """.trimIndent()

        val result = DeterministicCodeAnalyzer.analyze(code, "script.py")

        assertEquals(CodeLanguage.PYTHON, result.language)
        assertTrue("Should detect mutable default argument", result.findings.any { it.category == FindingCategory.BUG && it.description.contains("Mutable default") })
        assertTrue("Should detect bare except", result.findings.any { it.category == FindingCategory.ERROR_HANDLING && it.description.contains("bare", ignoreCase = true) })
    }

    // 4. LANGUAGE DETECTION
    @Test
    fun testLanguageDetection_identifiesMultipleFormats() {
        assertEquals(CodeLanguage.KOTLIN, CodeLanguageDetector.detectLanguage("Main.kt", "fun main() {}"))
        assertEquals(CodeLanguage.JAVA, CodeLanguageDetector.detectLanguage("App.java", "public class App {}"))
        assertEquals(CodeLanguage.PYTHON, CodeLanguageDetector.detectLanguage(null, "def calculate(x):\n    return x * 2"))
        assertEquals(CodeLanguage.JSON, CodeLanguageDetector.detectLanguage("data.json", "{\"key\": \"value\"}"))
        assertEquals(CodeLanguage.XML, CodeLanguageDetector.detectLanguage(null, "<?xml version=\"1.0\"?><root><item/></root>"))
        assertEquals(CodeLanguage.HTML, CodeLanguageDetector.detectLanguage("index.html", "<!DOCTYPE html><html><body></body></html>"))
        assertEquals(CodeLanguage.SQL, CodeLanguageDetector.detectLanguage(null, "SELECT id, name FROM users WHERE active = 1"))
        assertEquals(CodeLanguage.SHELL, CodeLanguageDetector.detectLanguage(null, "#!/bin/bash\necho 'Running setup'"))
        assertEquals(CodeLanguage.GRADLE, CodeLanguageDetector.detectLanguage("build.gradle.kts", "plugins { id(\"com.android.application\") }"))
    }

    // 5. SECRET DETECTION
    @Test
    fun testSecretDetection_identifiesCriticalCredentials() {
        val codeWithSecrets = """
            val googleApiKey = "AIzaSyDa9876543210abcdefghijklmnopqrstu"
            val openAiKey = "sk-1234567890abcdef1234567890abcdef12"
            val awsKey = "AKIAIOSFODNN7EXAMPLE"
            val githubToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
            val privateKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...\n-----END RSA PRIVATE KEY-----"
        """.trimIndent()

        val findings = CodeSecurityScanner.scanForSecrets(codeWithSecrets)

        assertTrue(findings.size >= 5)
        assertTrue("Should identify Google API key", findings.any { it.description.contains("Google") })
        assertTrue("Should identify OpenAI key", findings.any { it.description.contains("OpenAI") })
        assertTrue("Should identify AWS key", findings.any { it.description.contains("AWS") })
        assertTrue("Should identify GitHub token", findings.any { it.description.contains("GitHub") })
        assertTrue("Should identify Private Key", findings.any { it.description.contains("Private Key") })
        findings.forEach {
            assertEquals(FindingCategory.SECURITY, it.category)
            assertTrue(it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.HIGH)
        }
    }

    // 6. MALFORMED SOURCE (SYNTAX SANITY)
    @Test
    fun testMalformedSource_detectsUnbalancedBrackets() {
        val brokenCode = """
            fun calculate(value: Int {
                if (value > 0) {
                    return value * 2
                // Missing closing braces
        """.trimIndent()

        val result = DeterministicCodeAnalyzer.analyze(brokenCode, "Broken.kt")

        assertTrue("Should detect syntax issues", result.errors.isNotEmpty() || result.findings.any { it.category == FindingCategory.SYNTAX })
        assertTrue(result.findings.any { it.description.contains("brace") || it.description.contains("parenthesis") })
    }

    // 7. STACK TRACE & ERROR EXTRACTION
    @Test
    fun testStackTraceAnalyzer_parsesKotlinAndJavaExceptions() {
        // Kotlin compiler error
        val ktError = "e: /app/src/main/java/com/example/Test.kt: (42, 18): Unresolved reference: calculateTotal"
        assertTrue(StackTraceAnalyzer.isStackTraceOrErrorLog(ktError))
        val ktResult = StackTraceAnalyzer.analyze(ktError)
        assertEquals("KotlinCompilationError", ktResult.errorType)
        assertEquals("/app/src/main/java/com/example/Test.kt:42:18", ktResult.likelySourceLocation)
        assertTrue(ktResult.likelyCauses.any { it.contains("Unresolved reference") || it.contains("calculateTotal") })

        // Java runtime exception
        val javaCrash = """
            FATAL EXCEPTION: main
            Process: com.example.jarvis, PID: 1234
            java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
                at com.example.jarvis.ui.MainActivity.onCreate(MainActivity.kt:105)
                at android.app.Activity.performCreate(Activity.java:8051)
        """.trimIndent()
        val crashResult = StackTraceAnalyzer.analyze(javaCrash)
        assertEquals("java.lang.NullPointerException", crashResult.errorType)
        assertEquals("MainActivity.kt:105 (com.example.jarvis.ui.MainActivity.onCreate)", crashResult.likelySourceLocation)
        assertTrue(crashResult.suggestedFixes.any { it.contains("safe calls") || it.contains("null") })
    }

    // 8. OBVIOUS BUG DETECTION
    @Test
    fun testObviousBugDetection_identifiesDivisionByZero() {
        val code = """
            fun compute(): Int {
                val ratio = 100 / 0
                return ratio
            }
        """.trimIndent()

        val result = DeterministicCodeAnalyzer.analyze(code, "MathUtils.kt")
        assertTrue("Should detect literal division by zero", result.findings.any { it.category == FindingCategory.BUG && it.description.contains("division by zero") })
    }

    // 9. STRUCTURED AI RESPONSE PARSING
    @Test
    fun testAiCodeAnalyzer_parsesStructuredJsonResponse() = runBlocking {
        val mockAiProvider = object : AIProvider {
            override suspend fun generateResponse(prompt: String, systemInstruction: String, onChunkReceived: (String) -> Unit): String {
                return """
                    {
                      "summary": "Detected potential concurrency leak and missing null check.",
                      "findings": [
                        {
                          "severity": "HIGH",
                          "category": "CONCURRENCY",
                          "line": 12,
                          "description": "Unbounded coroutine dispatch",
                          "explanation": "Dispatching infinite jobs without lifecycle scope can crash the application.",
                          "suggestedFix": "Bind to viewModelScope."
                        }
                      ],
                      "suggestions": ["Add unit tests for edge cases"]
                    }
                """.trimIndent()
            }
            override suspend fun decideTool(userInput: String, availableTools: List<Pair<String, String>>, contextHistory: String) = ToolDecision(false)
            override suspend fun analyzeImage(prompt: String, bitmap: android.graphics.Bitmap) = ""
        }

        val code = "fun runBackground() {}"
        val deterministic = DeterministicCodeAnalyzer.analyze(code, "Async.kt")
        val aiResult = AiCodeAnalyzer.analyzeWithAi(code, "Async.kt", CodeLanguage.KOTLIN, mockAiProvider, deterministic)

        assertTrue(aiResult.isAiAssisted)
        assertEquals("Detected potential concurrency leak and missing null check.", aiResult.summary)
        assertTrue(aiResult.findings.any { it.description == "Unbounded coroutine dispatch" })
    }

    // 10. MALFORMED AI RESPONSE FALLBACK
    @Test
    fun testAiCodeAnalyzer_fallbackOnMalformedAiResponse() = runBlocking {
        val mockBrokenAi = object : AIProvider {
            override suspend fun generateResponse(prompt: String, systemInstruction: String, onChunkReceived: (String) -> Unit): String {
                return "Sure! Here is my analysis: The code is totally broken because reasons. Have a great day!"
            }
            override suspend fun decideTool(userInput: String, availableTools: List<Pair<String, String>>, contextHistory: String) = ToolDecision(false)
            override suspend fun analyzeImage(prompt: String, bitmap: android.graphics.Bitmap) = ""
        }

        val code = "val x = 10"
        val deterministic = DeterministicCodeAnalyzer.analyze(code, "Simple.kt")
        val result = AiCodeAnalyzer.analyzeWithAi(code, "Simple.kt", CodeLanguage.KOTLIN, mockBrokenAi, deterministic)

        assertNotNull(result)
        // Retains deterministic findings and does not crash or fabricate false findings
        assertFalse(result.findings.any { it.description.contains("totally broken") })
    }

    // 11. SECRET NEVER SENT TO AI PROVIDER
    @Test
    fun testSecretNeverSentToAiProvider() = runBlocking {
        var capturedPrompt = ""
        val secretKey = "AIzaSyDa9876543210abcdefghijklmnopqrstu"

        val capturingAiProvider = object : AIProvider {
            override suspend fun generateResponse(prompt: String, systemInstruction: String, onChunkReceived: (String) -> Unit): String {
                capturedPrompt = prompt
                return "{ \"summary\": \"Clean code\", \"findings\": [] }"
            }
            override suspend fun decideTool(userInput: String, availableTools: List<Pair<String, String>>, contextHistory: String) = ToolDecision(false)
            override suspend fun analyzeImage(prompt: String, bitmap: android.graphics.Bitmap) = ""
        }

        val rawCode = "val apiKey = \"$secretKey\""
        val deterministic = DeterministicCodeAnalyzer.analyze(rawCode, "Secret.kt")

        AiCodeAnalyzer.analyzeWithAi(rawCode, "Secret.kt", CodeLanguage.KOTLIN, capturingAiProvider, deterministic)

        assertFalse("Raw secret key MUST NEVER be sent to AIProvider!", capturedPrompt.contains(secretKey))
        assertTrue("Prompt should contain redacted placeholder", capturedPrompt.contains("[REDACTED_GOOGLE_API_KEY]"))
    }

    // 12. NO AUTOMATIC SOURCE MODIFICATION (PURE DATA ANALYSIS)
    @Test
    fun testNoAutomaticSourceModification_isReadOnlyDataResult() {
        val originalCode = "val a = 1\nval b = 2"
        val result = DeterministicCodeAnalyzer.analyze(originalCode, "Readonly.kt")

        // Verifies analysis output is structured data only
        assertNotNull(result)
        assertEquals(CodeLanguage.KOTLIN, result.language)
        assertEquals(2, result.complexityIndicators.totalLines)
    }

    // 13. NO CODE EXECUTION (ANALYSIS ONLY BOUNDARY)
    @Test
    fun testNoCodeExecution_treatsSourceStrictlyAsData() {
        val maliciousCode = """
            Runtime.getRuntime().exec("rm -rf /")
            System.exit(0)
        """.trimIndent()

        val result = DeterministicCodeAnalyzer.analyze(maliciousCode, "Dangerous.java")
        assertNotNull(result)
        // Successfully inspected as inert data without executing runtime commands
        assertEquals(CodeLanguage.JAVA, result.language)
    }

    // 14. EMPTY INPUT HANDLING
    @Test
    fun testEmptyInput_handledGracefully() = runBlocking {
        val result = DeterministicCodeAnalyzer.analyze("", null)
        assertEquals(CodeLanguage.UNKNOWN, result.language)
        assertEquals(0, result.complexityIndicators.codeLines)
        assertTrue(result.findings.isEmpty())

        val tool = CodeAnalysisTool()
        val ctx = ToolContext(repository = repository, bridge = bridge)
        val toolResult = tool.execute("", ctx)
        assertFalse("Empty input should fail gracefully with helpful guidance", toolResult.success)
    }

    // 15. PROJECT CODE ANALYZER (EXPLICIT FILES ONLY)
    @Test
    fun testProjectCodeAnalyzer_detectsMissingManifestPermissions() {
        val manifest = ProjectCodeAnalyzer.ExplicitFile(
            fileName = "AndroidManifest.xml",
            content = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><uses-permission android:name=\"android.permission.INTERNET\" /></manifest>"
        )
        val kotlinFile = ProjectCodeAnalyzer.ExplicitFile(
            fileName = "CameraActivity.kt",
            content = "class CameraActivity { fun takePicture() { val camera = Camera.open() } }"
        )

        val projectResult = ProjectCodeAnalyzer.analyzeExplicitFiles(listOf(manifest, kotlinFile))

        assertEquals(2, projectResult.filesAnalyzed.size)
        assertTrue("Should detect declared INTERNET permission", projectResult.declaredPermissions.contains("INTERNET"))
        assertTrue("Should detect missing CAMERA permission", projectResult.missingPermissions.contains("CAMERA"))
    }
}
