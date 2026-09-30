package com.example.jarvis.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.jarvis.provider.AIProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

interface VisionEngine {
    suspend fun processImage(context: Context, uri: Uri): VisionResult
    suspend fun processBitmap(bitmap: Bitmap, metadata: ImageMetadata? = null): VisionResult
    suspend fun summarizeText(extractedText: String, aiProvider: AIProvider?): String
    suspend fun answerQuestion(extractedText: String, question: String, aiProvider: AIProvider?): String
    suspend fun translateText(extractedText: String, targetLanguage: String, aiProvider: AIProvider?): String
}

class JarvisVisionEngine : VisionEngine {

    // Latin recognizer (English + extended Latin characters)
    private val defaultRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Devanagari recognizer (Hindi / Sanskrit / related Indic scripts)
    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    override suspend fun processImage(context: Context, uri: Uri): VisionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Extract metadata & verify URI
        val metadata = extractImageMetadata(context, uri)
            ?: return@withContext VisionResult.failure("Unable to access or inspect image file. Ensure file exists.", null)

        // 2. Safely decode downsampled Bitmap
        val bitmap = loadSafeBitmap(context, uri, maxDimension = 1600)
            ?: return@withContext VisionResult.failure("Could not decode image bitmap from device storage.", metadata)

        try {
            val result = executeOcr(bitmap, metadata, startTime)
            result
        } finally {
            // Bitmap recycling can be handled or left to GC
        }
    }

    override suspend fun processBitmap(bitmap: Bitmap, metadata: ImageMetadata?): VisionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val meta = metadata ?: ImageMetadata(
            width = bitmap.width,
            height = bitmap.height,
            fileName = "Bitmap_Frame_${System.currentTimeMillis()}"
        )
        executeOcr(bitmap, meta, startTime)
    }

    private suspend fun executeOcr(bitmap: Bitmap, metadata: ImageMetadata, startTime: Long): VisionResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Primary OCR pass (Latin / Standard)
            var mlkitResult = defaultRecognizer.process(inputImage).await()
            var extractedText = mlkitResult.text.trim()

            // If empty or if script might be Indic/Devanagari, perform secondary pass
            if (extractedText.isBlank()) {
                try {
                    val devanagariResult = devanagariRecognizer.process(inputImage).await()
                    if (devanagariResult.text.isNotBlank()) {
                        mlkitResult = devanagariResult
                        extractedText = devanagariResult.text.trim()
                    }
                } catch (_: Exception) {
                    // Ignore secondary pass errors
                }
            }

            val elapsed = System.currentTimeMillis() - startTime

            if (extractedText.isBlank()) {
                return VisionResult(
                    success = true,
                    extractedText = "",
                    blocks = emptyList(),
                    metadata = metadata,
                    processingTimeMs = elapsed,
                    errorMessage = "Optical inspection complete. No readable text characters were detected in this frame."
                )
            }

            // Map ML Kit blocks
            val structuredBlocks = mlkitResult.textBlocks.map { block ->
                val lines = block.lines.map { line ->
                    VisionTextLine(
                        text = line.text,
                        confidence = line.confidence,
                        boundingBox = line.boundingBox,
                        elements = line.elements.map { it.text },
                        recognizedLanguage = line.recognizedLanguage
                    )
                }
                VisionTextBlock(
                    text = block.text,
                    confidence = null,
                    boundingBox = block.boundingBox,
                    cornerPoints = block.cornerPoints?.map { Pair(it.x, it.y) } ?: emptyList(),
                    lines = lines,
                    recognizedLanguage = block.recognizedLanguage
                )
            }

            // Sensitive data inspection
            val sensitiveEntities = SensitiveDataFilter.detectSensitiveEntities(extractedText)
            val detectedLang = detectPrimaryLanguage(extractedText)

            VisionResult(
                success = true,
                extractedText = extractedText,
                blocks = structuredBlocks,
                confidence = null,
                metadata = metadata,
                processingTimeMs = elapsed,
                detectedLanguage = detectedLang,
                containsSensitiveData = sensitiveEntities.isNotEmpty(),
                sensitiveEntitiesDetected = sensitiveEntities
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            VisionResult(
                success = false,
                extractedText = "",
                blocks = emptyList(),
                metadata = metadata,
                processingTimeMs = elapsed,
                errorMessage = "On-device OCR engine error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    override suspend fun summarizeText(extractedText: String, aiProvider: AIProvider?): String = withContext(Dispatchers.IO) {
        if (extractedText.isBlank()) return@withContext "No text available to summarize."

        if (aiProvider != null) {
            val prompt = """
                Summarize the following text extracted from an image or screenshot clearly and concisely for executive review:
                
                $extractedText
            """.trimIndent()
            try {
                return@withContext aiProvider.generateResponse(prompt, "You are JARVIS. Summarize key facts cleanly.") {}
            } catch (_: Exception) {
                // Fallback to local summarization
            }
        }

        // Local extractive summarization
        val lines = extractedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext "No legible lines found."
        
        buildString {
            appendLine("DOCUMENT / IMAGE TEXT SUMMARY:")
            appendLine("• Total Lines Detected: ${lines.size}")
            appendLine("• Word Count: ${extractedText.split(Regex("\\s+")).size}")
            appendLine("• Key Content Snippet:")
            lines.take(5).forEach { appendLine("  - $it") }
            if (lines.size > 5) {
                appendLine("  ... [${lines.size - 5} additional lines omitted]")
            }
        }
    }

    override suspend fun answerQuestion(
        extractedText: String,
        question: String,
        aiProvider: AIProvider?
    ): String = withContext(Dispatchers.IO) {
        if (extractedText.isBlank()) {
            return@withContext "I cannot answer about this image because no text was extracted from it."
        }

        val sanitizedText = if (SensitiveDataFilter.containsSensitiveData(extractedText)) {
            SensitiveDataFilter.redactSensitiveData(extractedText)
        } else {
            extractedText
        }

        if (aiProvider != null) {
            val prompt = """
                The user has selected an image. The following text was extracted from it via on-device OCR:
                ---
                $sanitizedText
                ---
                The user asks: "$question"
                
                Answer the user's question accurately and objectively using only the information in the extracted text. If the text does not contain the answer, say so directly.
            """.trimIndent()

            try {
                return@withContext aiProvider.generateResponse(
                    prompt = prompt,
                    systemInstruction = "You are JARVIS. Answer queries regarding extracted image content precisely."
                ) {}
            } catch (_: Exception) {
                // Fallback to local search
            }
        }

        // Local heuristic keyword search
        val qTokens = question.lowercase(Locale.ROOT).split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 }
        val matchingLines = extractedText.lines().filter { line ->
            val lLower = line.lowercase(Locale.ROOT)
            qTokens.any { token -> lLower.contains(token) }
        }

        if (matchingLines.isNotEmpty()) {
            "Based on the image text, the following relevant information was identified:\n" +
                    matchingLines.joinToString("\n") { "• $it" }
        } else {
            "I analyzed the text from your image, but could not locate specific details answering: \"$question\". Here is the extracted text preview:\n${extractedText.take(200)}..."
        }
    }

    override suspend fun translateText(
        extractedText: String,
        targetLanguage: String,
        aiProvider: AIProvider?
    ): String = withContext(Dispatchers.IO) {
        if (extractedText.isBlank()) return@withContext "No text available to translate."

        if (aiProvider != null) {
            val prompt = """
                Translate the following text extracted from an image into $targetLanguage accurately and naturally:
                ---
                $extractedText
                ---
            """.trimIndent()
            try {
                return@withContext aiProvider.generateResponse(prompt, "You are JARVIS translator. Return only the translated text.") {}
            } catch (_: Exception) {
                // Fallback
            }
        }

        if (targetLanguage.contains("bengali", ignoreCase = true) || targetLanguage.contains("বাংলা", ignoreCase = true)) {
            "অনুবাদ প্রস্তুত করার জন্য সক্রিয় AI সংযোগ প্রয়োজন। প্রাপ্ত মূল পাঠ্য:\n$extractedText"
        } else {
            "Translation to $targetLanguage requires an active AI provider connection. Original text:\n$extractedText"
        }
    }

    private fun detectPrimaryLanguage(text: String): String {
        var devanagariCount = 0
        var bengaliCount = 0
        var latinCount = 0

        for (ch in text) {
            val code = ch.code
            when (code) {
                in 0x0900..0x097F -> devanagariCount++
                in 0x0980..0x09FF -> bengaliCount++
                in 0x0041..0x005A, in 0x0061..0x007A -> latinCount++
            }
        }

        return when {
            bengaliCount > 5 && bengaliCount >= devanagariCount -> "Bengali (বাংলা)"
            devanagariCount > 5 && devanagariCount >= bengaliCount -> "Hindi (हिंदी)"
            latinCount > 0 -> "English / Latin"
            else -> "Multilingual"
        }
    }

    private fun extractImageMetadata(context: Context, uri: Uri): ImageMetadata? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val pfd = contentResolver.openFileDescriptor(uri, "r")
            val sizeBytes = pfd?.statSize ?: 0L
            pfd?.close()

            val sizeFormatted = when {
                sizeBytes > 1024 * 1024 -> String.format(Locale.ROOT, "%.2f MB", sizeBytes / (1024f * 1024f))
                sizeBytes > 0 -> "${sizeBytes / 1024} KB"
                else -> "Unknown size"
            }

            val fileName = uri.lastPathSegment ?: "IMG_${System.currentTimeMillis()}.jpg"

            ImageMetadata(
                width = options.outWidth,
                height = options.outHeight,
                mimeType = mimeType,
                fileSizeFormatted = sizeFormatted,
                uriString = uri.toString(),
                fileName = fileName
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadSafeBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var stream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            var sampleSize = 1
            val maxSide = maxOf(options.outWidth, options.outHeight)
            while (maxSide / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            stream = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(stream, null, decodeOptions)
            stream?.close()
            bmp
        } catch (_: Exception) {
            null
        }
    }
}
