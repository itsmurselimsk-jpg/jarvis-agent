package com.example.jarvis.generation

import android.content.Context
import com.example.jarvis.security.SensitiveDataFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Orchestrates file generation across all supported formats:
 * TXT, Markdown (.md), CSV, JSON, PDF, DOCX, XLSX.
 * Enforces:
 * - Clean format routing
 * - Strict verification before claiming success
 * - Overwrite protection (never overwrite without explicit permission)
 * - Safe app-specific scoped storage
 * - Redaction of sensitive credentials during logging
 * - Never executes generated files or code within them
 */
class FileGenerationPipeline(
    private val context: Context,
    private val baseDirectory: File = File(context.filesDir, "generated_files")
) {
    private val generators: Map<GeneratedFileFormat, FileGenerator> = mapOf(
        GeneratedFileFormat.TXT to TxtFileGenerator(),
        GeneratedFileFormat.MARKDOWN to MarkdownFileGenerator(),
        GeneratedFileFormat.JSON to JsonFileGenerator(),
        GeneratedFileFormat.CSV to CsvFileGenerator(),
        GeneratedFileFormat.PDF to PdfFileGenerator(),
        GeneratedFileFormat.DOCX to DocxFileGenerator(),
        GeneratedFileFormat.XLSX to XlsxFileGenerator()
    )

    init {
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs()
        }
    }

    /**
     * Executes file generation and verifies the output.
     */
    suspend fun generateFile(request: GenerationRequest): GenerationResult = withContext(Dispatchers.IO) {
        val format = request.format
        val generator = generators[format]
            ?: return@withContext GenerationResult(
                success = false,
                fileName = request.fileName,
                format = format,
                mimeType = format.mimeType,
                verified = false,
                errors = listOf("Unsupported output format: ${format.name}"),
                message = "Generation failed: No generator available for format ${format.name}."
            )

        // 1. Sanitize file name
        val sanitizedFileName = sanitizeFileName(request.fileName, format)

        // 2. Resolve target storage directory (app-scoped)
        val targetDir = request.targetDirectory ?: baseDirectory
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val targetFile = File(targetDir, sanitizedFileName)

        // 3. Overwrite protection
        if (targetFile.exists() && !request.allowOverwrite) {
            return@withContext GenerationResult(
                success = false,
                fileName = sanitizedFileName,
                format = format,
                mimeType = format.mimeType,
                file = targetFile,
                absolutePath = targetFile.absolutePath,
                sizeBytes = targetFile.length(),
                verified = false,
                errors = listOf("File '${sanitizedFileName}' already exists and overwrite protection is active."),
                message = "Overwrite protection active: A file named '$sanitizedFileName' already exists. User confirmation required before replacing."
            )
        }

        // 4. Generate byte content
        val rawBytes = try {
            generator.generate(request)
        } catch (e: Exception) {
            return@withContext GenerationResult(
                success = false,
                fileName = sanitizedFileName,
                format = format,
                mimeType = format.mimeType,
                verified = false,
                errors = listOf("Generator failure: ${e.message ?: "Unknown generation exception"}"),
                message = "Failed to generate $sanitizedFileName: ${e.message}"
            )
        }

        if (rawBytes.isEmpty()) {
            return@withContext GenerationResult(
                success = false,
                fileName = sanitizedFileName,
                format = format,
                mimeType = format.mimeType,
                verified = false,
                errors = listOf("Generator produced 0 bytes."),
                message = "Failed to generate $sanitizedFileName: Generated content is empty."
            )
        }

        // 5. Atomic write to temporary file then replace
        val tempFile = File(targetDir, "$sanitizedFileName.tmp_${System.currentTimeMillis()}")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(rawBytes)
                fos.flush()
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                // Fallback copy if atomic rename fails on some storage mounts
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            return@withContext GenerationResult(
                success = false,
                fileName = sanitizedFileName,
                format = format,
                mimeType = format.mimeType,
                verified = false,
                errors = listOf("I/O write failure: ${e.message}"),
                message = "Storage write error while saving $sanitizedFileName: ${e.message}"
            )
        }

        // 6. Verification: MUST NOT claim success unless verification passes
        val verification = FileVerifier.verify(targetFile, format)
        if (!verification.isValid) {
            // Delete corrupt file
            if (targetFile.exists()) targetFile.delete()
            return@withContext GenerationResult(
                success = false,
                fileName = sanitizedFileName,
                format = format,
                mimeType = format.mimeType,
                verified = false,
                verificationDetails = verification.details,
                errors = verification.errors,
                message = "File verification failed for $sanitizedFileName: ${verification.details}"
            )
        }

        // Check for sensitive data warnings (for safe reporting)
        val warnings = mutableListOf<String>()
        if (!format.isBinary) {
            val contentStr = String(rawBytes, Charsets.UTF_8)
            if (SensitiveDataFilter.containsSensitiveData(contentStr)) {
                warnings.add("File contains protected tokens or sensitive patterns.")
            }
        }

        GenerationResult(
            success = true,
            fileName = sanitizedFileName,
            format = format,
            mimeType = format.mimeType,
            file = targetFile,
            absolutePath = targetFile.absolutePath,
            sizeBytes = targetFile.length(),
            verified = true,
            verificationDetails = verification.details,
            warnings = warnings,
            message = "Successfully created and verified $sanitizedFileName (${targetFile.length()} bytes, ${format.name})."
        )
    }

    /**
     * Checks if a file already exists in the target storage.
     */
    fun fileExists(fileName: String, targetDir: File? = null): Boolean {
        val dir = targetDir ?: baseDirectory
        val file = File(dir, fileName)
        return file.exists()
    }

    /**
     * Sanitizes file names to prevent directory traversal or invalid characters.
     */
    fun sanitizeFileName(rawName: String, format: GeneratedFileFormat): String {
        var clean = rawName.trim().replace(Regex("""[\\/:\*\?"<>\|]"""), "_")
        if (clean.isBlank()) {
            clean = "document_${System.currentTimeMillis()}"
        }
        val expectedExt = format.extension.lowercase(Locale.ROOT)
        return if (!clean.lowercase(Locale.ROOT).endsWith(".$expectedExt")) {
            "$clean.$expectedExt"
        } else {
            clean
        }
    }
}
