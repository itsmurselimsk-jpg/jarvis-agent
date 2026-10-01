package com.jarvis.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.ai.model.SafetyRequest

enum class HologramTheme(
    val displayName: String,
    val primary: String,
    val secondary: String,
    val description: String
) {
    JARVIS_BLUE("Jarvis Blue", "#00E5FF", "#00838F", "Standard Mark VII Hologram UI"),
    JARVIS_CRIMSON("Protocols Crimson", "#FF1744", "#B71C1C", "House Party Protocol & Defense Matrix"),
    JARVIS_GOLD("Mark XLII Gold", "#FFD700", "#FF8F00", "Autonomous Nanotech Integration Matrix"),
    JARVIS_EMERALD("Vibranium Emerald", "#00E676", "#1B5E20", "Secure Enterprise Quantum Grid");
}

object HologramThemeManager {
    private val _activeTheme = mutableStateOf(HologramTheme.JARVIS_BLUE)
    val activeTheme: State<HologramTheme> = _activeTheme

    fun getActiveTheme(): HologramTheme = _activeTheme.value

    fun setThemeByName(name: String): HologramTheme {
        val lower = name.lowercase()
        val theme = when {
            lower.contains("crimson") || lower.contains("red") || lower.contains("house") -> HologramTheme.JARVIS_CRIMSON
            lower.contains("gold") || lower.contains("mark") || lower.contains("yellow") -> HologramTheme.JARVIS_GOLD
            lower.contains("emerald") || lower.contains("green") || lower.contains("vibranium") -> HologramTheme.JARVIS_EMERALD
            else -> HologramTheme.JARVIS_BLUE
        }
        _activeTheme.value = theme
        return theme
    }
}

enum class NavTab {
    CONVERSATION,
    HOME,
    SETTINGS
}

@Composable
fun SafetyConfirmationDialog(
    request: SafetyRequest,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Safety Confirmation Required") },
        text = {
            Column {
                Text("Action: ${request.toolName}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Risk Level: ${request.riskLevel}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(request.actionDescription)
            }
        },
        confirmButton = {
            Button(onClick = {
                request.onConfirm()
                onDismiss()
            }) {
                Text("Confirm & Execute")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                request.onCancel()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CodeStudioView(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
        Text("[$language] $code", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun HolographicCoreHero(
    state: Any?,
    isListening: Boolean,
    isSpeaking: Boolean,
    liveTranscript: String,
    lastResponse: String,
    rmsDb: Float,
    coreSize: Dp,
    onCoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(coreSize)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("JARVIS Holographic Core (State: $state)", style = MaterialTheme.typography.titleMedium)
            if (isListening) Text("Listening: $liveTranscript", color = MaterialTheme.colorScheme.primary)
            if (isSpeaking) Text("Speaking: $lastResponse", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun VoiceWaveform(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        Text("~~~ Audio Waveform ~~~", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ChatGPTVoiceOrb(
    size: Dp = 120.dp,
    state: Any? = null,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isThinking: Boolean = false,
    audioRmsDb: Float = 0f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("JARVIS", color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun HolographicJarvisCore3D(
    size: Dp = 280.dp,
    state: Any? = null,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isThinking: Boolean = false,
    audioRmsDb: Float = 0f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("3D Core", style = MaterialTheme.typography.titleLarge)
            Text("State: $state", style = MaterialTheme.typography.bodySmall)
        }
    }
}
