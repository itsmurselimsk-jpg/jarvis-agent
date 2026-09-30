package com.example.jarvis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jarvis.model.JarvisState

/**
 * Voice Core Visualizer (Delegates to the premium 3D Holographic JARVIS Core).
 * Preserves backward compatibility while guaranteeing the premium 3D hologram identity.
 */
@Composable
fun ChatGPTVoiceOrb(
    modifier: Modifier = Modifier,
    size: Dp = 270.dp,
    isActive: Boolean = true,
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    audioRmsDb: Float = 0f,
    primaryColor: Color = Color(0xFF00E5FF),
    secondaryColor: Color = Color(0xFF2979FF),
    cloudColor: Color = Color(0xFFA855F7)
) {
    val state = when {
        isSpeaking -> JarvisState.SPEAKING
        isListening -> JarvisState.LISTENING
        !isActive -> JarvisState.IDLE
        else -> JarvisState.IDLE
    }

    HolographicJarvisCore3D(
        modifier = modifier,
        size = size,
        state = state,
        isListening = isListening,
        isSpeaking = isSpeaking,
        isThinking = !isSpeaking && !isListening && isActive && state == JarvisState.THINKING,
        audioRmsDb = audioRmsDb
    )
}
