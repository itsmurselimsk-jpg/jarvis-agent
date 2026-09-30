package com.example.jarvis.brain

import com.example.jarvis.model.RiskLevel
import com.example.jarvis.provider.JarvisUnifiedAIProvider
import com.example.jarvis.translation.SupportedLanguage
import com.example.jarvis.translation.TranslationEngine
import com.example.jarvis.translation.TranslationRequest
import java.util.Locale

/**
 * Dedicated Translation Tool registered with AgentBrain.
 * Safely translates user queries or attached document/vision text between languages.
 */
class TranslationTool : Tool {

    override val name: String = "Translation"
    override val description: String = "Translates text between English, Bengali, Hindi, Urdu, Assamese, Nepali, and other languages while preserving Markdown formatting, code blocks, URLs, and placeholders."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val permissions: List<String> = emptyList()

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) {
            return ToolResult(
                success = false,
                output = "Please provide the text you would like to translate and the target language (e.g., 'Translate to Bengali: Hello world')."
            )
        }

        val (sourceLang, targetLang, textToTranslate) = parseTranslationIntent(trimmedInput, context)

        if (textToTranslate.isBlank()) {
            return ToolResult(
                success = false,
                output = "No text found to translate. Please specify the text to translate (e.g., 'Translate to Hindi: Good morning')."
            )
        }

        // Get AI Provider from context or create unified provider from repository
        val aiProvider = context.aiProvider ?: JarvisUnifiedAIProvider(context.repository)

        val request = TranslationRequest(
            sourceText = textToTranslate,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            preserveFormatting = true
        )

        val result = TranslationEngine.translate(request, aiProvider)

        return if (result.success) {
            val srcName = SupportedLanguage.getDisplayName(result.sourceLanguage)
            val tgtName = SupportedLanguage.getDisplayName(result.targetLanguage)
            val formattedOutput = buildString {
                appendLine("🌐 **Translation ($srcName → $tgtName)**")
                appendLine()
                appendLine(result.translatedText)
                if (result.qualityIndicator != null && result.qualityIndicator != "High-fidelity AI Translation") {
                    appendLine()
                    appendLine("*(Mode: ${result.qualityIndicator})*")
                }
                if (result.metadata.containsKey("privacyAlert")) {
                    appendLine()
                    appendLine("🛡️ *Privacy note: Detected sensitive tokens were safely protected before processing.*")
                }
            }.trimEnd()

            ToolResult(
                success = true,
                output = formattedOutput,
                metadata = result.metadata
            )
        } else {
            ToolResult(
                success = false,
                output = "Translation failed: ${result.errorMessage ?: "Unknown error"}"
            )
        }
    }

    private data class ParsedIntent(
        val sourceLang: String,
        val targetLang: String,
        val text: String
    )

    /**
     * Extracts source language, target language, and text from a natural language request.
     */
    private fun parseTranslationIntent(input: String, context: ToolContext): ParsedIntent {
        var text = input
        var sourceLang = "auto"
        var targetLang = "en"

        // 1. Check if user wants to translate attached document or OCR text
        val lower = input.lowercase(Locale.ROOT)
        if (lower.contains("this document") || lower.contains("attached document") || lower.contains("the file")) {
            val doc = context.activeDocument
            if (doc != null && doc.extractedText.isNotBlank()) {
                text = doc.extractedText
            }
        } else if (lower.contains("this image") || lower.contains("scanned text") || lower.contains("the photo")) {
            val vision = context.activeVisionResult
            if (vision != null && vision.extractedText.isNotBlank()) {
                text = vision.extractedText
            }
        }

        // 2. Pattern: "Translate from X to Y: TEXT"
        val fromToRegex = Regex("""(?i)(?:translate|convert)\s+from\s+([a-zA-Z\u0980-\u09FF\u0900-\u097F]+)\s+(?:to|into)\s+([a-zA-Z\u0980-\u09FF\u0900-\u097F]+)(?:\s*[:\-]\s*|\s+)(.*)""", RegexOption.DOT_MATCHES_ALL)
        val fromToMatch = fromToRegex.find(input)
        if (fromToMatch != null) {
            val src = fromToMatch.groupValues[1].trim()
            val tgt = fromToMatch.groupValues[2].trim()
            val remaining = fromToMatch.groupValues[3].trim()
            return ParsedIntent(
                sourceLang = SupportedLanguage.resolveTargetCode(src, defaultCode = "auto"),
                targetLang = SupportedLanguage.resolveTargetCode(tgt, defaultCode = "en"),
                text = remaining.ifBlank { text }
            )
        }

        // 3. Pattern: "Translate (this )?to/into Y: TEXT" or "Translate to Y TEXT"
        val toRegex = Regex("""(?i)(?:translate|convert)(?:\s+this)?\s+(?:to|into|in)\s+([a-zA-Z\u0980-\u09FF\u0900-\u097F]+)(?:\s*[:\-]\s*|\s+)(.*)""", RegexOption.DOT_MATCHES_ALL)
        val toMatch = toRegex.find(input)
        if (toMatch != null) {
            val tgt = toMatch.groupValues[1].trim()
            val remaining = toMatch.groupValues[2].trim()
            return ParsedIntent(
                sourceLang = "auto",
                targetLang = SupportedLanguage.resolveTargetCode(tgt, defaultCode = "en"),
                text = remaining.ifBlank { text }
            )
        }

        // 4. Pattern: Bengali "এটা [ভাষা]-তে অনুবাদ করো: TEXT" or "[ভাষা]-য় অনুবাদ করো"
        val bnRegex = Regex("""(?i)(?:এটা\s+)?([a-zA-Z\u0980-\u09FF]+)(?:-তে|-এ|য়| তে)?\s+অনুবাদ\s+করো(?:\s*[:\-]\s*|\s+)?(.*)""", RegexOption.DOT_MATCHES_ALL)
        val bnMatch = bnRegex.find(input)
        if (bnMatch != null) {
            val tgtRaw = bnMatch.groupValues[1].trim()
            val remaining = bnMatch.groupValues[2].trim()
            val tgt = when {
                tgtRaw.contains("ইংরেজি") || tgtRaw.contains("english") -> "en"
                tgtRaw.contains("বাংলা") || tgtRaw.contains("bengali") -> "bn"
                tgtRaw.contains("হিন্দি") || tgtRaw.contains("hindi") -> "hi"
                tgtRaw.contains("উর্দু") || tgtRaw.contains("urdu") -> "ur"
                tgtRaw.contains("অসমীয়া") || tgtRaw.contains("assamese") -> "as"
                tgtRaw.contains("নেপালি") || tgtRaw.contains("nepali") -> "ne"
                else -> SupportedLanguage.resolveTargetCode(tgtRaw, defaultCode = "en")
            }
            return ParsedIntent(
                sourceLang = "auto",
                targetLang = tgt,
                text = remaining.ifBlank { text }
            )
        }

        // 5. Pattern: Hinglish "Isko [language] mein translate karo: TEXT"
        val hiRegex = Regex("""(?i)(?:isko|ise)\s+([a-zA-Z\u0900-\u097F]+)\s+mein\s+translate\s+karo(?:\s*[:\-]\s*|\s+)?(.*)""", RegexOption.DOT_MATCHES_ALL)
        val hiMatch = hiRegex.find(input)
        if (hiMatch != null) {
            val tgt = hiMatch.groupValues[1].trim()
            val remaining = hiMatch.groupValues[2].trim()
            return ParsedIntent(
                sourceLang = "auto",
                targetLang = SupportedLanguage.resolveTargetCode(tgt, defaultCode = "en"),
                text = remaining.ifBlank { text }
            )
        }

        // 6. Pattern: "What does this mean in [language]?: TEXT"
        val meanRegex = Regex("""(?i)what\s+does\s+(?:this|it)\s+mean\s+in\s+([a-zA-Z]+)(?:\s*\??\s*[:\-]?\s*)(.*)""", RegexOption.DOT_MATCHES_ALL)
        val meanMatch = meanRegex.find(input)
        if (meanMatch != null) {
            val tgt = meanMatch.groupValues[1].trim()
            val remaining = meanMatch.groupValues[2].trim()
            return ParsedIntent(
                sourceLang = "auto",
                targetLang = SupportedLanguage.resolveTargetCode(tgt, defaultCode = "en"),
                text = remaining.ifBlank { text }
            )
        }

        // 7. General fallback: if input starts with "Translate:" or "Translate "
        if (lower.startsWith("translate:") || lower.startsWith("translate ")) {
            val clean = input.substringAfter("translate", "").trim().removePrefix(":").trim()
            return ParsedIntent("auto", "bn", clean) // default to Bengali if user prompt in English says Translate
        }

        return ParsedIntent(sourceLang, targetLang, text)
    }
}
