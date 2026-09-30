package com.example.jarvis.translation

import com.example.jarvis.code.CodeSecurityScanner
import com.example.jarvis.provider.AIProvider
import com.example.jarvis.recovery.ResponseRepair
import com.example.jarvis.vision.SensitiveDataFilter
import com.example.jarvis.voice.LanguageDetector
import java.util.Locale

/**
 * Dedicated, privacy-respecting Translation Engine for JARVIS.
 * Handles language detection, structure preservation, formatting masking,
 * secure AI translation, and offline fallbacks.
 */
object TranslationEngine {

    suspend fun translate(
        request: TranslationRequest,
        aiProvider: AIProvider? = null
    ): TranslationResult {
        val rawText = request.sourceText.trim()
        if (rawText.isEmpty()) {
            return TranslationResult(
                translatedText = "",
                sourceLanguage = request.sourceLanguage,
                targetLanguage = request.targetLanguage,
                success = false,
                errorMessage = "Translation input text cannot be empty."
            )
        }

        // 1. Resolve Target Language
        val targetCode = SupportedLanguage.resolveTargetCode(request.targetLanguage, defaultCode = "en")

        // 2. Resolve Source Language
        val detectedSource = if (request.sourceLanguage.equals("auto", ignoreCase = true) || request.sourceLanguage.isBlank()) {
            detectSourceLanguageCode(rawText)
        } else {
            SupportedLanguage.resolveTargetCode(request.sourceLanguage, defaultCode = "auto")
        }

        // 3. Security & Sensitive Data Check (Privacy Protection)
        val sensitiveEntities = SensitiveDataFilter.detectSensitiveEntities(rawText)
        val secretFindings = CodeSecurityScanner.scanForSecrets(rawText)
        val containsCriticalSecrets = secretFindings.isNotEmpty() || sensitiveEntities.isNotEmpty()

        // Sanitize credentials so raw secrets are never sent to external AI
        val (sanitizedInput, _) = if (containsCriticalSecrets) {
            CodeSecurityScanner.sanitizeForAiTransmission(rawText)
        } else {
            Pair(rawText, emptyList())
        }

        // 4. Check Identical Source & Target Language
        if (detectedSource.equals(targetCode, ignoreCase = true) && detectedSource != "auto") {
            return TranslationResult(
                translatedText = rawText,
                sourceLanguage = detectedSource,
                targetLanguage = targetCode,
                qualityIndicator = "Identical language passthrough",
                success = true,
                metadata = if (containsCriticalSecrets) mapOf("privacyWarning" to "Sensitive data detected and protected") else emptyMap()
            )
        }

        // 5. Offline Fallback Attempt
        val offlineResult = OfflineTranslationFallback.attemptOfflineTranslation(
            text = sanitizedInput,
            sourceLang = detectedSource,
            targetLang = targetCode
        )
        if (offlineResult != null) {
            return offlineResult.copy(
                metadata = if (containsCriticalSecrets) mapOf("privacyWarning" to "Sensitive data detected and protected") else emptyMap()
            )
        }

        // 6. AI Translation Check
        if (aiProvider == null) {
            return OfflineTranslationFallback.createOfflineUnavailableResult(
                sourceLang = detectedSource,
                targetLang = targetCode
            )
        }

        // 7. Formatting & Code Block Preservation (Masking)
        val masked = if (request.preserveFormatting) {
            FormattingPreserver.maskFormatters(sanitizedInput)
        } else {
            FormattingPreserver.MaskedContent(sanitizedInput, emptyMap())
        }

        val srcName = SupportedLanguage.getDisplayName(detectedSource)
        val tgtName = SupportedLanguage.getDisplayName(targetCode)

        val systemInstruction = """
            You are a professional, high-fidelity translation engine.
            Your task is to translate the supplied text from $srcName to $tgtName.
            
            MANDATORY RULES:
            1. Output ONLY the translated text. Do NOT include greetings, preamble, explanations, notes, or conversational chitchat.
            2. Preserve the exact paragraph, newline, and indentation structure.
            3. Preserve all Markdown elements (headers, bold, italics, lists, tables).
            4. Do NOT translate or modify placeholder tokens like __CODE_BLOCK_0__, __URL_0__, __EMAIL_0__, __PLACEHOLDER_0__, __INLINE_CODE_0__. Keep them verbatim in their natural grammatical positions.
            5. Ensure natural grammar, idiomatic nuance, and polite tone in $tgtName.
        """.trimIndent()

        val prompt = """
            Source Language: $srcName
            Target Language: $tgtName
            
            Text to translate:
            ${masked.maskedText}
        """.trimIndent()

        return try {
            val response = aiProvider.generateResponse(
                prompt = prompt,
                systemInstruction = systemInstruction,
                onChunkReceived = {}
            )

            val cleanOutput = cleanAiTranslationOutput(response)

            // 8. Restore Formatting & Code Blocks
            val restoredOutput = if (request.preserveFormatting) {
                FormattingPreserver.restoreFormatters(cleanOutput, masked.tokenMap)
            } else {
                cleanOutput
            }

            if (restoredOutput.isBlank()) {
                TranslationResult(
                    translatedText = "",
                    sourceLanguage = detectedSource,
                    targetLanguage = targetCode,
                    success = false,
                    errorMessage = "Translation provider returned empty output."
                )
            } else {
                val metadata = mutableMapOf<String, String>()
                metadata["provider"] = "AIProvider"
                if (containsCriticalSecrets) {
                    metadata["privacyAlert"] = "Input contained sensitive tokens that were safely sanitized prior to translation."
                }

                TranslationResult(
                    translatedText = restoredOutput,
                    sourceLanguage = detectedSource,
                    targetLanguage = targetCode,
                    qualityIndicator = "High-fidelity AI Translation",
                    success = true,
                    metadata = metadata
                )
            }
        } catch (e: Exception) {
            // Check if deterministic offline fallback can satisfy any portion
            val fallback = OfflineTranslationFallback.attemptOfflineTranslation(sanitizedInput, detectedSource, targetCode)
            if (fallback != null) {
                fallback
            } else {
                TranslationResult(
                    translatedText = "",
                    sourceLanguage = detectedSource,
                    targetLanguage = targetCode,
                    success = false,
                    errorMessage = "Translation failed: ${e.message ?: "AI Provider error"}"
                )
            }
        }
    }

    /**
     * Inspects text using language detector and character ranges to identify language.
     */
    fun detectSourceLanguageCode(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "en"

        // 1. Check for Assamese-specific Unicode letters (ৰ \u09F0, ৱ \u09F1)
        if (trimmed.any { it == '\u09F0' || it == '\u09F1' }) {
            return "as"
        }

        // 2. Bengali script (\u0980 - \u09FF)
        if (trimmed.any { it in '\u0980'..'\u09FF' }) {
            return "bn"
        }

        // 3. Arabic / Urdu script (\u0600 - \u06FF, \u0750 - \u077F, \uFB50 - \uFDFF, \uFE70 - \uFEFF)
        if (trimmed.any { it in '\u0600'..'\u06FF' || it in '\uFB50'..'\uFDFF' }) {
            return "ur"
        }

        // 4. Devanagari script (\u0900 - \u097F)
        if (trimmed.any { it in '\u0900'..'\u097F' }) {
            // Check for Nepali-specific markers
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.contains("छ") || lower.contains("तपाईं") || lower.contains("हुनुहुन्छ") || lower.contains("नमस्ते")) {
                // If contains nepali markers
                if (lower.contains("तपाईं") || lower.contains("कस्तो") || lower.contains("छन्")) {
                    return "ne"
                }
            }
            return "hi"
        }

        // 5. Use existing LanguageDetector for phonetic transliterations
        val detected = LanguageDetector.detectLanguage(trimmed)
        return when (detected) {
            LanguageDetector.DetectedLanguage.BENGALI -> "bn"
            LanguageDetector.DetectedLanguage.HINDI -> "hi"
            LanguageDetector.DetectedLanguage.ENGLISH -> "en"
        }
    }

    /**
     * Cleans AI translation output, stripping unintended markdown code wrappers or explanations.
     */
    private fun cleanAiTranslationOutput(raw: String): String {
        var text = raw.trim()
        // If AI wrapped the entire translation in ```...```, remove wrapper
        if (text.startsWith("```") && text.endsWith("```")) {
            val lines = text.lines()
            if (lines.size >= 2) {
                text = lines.subList(1, lines.size - 1).joinToString("\n").trim()
            }
        }
        // Remove common intro chitchat prefixes if present
        val chitchatPrefixes = listOf(
            "Here is the translation:",
            "Translation:",
            "Translated text:",
            "Here's the translation:"
        )
        for (prefix in chitchatPrefixes) {
            if (text.startsWith(prefix, ignoreCase = true)) {
                text = text.substring(prefix.length).trim()
            }
        }

        return text
    }
}
