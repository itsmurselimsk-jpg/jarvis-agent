package com.example

import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.model.VoiceSynthesisEngine
import com.example.jarvis.voice.SupportedLanguage
import com.example.jarvis.voice.TtsSanitizer
import com.example.jarvis.voice.VoiceProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisHumanVoiceUnitTest {

    @Test
    fun testVoiceSynthesisEngineResolution() {
        assertEquals(VoiceSynthesisEngine.HYBRID_AUTO, VoiceSynthesisEngine.fromId("hybrid_auto"))
        assertEquals(VoiceSynthesisEngine.GEMINI_STUDIO, VoiceSynthesisEngine.fromId("gemini_studio"))
        assertEquals(VoiceSynthesisEngine.NEURAL_DEVICE, VoiceSynthesisEngine.fromId("neural_device"))
        assertEquals(VoiceSynthesisEngine.HYBRID_AUTO, VoiceSynthesisEngine.fromId("unknown_engine"))

        // Verify descriptive metadata exists
        assertTrue(VoiceSynthesisEngine.HYBRID_AUTO.title.contains("Hybrid"))
        assertTrue(VoiceSynthesisEngine.GEMINI_STUDIO.description.contains("human"))
        assertTrue(VoiceSynthesisEngine.NEURAL_DEVICE.subtitle.contains("Offline"))
    }

    @Test
    fun testVoiceProfilesAndGeminiVoices() {
        val bettany = VoiceProfileType.fromName("JARVIS Bettany")
        assertEquals(VoiceProfileType.BETTANY, bettany)
        assertEquals("Puck", bettany.geminiVoiceName)
        assertEquals("en-GB", bettany.preferredLocaleTag)
        assertTrue(bettany.tagline.contains("Bettany"))

        val friday = VoiceProfileType.fromName("F.R.I.D.A.Y. Female")
        assertEquals(VoiceProfileType.FRIDAY, friday)
        assertEquals("Kore", friday.geminiVoiceName)

        val deep = VoiceProfileType.fromName("JARVIS Deep")
        assertEquals(VoiceProfileType.DEEP, deep)
        assertEquals("Charon", deep.geminiVoiceName)

        val natural = VoiceProfileType.fromName("JARVIS Natural")
        assertEquals(VoiceProfileType.NATURAL, natural)

        // Verify fallback
        val fallback = VoiceProfileType.fromName("NonExistentProfile")
        assertNotNull(fallback)
    }

    @Test
    fun testCleanForHumanSpeechMarkdownStripping() {
        val markdown = "### System Status\n**JARVIS** is `online`! [View Docs](https://example.com)\n* System 1: Normal\n---"
        val cleaned = TtsSanitizer.cleanForHumanSpeech(markdown)

        assertFalse(cleaned.contains("###"))
        assertFalse(cleaned.contains("**"))
        assertFalse(cleaned.contains("`"))
        assertFalse(cleaned.contains("https://example.com"))
        assertFalse(cleaned.contains("---"))
        assertTrue(cleaned.contains("JARVIS is online"))
        assertTrue(cleaned.contains("View Docs"))
    }

    @Test
    fun testCleanForHumanSpeechCadence() {
        val input = "Good day Sir, all telemetry streams are optimal. Ready for instructions."
        val cleaned = TtsSanitizer.cleanForHumanSpeech(input)

        assertTrue(cleaned.contains("Good day Sir"))
        assertTrue(cleaned.contains("Ready for instructions"))
        assertEquals(cleaned, TtsSanitizer.cleanForHumanSpeech(cleaned))
    }

    @Test
    fun testProviderSettingsDefaultAudioEngine() {
        val settings = ProviderSettings()
        assertEquals(VoiceSynthesisEngine.HYBRID_AUTO, settings.voiceSynthesisEngine)
        assertEquals("Puck", settings.geminiVoiceName)
        assertEquals("JARVIS Natural", settings.voiceProfileName)
    }

    @Test
    fun testMultilingualGreetings() {
        val bengali = SupportedLanguage.fromCode("bn")
        assertEquals(SupportedLanguage.BENGALI, bengali)
        assertTrue(bengali.greetingPhrase.contains("স্যার"))

        val hindi = SupportedLanguage.fromCode("hi")
        assertEquals(SupportedLanguage.HINDI, hindi)
        assertTrue(hindi.greetingPhrase.contains("सर"))

        val hinglish = SupportedLanguage.fromCode("hi-Latn")
        assertEquals(SupportedLanguage.HINGLISH, hinglish)
        assertTrue(hinglish.greetingPhrase.contains("Sir"))
    }
}
