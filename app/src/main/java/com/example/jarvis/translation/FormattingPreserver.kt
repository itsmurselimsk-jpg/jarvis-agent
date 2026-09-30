package com.example.jarvis.translation

/**
 * Pre- and post-processing utility to guarantee preservation of:
 * - Code blocks (```...``` and `...`)
 * - URLs (https://... and http://...)
 * - Emails (user@domain.com)
 * - Placeholders ({name}, %s, %d, ${value}, {{var}})
 * - Markdown structural markers
 */
object FormattingPreserver {

    data class MaskedContent(
        val maskedText: String,
        val tokenMap: Map<String, String>
    )

    private val MULTI_LINE_CODE_BLOCK_REGEX = Regex("""```[\s\S]*?```""")
    private val INLINE_CODE_REGEX = Regex("""`[^`\n]+`""")
    private val URL_REGEX = Regex("""https?://[^\s<>"]+|www\.[^\s<>"]+""")
    private val EMAIL_REGEX = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val PLACEHOLDER_REGEX = Regex("""(\{[a-zA-Z0-9_]+\}|\{\{[a-zA-Z0-9_]+\}\}|\$\{[a-zA-Z0-9_.]+\}|%[0-9]*[sdxfbB])""")

    /**
     * Masks literal code blocks, URLs, emails, and variable placeholders
     * with inert tokens so the translation engine does not alter them.
     */
    fun maskFormatters(rawText: String): MaskedContent {
        if (rawText.isBlank()) return MaskedContent(rawText, emptyMap())

        val tokenMap = mutableMapOf<String, String>()
        var counter = 0
        var workingText = rawText

        // 1. Multi-line code blocks
        workingText = MULTI_LINE_CODE_BLOCK_REGEX.replace(workingText) { match ->
            val token = "__CODE_BLOCK_${counter++}__"
            tokenMap[token] = match.value
            token
        }

        // 2. Inline code
        workingText = INLINE_CODE_REGEX.replace(workingText) { match ->
            val token = "__INLINE_CODE_${counter++}__"
            tokenMap[token] = match.value
            token
        }

        // 3. URLs
        workingText = URL_REGEX.replace(workingText) { match ->
            val token = "__URL_${counter++}__"
            tokenMap[token] = match.value
            token
        }

        // 4. Emails
        workingText = EMAIL_REGEX.replace(workingText) { match ->
            val token = "__EMAIL_${counter++}__"
            tokenMap[token] = match.value
            token
        }

        // 5. Placeholders ({name}, %s, ${var})
        workingText = PLACEHOLDER_REGEX.replace(workingText) { match ->
            val token = "__PLACEHOLDER_${counter++}__"
            tokenMap[token] = match.value
            token
        }

        return MaskedContent(workingText, tokenMap)
    }

    /**
     * Restores original code blocks, URLs, emails, and placeholders into translated text.
     */
    fun restoreFormatters(translatedText: String, tokenMap: Map<String, String>): String {
        if (tokenMap.isEmpty() || translatedText.isBlank()) return translatedText

        var restored = translatedText
        // Replace exact tokens and also handle accidental whitespace inserted by LLMs like __ CODE_BLOCK_0 __
        tokenMap.forEach { (token, originalValue) ->
            restored = restored.replace(token, originalValue)

            // Flexible regex fallback in case AI separated underscores or changed casing
            val tokenContent = token.trim('_')
            val looseRegex = Regex("""__\s*${Regex.escape(tokenContent)}\s*__""", RegexOption.IGNORE_CASE)
            restored = looseRegex.replace(restored, originalValue)
        }

        return restored
    }
}
