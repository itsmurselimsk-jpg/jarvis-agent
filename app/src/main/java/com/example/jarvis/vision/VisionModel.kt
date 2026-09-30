package com.example.jarvis.vision

import android.graphics.Rect

/**
 * Structured block of recognized text in an image.
 */
data class VisionTextBlock(
    val text: String,
    val confidence: Float? = null,
    val boundingBox: Rect? = null,
    val cornerPoints: List<Pair<Int, Int>> = emptyList(),
    val lines: List<VisionTextLine> = emptyList(),
    val recognizedLanguage: String? = null
)

/**
 * Structured line of recognized text in a block.
 */
data class VisionTextLine(
    val text: String,
    val confidence: Float? = null,
    val boundingBox: Rect? = null,
    val elements: List<String> = emptyList(),
    val recognizedLanguage: String? = null
)

/**
 * Metadata about the scanned image.
 */
data class ImageMetadata(
    val width: Int,
    val height: Int,
    val mimeType: String? = null,
    val fileSizeFormatted: String? = null,
    val uriString: String? = null,
    val fileName: String? = null
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f
}

/**
 * Result produced by OCR and Vision processing.
 */
data class VisionResult(
    val success: Boolean,
    val extractedText: String,
    val blocks: List<VisionTextBlock> = emptyList(),
    val confidence: Float? = null,
    val metadata: ImageMetadata? = null,
    val processingTimeMs: Long = 0L,
    val errorMessage: String? = null,
    val detectedLanguage: String? = null,
    val containsSensitiveData: Boolean = false,
    val sensitiveEntitiesDetected: List<String> = emptyList(),
    val summary: String? = null
) {
    companion object {
        fun failure(message: String, metadata: ImageMetadata? = null): VisionResult {
            return VisionResult(
                success = false,
                extractedText = "",
                blocks = emptyList(),
                metadata = metadata,
                errorMessage = message
            )
        }

        fun empty(metadata: ImageMetadata? = null): VisionResult {
            return VisionResult(
                success = true,
                extractedText = "",
                blocks = emptyList(),
                metadata = metadata,
                errorMessage = "No legible text was detected in the frame."
            )
        }
    }
}

/**
 * Action suggestion derived from extracted text (e.g. phone number, URL, email).
 */
data class VisionDerivedAction(
    val type: VisionActionType,
    val label: String,
    val payload: String,
    val isSensitiveOrDestructive: Boolean
) {
    val isSensitive: Boolean
        get() = isSensitiveOrDestructive
}

enum class VisionActionType {
    DIAL_PHONE,
    SEARCH_PHONE,
    OPEN_URL,
    SEND_EMAIL,
    COPY_TEXT
}
