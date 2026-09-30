package com.example

import com.example.jarvis.voice.JarvisWakePhraseMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for JarvisWakePhraseMatcher.
 * Validates normalization, accepted phrase matching, unrelated speech rejection,
 * punctuation handling, whitespace normalization, and directive extraction.
 */
class JarvisWakePhraseMatcherTest {

    @Test
    fun testExactEnglishWakePhrases() {
        val r1 = JarvisWakePhraseMatcher.match("hey jarvis")
        assertTrue(r1.isMatched)
        assertEquals("hey jarvis", r1.matchedPhrase)
        assertEquals("", r1.commandAfterWake)

        val r2 = JarvisWakePhraseMatcher.match("ok jarvis")
        assertTrue(r2.isMatched)
        assertEquals("ok jarvis", r2.matchedPhrase)
        assertEquals("", r2.commandAfterWake)

        val r3 = JarvisWakePhraseMatcher.match("okay jarvis")
        assertTrue(r3.isMatched)
        assertEquals("okay jarvis", r3.matchedPhrase)
    }

    @Test
    fun testHindiWakePhrases() {
        val r1 = JarvisWakePhraseMatcher.match("हे जार्विस")
        assertTrue(r1.isMatched)
        assertEquals("हे जार्विस", r1.matchedPhrase)

        val r2 = JarvisWakePhraseMatcher.match("हे जार्विस लाइट चालू करो")
        assertTrue(r2.isMatched)
        assertEquals("हे जार्विस", r2.matchedPhrase)
        assertEquals("लाइट चालू करो", r2.commandAfterWake)
    }

    @Test
    fun testCaseInsensitivityAndVariations() {
        val r1 = JarvisWakePhraseMatcher.match("HEY JARVIS")
        assertTrue(r1.isMatched)

        val r2 = JarvisWakePhraseMatcher.match("Ok JARVIS")
        assertTrue(r2.isMatched)

        val r3 = JarvisWakePhraseMatcher.match("hEy JaRvIs")
        assertTrue(r3.isMatched)
    }

    @Test
    fun testPunctuationHandling() {
        val r1 = JarvisWakePhraseMatcher.match("Hey, JARVIS!")
        assertTrue(r1.isMatched)
        assertEquals("", r1.commandAfterWake)

        val r2 = JarvisWakePhraseMatcher.match("Ok, JARVIS... What is the weather?")
        assertTrue(r2.isMatched)
        assertEquals("what is the weather", r2.commandAfterWake)

        val r3 = JarvisWakePhraseMatcher.match("हे, जार्विस!")
        assertTrue(r3.isMatched)
    }

    @Test
    fun testExtraSpacesAndTabs() {
        val r1 = JarvisWakePhraseMatcher.match("   hey    jarvis   ")
        assertTrue(r1.isMatched)

        val r2 = JarvisWakePhraseMatcher.match("  ok \t  jarvis  turn  on  flashlight  ")
        assertTrue(r2.isMatched)
        assertEquals("turn on flashlight", r2.commandAfterWake)
    }

    @Test
    fun testUnrelatedSpeechIgnored() {
        val r1 = JarvisWakePhraseMatcher.match("hello how are you")
        assertFalse(r1.isMatched)

        val r2 = JarvisWakePhraseMatcher.match("hey assistant")
        assertFalse(r2.isMatched)

        val r3 = JarvisWakePhraseMatcher.match("turn on the flashlight")
        assertFalse(r3.isMatched)

        val r4 = JarvisWakePhraseMatcher.match("google please help")
        assertFalse(r4.isMatched)

        val r5 = JarvisWakePhraseMatcher.match("siri what is the time")
        assertFalse(r5.isMatched)
    }

    @Test
    fun testEmptyAndNullHandling() {
        val r1 = JarvisWakePhraseMatcher.match("")
        assertFalse(r1.isMatched)

        val r2 = JarvisWakePhraseMatcher.match("    ")
        assertFalse(r2.isMatched)

        val r3 = JarvisWakePhraseMatcher.match(null)
        assertFalse(r3.isMatched)
    }

    @Test
    fun testDisclosureStatementText() {
        assertEquals(
            "Wake detection is phrase-match after speech recognition, not a neural wake-word chip.",
            JarvisWakePhraseMatcher.DISCLOSURE_TEXT
        )
    }
}
