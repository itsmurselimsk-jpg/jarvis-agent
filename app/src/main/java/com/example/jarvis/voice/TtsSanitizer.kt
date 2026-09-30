package com.example.jarvis.voice

object TtsSanitizer {

    fun sanitizeForTts(text: String?): String {
        if (text.isNullOrBlank()) return ""

        val sb = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            if (!isEmojiCodePoint(codePoint)) {
                sb.appendCodePoint(codePoint)
            }
            i += charCount
        }

        return sb.toString()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s+([,.!?])"""), "$1")
            .trim()
    }

    fun isEmojiCodePoint(codePoint: Int): Boolean {
        // Preserve standard ASCII characters (32 to 126)
        if (codePoint in 32..126) return false
        // Preserve ASCII control chars / newlines / tabs
        if (codePoint in 0..31) return false

        // Preserve Devanagari (Hindi) script
        if (codePoint in 0x0900..0x097F) return false

        // Preserve Bengali script
        if (codePoint in 0x0980..0x09FF) return false

        // Explicit emoji/symbol ranges
        if (codePoint in 0xFE00..0xFE0F) return true // Variation Selectors
        if (codePoint == 0x200D || codePoint == 0x200B) return true // ZWJ & ZWSP
        if (codePoint in 0x1F3FB..0x1F3FF) return true // Skin tone modifiers
        if (codePoint == 0x20E3) return true // Keycap
        if (codePoint in 0x1F1E6..0x1F1FF) return true // Flags / Regional Indicators

        if (codePoint in 0x1F600..0x1F64F) return true // Emoticons
        if (codePoint in 0x1F300..0x1F5FF) return true // Misc Symbols and Pictographs
        if (codePoint in 0x1F680..0x1F6FF) return true // Transport and Map
        if (codePoint in 0x1F900..0x1F9FF) return true // Supplemental Symbols & Pictographs
        if (codePoint in 0x1FA70..0x1FAFF) return true // Symbols & Pictographs Ext-A
        if (codePoint in 0x1F000..0x1FFFF) return true // All Supplementary Multilingual Plane symbols

        if (codePoint in 0x2600..0x26FF) return true // Misc Symbols (e.g. ❤️, ☀️, ⚡)
        if (codePoint in 0x2700..0x27BF) return true // Dingbats
        if (codePoint in 0x2300..0x23FF) return true // Misc Technical
        if (codePoint in 0x2B00..0x2BFF) return true // Misc Symbols and Arrows
        if (codePoint in 0x2190..0x21FF) return true // Arrows
        if (codePoint in 0x3297..0x3299) return true

        // Fallback by Unicode Character Category
        val type = Character.getType(codePoint)
        if (type == Character.OTHER_SYMBOL.toInt() ||
            type == Character.SURROGATE.toInt() ||
            type == Character.MODIFIER_SYMBOL.toInt()) {
            return true
        }

        return false
    }

    /**
     * Cleans markdown formatting, code blocks, technical URLs, and inserts natural
     * acoustic pauses so synthesis sounds conversational and human rather than robotic.
     */
    fun cleanForHumanSpeech(text: String?): String {
        val base = sanitizeForTts(text)
        if (base.isBlank()) return ""

        return base
            // Remove code blocks
            .replace(Regex("""```[\s\S]*?```"""), " code block omitted ")
            // Remove inline backticks
            .replace("`", "")
            // Remove markdown headers
            .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
            // Remove bold/italic markers
            .replace("**", "")
            .replace("*", "")
            .replace("__", "")
            .replace("~~", "")
            // Convert markdown links [text](url) -> text
            .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
            // Remove bullet points / numbering markers at line start
            .replace(Regex("""^[•\-\*]\s+""", RegexOption.MULTILINE), "")
            // Replace long divider lines with a breath pause
            .replace(Regex("""[-=_]{3,}"""), ", ")
            // Convert technical symbols to words or pauses
            .replace("&", " and ")
            .replace("/", " slash ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
