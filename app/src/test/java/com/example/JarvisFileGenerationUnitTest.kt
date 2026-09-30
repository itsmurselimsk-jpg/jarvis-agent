package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.brain.FileGenerationTool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.document.DocumentModel
import com.example.jarvis.document.DocumentType
import com.example.jarvis.generation.CsvFileGenerator
import com.example.jarvis.generation.FileGenerationPipeline
import com.example.jarvis.generation.FileVerifier
import com.example.jarvis.generation.GeneratedFileFormat
import com.example.jarvis.generation.GenerationRequest
import com.example.jarvis.generation.GenerationSection
import com.example.jarvis.generation.GenerationTable
import com.example.jarvis.generation.JsonFileGenerator
import com.example.jarvis.generation.MarkdownFileGenerator
import com.example.jarvis.generation.TxtFileGenerator
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.provider.JarvisUnifiedAIProvider
import com.example.jarvis.provider.LocalNeuralBrainProvider
import com.example.jarvis.safety.RiskEngine
import com.example.jarvis.security.SensitiveDataFilter
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisFileGenerationUnitTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge
    private lateinit var pipeline: FileGenerationPipeline
    private lateinit var testOutputDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        bridge = AndroidBridge(context)
        testOutputDir = File(context.filesDir, "test_generated_files")
        if (testOutputDir.exists()) {
            testOutputDir.deleteRecursively()
        }
        testOutputDir.mkdirs()
        pipeline = FileGenerationPipeline(context, testOutputDir)
    }

    // 1. TXT GENERATION & VERIFICATION
    @Test
    fun testTxtGenerationAndVerification() = runBlocking {
        val request = GenerationRequest(
            fileName = "system_summary.txt",
            format = GeneratedFileFormat.TXT,
            title = "System Summary",
            sections = listOf(
                GenerationSection(
                    title = "Hardware State",
                    content = "Battery 98%, WiFi connected to JARVIS-Secure-Net.",
                    items = listOf("CPU: 12%", "RAM: 3.4GB / 8GB")
                )
            ),
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertNotNull(result.file)
        assertTrue(result.file!!.exists())
        assertTrue(result.file!!.length() > 0)
        assertEquals("system_summary.txt", result.fileName)
        assertEquals("text/plain", result.mimeType)

        val content = result.file!!.readText(Charsets.UTF_8)
        assertTrue(content.contains("SYSTEM SUMMARY"))
        assertTrue(content.contains("Hardware State"))
        assertTrue(content.contains("Battery 98%"))
        assertTrue(content.contains("CPU: 12%"))

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.TXT)
        assertTrue(ver.isValid)
    }

    // 2. MARKDOWN (.md) GENERATION & VERIFICATION
    @Test
    fun testMarkdownGenerationAndVerification() = runBlocking {
        val request = GenerationRequest(
            fileName = "architecture_report.md",
            format = GeneratedFileFormat.MARKDOWN,
            title = "JARVIS Operational Architecture",
            sections = listOf(
                GenerationSection(
                    title = "Executive Summary",
                    content = "Native Android operating layer with autonomous agentic capabilities.",
                    level = 1
                ),
                GenerationSection(
                    title = "Sensory Telemetry",
                    content = "val status = checkTelemetry()",
                    isCodeBlock = true,
                    codeLanguage = "kotlin",
                    level = 2
                )
            ),
            tables = listOf(
                GenerationTable(
                    title = "Subsystem Matrix",
                    headers = listOf("Subsystem", "Status", "Latency"),
                    rows = listOf(
                        listOf("Neural Brain", "Nominal", "45ms"),
                        listOf("Room DB", "Encrypted", "2ms")
                    )
                )
            ),
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertEquals("text/markdown", result.mimeType)

        val md = result.file!!.readText(Charsets.UTF_8)
        assertTrue(md.contains("# JARVIS Operational Architecture"))
        assertTrue(md.contains("## Executive Summary"))
        assertTrue(md.contains("```kotlin"))
        assertTrue(md.contains("| Subsystem | Status | Latency |"))

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.MARKDOWN)
        assertTrue(ver.isValid)
    }

    // 3. CSV GENERATION & RFC-4180 ESCAPING (Commas, Quotes, Newlines)
    @Test
    fun testCsvGenerationAndEscaping() = runBlocking {
        val request = GenerationRequest(
            fileName = "sensor_logs.csv",
            format = GeneratedFileFormat.CSV,
            tables = listOf(
                GenerationTable(
                    headers = listOf("Timestamp", "Sensor", "Value", "Notes"),
                    rows = listOf(
                        listOf("2026-09-18T10:00:00Z", "Battery", "95%", "Nominal"),
                        listOf("2026-09-18T10:05:00Z", "Temperature", "36.2", "Within limits, \"normal\" cooling\nSecond line")
                    )
                )
            ),
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertEquals("text/csv", result.mimeType)

        val rawCsv = result.file!!.readText(Charsets.UTF_8)
        // Verify quotes are doubled and enclosed
        assertTrue(rawCsv.contains("\"Within limits, \"\"normal\"\" cooling\nSecond line\""))

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.CSV)
        assertTrue(ver.isValid)
    }

    // 4. JSON VALIDITY & NESTED STRUCTURES
    @Test
    fun testJsonValidity() = runBlocking {
        val sampleObj = JSONObject().apply {
            put("status", "ACTIVE")
            put("version", "5.0")
            put("modules", org.json.JSONArray(listOf("Brain", "Room", "FileGen")))
        }

        val request = GenerationRequest(
            fileName = "telemetry.json",
            format = GeneratedFileFormat.JSON,
            jsonObject = sampleObj,
            prettyPrintJson = true,
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertEquals("application/json", result.mimeType)

        val jsonText = result.file!!.readText(Charsets.UTF_8)
        val parsed = JSONObject(jsonText)
        assertEquals("ACTIVE", parsed.getString("status"))
        assertEquals("5.0", parsed.getString("version"))
        assertEquals(3, parsed.getJSONArray("modules").length())

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.JSON)
        assertTrue(ver.isValid)
    }

    // 5. UNICODE & SPECIAL CHARACTERS
    @Test
    fun testUnicodeAndSpecialCharacters() = runBlocking {
        val unicodeTitle = "JARVIS 🚀 Intelligence Report — 智能助手"
        val unicodeContent = "Éléments clés: 100% opérationnel, température: 24.5°C, σύστημα: OK, 日本語テスト。"
        val request = GenerationRequest(
            fileName = "unicode_test.txt",
            format = GeneratedFileFormat.TXT,
            title = unicodeTitle,
            rawContent = unicodeContent,
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)

        val text = result.file!!.readText(Charsets.UTF_8)
        assertTrue(text.contains("JARVIS 🚀"))
        assertTrue(text.contains("Éléments clés"))
        assertTrue(text.contains("日本語テスト"))
    }

    // 6. OVERWRITE PROTECTION ENFORCEMENT
    @Test
    fun testOverwriteProtectionEnforcement() = runBlocking {
        val initialRequest = GenerationRequest(
            fileName = "protected_doc.txt",
            format = GeneratedFileFormat.TXT,
            title = "Initial Document",
            rawContent = "Important original data.",
            targetDirectory = testOutputDir
        )

        val initialResult = pipeline.generateFile(initialRequest)
        assertTrue(initialResult.success)
        assertTrue(initialResult.file!!.exists())

        // Attempt overwrite without permission
        val overwriteAttempt = GenerationRequest(
            fileName = "protected_doc.txt",
            format = GeneratedFileFormat.TXT,
            title = "Overwritten Document",
            rawContent = "Malicious or accidental overwrite attempt.",
            allowOverwrite = false,
            targetDirectory = testOutputDir
        )

        val failedResult = pipeline.generateFile(overwriteAttempt)
        assertFalse(failedResult.success)
        assertFalse(failedResult.verified)
        assertTrue(failedResult.errors.any { it.contains("already exists") })
        // Confirm original content is preserved
        assertTrue(initialResult.file!!.readText(Charsets.UTF_8).contains("Important original data."))

        // Now perform with explicit allowOverwrite = true
        val confirmedOverwrite = overwriteAttempt.copy(allowOverwrite = true)
        val successResult = pipeline.generateFile(confirmedOverwrite)
        assertTrue(successResult.success)
        assertTrue(successResult.verified)
        assertTrue(initialResult.file!!.readText(Charsets.UTF_8).contains("Overwritten Document", ignoreCase = true))
    }

    // 7. VERIFICATION FAILURE DETECTION
    @Test
    fun testVerificationFailureDetection() {
        val badJsonFile = File(testOutputDir, "corrupt.json")
        badJsonFile.writeText("{ status: 'malformed json without quotes, }", Charsets.UTF_8)

        val ver = FileVerifier.verify(badJsonFile, GeneratedFileFormat.JSON)
        assertFalse(ver.isValid)
        assertTrue(ver.details.contains("JSON parsing failure") || ver.errors.isNotEmpty())

        val emptyFile = File(testOutputDir, "empty.txt")
        emptyFile.writeText("", Charsets.UTF_8)
        val emptyVer = FileVerifier.verify(emptyFile, GeneratedFileFormat.TXT)
        assertFalse(emptyVer.isValid)
    }

    // 8. FILE GENERATION TOOL INTEGRATION WITH AGENT BRAIN
    @Test
    fun testFileGenerationToolDirectExecution() = runBlocking {
        val brain = AgentBrain(
            repository = repository,
            bridge = bridge,
            aiProvider = JarvisUnifiedAIProvider(repository),
            onConfirmationRequired = {}
        )

        val activeDoc = DocumentModel(
            fileName = "telemetry_source.txt",
            mimeType = "text/plain",
            sizeBytes = 256,
            documentType = DocumentType.TXT,
            extractedText = "JARVIS System Telemetry Report: CPU 15%, Battery 92%, Network LTE Online.",
            pageOrSectionCount = 1
        )
        brain.attachDocument(activeDoc)

        val tool = FileGenerationTool()
        val toolContext = ToolContext(
            repository = repository,
            bridge = bridge,
            activeDocument = activeDoc,
            fileGenerationPipeline = pipeline
        )

        val result = tool.execute("generate a markdown report called telemetry_export.md", toolContext)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertTrue(result.output.contains("FILE GENERATION COMPLETED"))
        assertTrue(result.output.contains("telemetry_export.md"))
        assertTrue(result.output.contains("MARKDOWN"))

        val exportedFile = File(testOutputDir, "telemetry_export.md")
        assertTrue(exportedFile.exists())
        assertTrue(exportedFile.length() > 0)
    }

    // 9. OVERWRITE CONFIRMATION DETECTION VIA TOOL & RISK ENGINE
    @Test
    fun testOverwriteConfirmationViaTool() = runBlocking {
        val existing = File(testOutputDir, "budget.csv")
        existing.writeText("col1,col2\nval1,val2")

        val tool = FileGenerationTool()
        val toolContext = ToolContext(
            repository = repository,
            bridge = bridge,
            fileGenerationPipeline = pipeline
        )

        // Attempt without overwrite permission
        val result = tool.execute("save as csv called budget.csv with col1,col2", toolContext)
        assertFalse(result.success)
        assertTrue(result.requiresUserAction)
        assertTrue(result.output.contains("already exists"))

        // Verify RiskEngine flags explicit overwrite as CONFIRMATION risk level
        val assessment = RiskEngine.assessAction("FileGeneration", "overwrite existing file budget.csv")
        assertEquals(RiskLevel.CONFIRMATION, assessment.level)
        assertTrue(assessment.isPermitted)
    }

    // 10. SENSITIVE DATA NOT LOGGED IN PLAIN TEXT
    @Test
    fun testSensitiveDataWarningAndMasking() = runBlocking {
        val request = GenerationRequest(
            fileName = "credentials_summary.txt",
            format = GeneratedFileFormat.TXT,
            title = "API Credential Export",
            rawContent = "AIzaSySecretTokenKey1234567890",
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.warnings.isNotEmpty())

        val sanitized = SensitiveDataFilter.sanitizeForDisplay(result.fileName)
        assertEquals("credentials_summary.txt", sanitized)
    }

    // 11. PDF GENERATION & VERIFICATION
    @Test
    fun testPdfGenerationAndVerification() = runBlocking {
        val request = GenerationRequest(
            fileName = "system_report.pdf",
            format = GeneratedFileFormat.PDF,
            title = "JARVIS System Report",
            sections = listOf(
                GenerationSection(
                    title = "Telemetry Overview",
                    content = "System CPU nominal at 14%. Memory usage 3.2GB / 8.0GB.",
                    items = listOf("Battery: 95%", "Network: WiFi Connected")
                )
            ),
            tables = listOf(
                GenerationTable(
                    title = "Module Status",
                    headers = listOf("Module", "State", "Uptime"),
                    rows = listOf(
                        listOf("Neural Brain", "ACTIVE", "99.9%"),
                        listOf("File Pipeline", "ACTIVE", "100.0%")
                    )
                )
            ),
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertNotNull(result.file)
        assertTrue(result.file!!.exists())
        assertTrue(result.file!!.length() > 0)
        assertEquals("application/pdf", result.mimeType)

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.PDF)
        assertTrue(ver.isValid)
    }

    // 12. DOCX GENERATION & VERIFICATION
    @Test
    fun testDocxGenerationAndVerification() = runBlocking {
        val request = GenerationRequest(
            fileName = "executive_briefing.docx",
            format = GeneratedFileFormat.DOCX,
            title = "Executive Briefing",
            sections = listOf(
                GenerationSection(
                    title = "Key Takeaways",
                    content = "JARVIS core operations are operating at full capacity.",
                    items = listOf("Zero unhandled exceptions", "100% verified output files")
                )
            ),
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertNotNull(result.file)
        assertTrue(result.file!!.exists())
        assertTrue(result.file!!.length() > 0)

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.DOCX)
        assertTrue(ver.isValid)
    }

    // 13. XLSX GENERATION & VERIFICATION
    @Test
    fun testXlsxGenerationAndVerification() = runBlocking {
        val request = GenerationRequest(
            fileName = "sensor_data.xlsx",
            format = GeneratedFileFormat.XLSX,
            tables = listOf(
                GenerationTable(
                    title = "Sensor Logs",
                    headers = listOf("Metric", "Value", "Unit"),
                    rows = listOf(
                        listOf("Battery", "95", "%"),
                        listOf("Temperature", "34.5", "C")
                    )
                )
            ),
            targetDirectory = testOutputDir
        )

        val result = pipeline.generateFile(request)
        assertTrue(result.success)
        assertTrue(result.verified)
        assertNotNull(result.file)
        assertTrue(result.file!!.exists())
        assertTrue(result.file!!.length() > 0)

        val ver = FileVerifier.verify(result.file!!, GeneratedFileFormat.XLSX)
        assertTrue(ver.isValid)
    }

    // 14. LOCAL NEURAL BRAIN TOOL DECISION ROUTING
    @Test
    fun testLocalBrainToolDecisionRouting() {
        val queries = listOf(
            "save as csv called data.csv" to "FileGeneration",
            "create a markdown file named readme.md" to "FileGeneration",
            "save as txt called log.txt" to "FileGeneration",
            "generate a json file with telemetry" to "FileGeneration",
            "generate a pdf report called summary.pdf" to "FileGeneration"
        )

        for ((query, expectedTool) in queries) {
            val decision = LocalNeuralBrainProvider.decideToolLocal(query, emptyList())
            assertTrue("Query '$query' should trigger tool $expectedTool", decision.useTool)
            assertEquals(expectedTool, decision.toolName)
        }
    }
}
