package com.example.jarvis.voice

import java.util.Locale

/**
 * JARVIS Selectable Voice Profiles.
 * Provides distinct tonal calibrations for Text-To-Speech synthesis.
 */
enum class VoiceProfileType(
    val profileName: String,
    val tagline: String,
    val description: String,
    val defaultPitch: Float,
    val defaultSpeed: Float,
    val previewPhrase: String,
    val geminiVoiceName: String = "Puck",
    val preferredLocaleTag: String = "en-GB"
) {
    BETTANY(
        profileName = "JARVIS Bettany",
        tagline = "Paul Bettany British Neural",
        description = "Sophisticated, witty British cadence with rich human inflections (Signature JARVIS)",
        defaultPitch = 0.94f,
        defaultSpeed = 0.98f,
        previewPhrase = "Good day, Sir. All telemetry systems are calibrated and fully operational.",
        geminiVoiceName = "Puck",
        preferredLocaleTag = "en-GB"
    ),
    CALM(
        profileName = "JARVIS Calm",
        tagline = "Composed & Tranquil",
        description = "Steady, composed, relaxed acoustic cadence with low resonance",
        defaultPitch = 0.85f,
        defaultSpeed = 0.95f,
        previewPhrase = "All telemetry streams are tranquil and within nominal thresholds, Sir.",
        geminiVoiceName = "Puck",
        preferredLocaleTag = "en-GB"
    ),
    DEEP(
        profileName = "JARVIS Deep",
        tagline = "Authoritative Baritone",
        description = "Authoritative baritone with deep resonance and measured pace",
        defaultPitch = 0.70f,
        defaultSpeed = 0.90f,
        previewPhrase = "Security protocols active. Executive override confirmed, Sir.",
        geminiVoiceName = "Charon",
        preferredLocaleTag = "en-US"
    ),
    NATURAL(
        profileName = "JARVIS Natural",
        tagline = "Balanced & Conversational",
        description = "Balanced pitch and standard conversational human cadence",
        defaultPitch = 1.00f,
        defaultSpeed = 1.00f,
        previewPhrase = "JARVIS online and standing by. How may I be of assistance today, Sir?",
        geminiVoiceName = "Puck",
        preferredLocaleTag = "en-US"
    ),
    WARM(
        profileName = "JARVIS Warm",
        tagline = "Gentle & Cordial",
        description = "Gentle, cordial tone with smooth acoustic modulation and warm warmth",
        defaultPitch = 0.95f,
        defaultSpeed = 0.92f,
        previewPhrase = "Good day, Sir. I hope your agenda proceeds smoothly today.",
        geminiVoiceName = "Aoede",
        preferredLocaleTag = "en-GB"
    ),
    CRISP(
        profileName = "JARVIS Crisp",
        tagline = "Brisk & High Clarity",
        description = "High clarity, crisp articulation, and brisk operational tempo",
        defaultPitch = 1.15f,
        defaultSpeed = 1.10f,
        previewPhrase = "Telemetry updated. All subsystems responding at optimal velocity.",
        geminiVoiceName = "Fenrir",
        preferredLocaleTag = "en-US"
    ),
    FRIDAY(
        profileName = "F.R.I.D.A.Y. Female",
        tagline = "Crisp Synthetic Female",
        description = "Adaptive Irish/British female persona with crystal clear diction and witty charm",
        defaultPitch = 1.08f,
        defaultSpeed = 1.02f,
        previewPhrase = "Boss, computational matrices are synced. What is our next objective?",
        geminiVoiceName = "Kore",
        preferredLocaleTag = "en-IE"
    );

    companion object {
        fun fromName(name: String): VoiceProfileType {
            return entries.find { it.profileName.equals(name, ignoreCase = true) }
                ?: entries.find { name.contains(it.name, ignoreCase = true) }
                ?: BETTANY
        }
    }
}

/**
 * Multi-Language support for Bengali (বাংলা), Hindi (हिंदी), and English.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale,
    val greetingPhrase: String
) {
    AUTO(
        code = "auto",
        displayName = "Auto Detect",
        nativeName = "English / বাংলা / हिंदी",
        locale = Locale.US,
        greetingPhrase = "Standing by for your directive."
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        locale = Locale.US,
        greetingPhrase = "Yes Sir, I am listening."
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা",
        locale = Locale.forLanguageTag("bn-BD"),
        greetingPhrase = "হ্যাঁ স্যার, আমি শুনছি। কীভাবে সাহায্য করতে পারি?"
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिंदी",
        locale = Locale.forLanguageTag("hi-IN"),
        greetingPhrase = "हाँ सर, मैं सुन रहा हूँ। मैं आपकी क्या मदद कर सकता हूँ?"
    ),
    HINGLISH(
        code = "hi-Latn",
        displayName = "Hinglish",
        nativeName = "Hinglish (Roman Script)",
        locale = Locale.forLanguageTag("hi-IN"),
        greetingPhrase = "Haan Sir, main sun raha hoon. How can I help you?"
    ),
    BANGLISH(
        code = "bn-Latn",
        displayName = "Banglish",
        nativeName = "Banglish (Roman Script)",
        locale = Locale.forLanguageTag("bn-BD"),
        greetingPhrase = "Hae Sir, ami shunchi. Kibaabe help korte pari?"
    );

    companion object {
        fun fromCode(code: String): SupportedLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: AUTO
        }
    }
}
