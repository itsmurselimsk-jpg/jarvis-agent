package com.example

import com.example.jarvis.voice.TtsSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisTtsSanitizerUnitTest {

    @Test
    fun testSimpleEmojiSanitization() {
        val input = "Hello 👋"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("Hello", sanitized)
    }

    @Test
    fun testHaanBhaiEmojiSanitization() {
        val input = "Haan bhai 😄 kya hua?"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("Haan bhai kya hua?", sanitized)
        assertFalse(sanitized.contains("😄"))
    }

    @Test
    fun testGreetingAndHelpEmojiSanitization() {
        val input = "Hey 👋 how can I help you today? 😊"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("Hey how can I help you today?", sanitized)
        assertFalse(sanitized.contains("👋"))
        assertFalse(sanitized.contains("😊"))
    }

    @Test
    fun testBatteryPercentagePreservation() {
        val input = "Battery 80% hai 🔋"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("Battery 80% hai", sanitized)
        assertTrue(sanitized.contains("80%"))
        assertFalse(sanitized.contains("🔋"))
    }

    @Test
    fun testHindiTextWithEmoji() {
        val input = "नमस्ते भाई! 👋 बताइए"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("नमस्ते भाई! बताइए", sanitized)
        assertTrue(sanitized.contains("नमस्ते"))
        assertFalse(sanitized.contains("👋"))
    }

    @Test
    fun testBengaliTextWithEmoji() {
        val input = "কেমন আছো? 😊"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("কেমন আছো?", sanitized)
        assertTrue(sanitized.contains("কেমন"))
        assertFalse(sanitized.contains("😊"))
    }

    @Test
    fun testHinglishBanglishWithEmoji() {
        val input1 = "Ho gaya bhai 🔥"
        val sanitized1 = TtsSanitizer.sanitizeForTts(input1)
        assertEquals("Ho gaya bhai", sanitized1)

        val input2 = "Thik hai 👍 kal milte hain ❤️"
        val sanitized2 = TtsSanitizer.sanitizeForTts(input2)
        assertEquals("Thik hai kal milte hain", sanitized2)
        assertFalse(sanitized2.contains("👍"))
        assertFalse(sanitized2.contains("❤️"))
    }

    @Test
    fun testTextWithoutEmojiUnchanged() {
        val input = "Hello world"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("Hello world", sanitized)
    }

    @Test
    fun testMultipleCombinedEmojis() {
        val input = "Hello 👋😄🔥"
        val sanitized = TtsSanitizer.sanitizeForTts(input)
        assertEquals("Hello", sanitized)
    }

    @Test
    fun testNullAndBlankInput() {
        assertEquals("", TtsSanitizer.sanitizeForTts(null))
        assertEquals("", TtsSanitizer.sanitizeForTts(""))
        assertEquals("", TtsSanitizer.sanitizeForTts("   "))
    }
}
