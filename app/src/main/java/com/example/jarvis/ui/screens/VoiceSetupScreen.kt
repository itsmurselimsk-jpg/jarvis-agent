package com.example.jarvis.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.voice.JarvisWakePhraseMatcher

/**
 * Focused, dedicated Voice Setup Screen for JARVIS.
 * Provides live microphone testing, TTS synthesis testing, SpeechRecognizer wake phrase testing,
 * essential permission verification, and strict wake detection disclosure.
 */
@Composable
fun VoiceSetupScreen(
    currentSettings: ProviderSettings = ProviderSettings(),
    onUpdateSettings: (ProviderSettings) -> Unit = {},
    onTestWakeTrigger: () -> Unit = {},
    bridge: AndroidBridge? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe live bridge telemetry if supplied
    val isSpeaking by (bridge?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) })
    val isListening by (bridge?.isListening?.collectAsState() ?: remember { mutableStateOf(false) })
    val liveRmsDb by (bridge?.voiceRmsDb?.collectAsState() ?: remember { mutableFloatStateOf(0f) })
    val liveTranscript by (bridge?.liveTranscript?.collectAsState() ?: remember { mutableStateOf("") })

    // Local Test State
    var isMicTestActive by remember { mutableStateOf(false) }
    var isWakeTestActive by remember { mutableStateOf(false) }
    var wakeTestTranscript by remember { mutableStateOf("") }
    var wakeTestResult by remember { mutableStateOf<JarvisWakePhraseMatcher.MatchResult?>(null) }

    var refreshPermissionCounter by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isMicTestActive || isWakeTestActive) {
                bridge?.stopListening()
            }
            bridge?.stopSpeaking()
        }
    }

    // Permission Checkers
    val hasMicPermission = remember(refreshPermissionCounter) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotificationPermission = remember(refreshPermissionCounter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionCounter++
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionCounter++
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("voice_setup_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("voice_setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisCyanBright
                        )
                    }
                    Column {
                        Text(
                            text = "VOICE SETUP // DIAGNOSTICS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            color = JarvisCyanBright
                        )
                        Text(
                            text = "Acoustics, SpeechRecognizer & Synthesis Verification",
                            fontSize = 11.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }
        }

        // 1. Mandatory Disclosure Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Disclosure",
                        tint = JarvisCyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ARCHITECTURE DISCLOSURE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = JarvisCyanBright
                        )
                        Text(
                            text = JarvisWakePhraseMatcher.DISCLOSURE_TEXT,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = JarvisTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Permission Status Card
        item {
            VoiceCard(title = "VOICE PERMISSIONS // SECURITY") {
                // Microphone Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hasMicPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasMicPermission) JarvisGreen else JarvisAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Microphone (RECORD_AUDIO)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTextPrimary
                            )
                            Text(
                                text = if (hasMicPermission) "Authorized for speech input & wake detection" else "Required for voice input",
                                fontSize = 10.sp,
                                color = JarvisTextSecondary
                            )
                        }
                    }

                    if (!hasMicPermission) {
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        Text(
                            text = "GRANTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisGreen
                        )
                    }
                }

                // Notification Permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (hasNotificationPermission) JarvisGreen else JarvisTextDim,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Foreground Service Dispatch",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisTextPrimary
                                )
                                Text(
                                    text = "Keeps voice listener active in background",
                                    fontSize = 10.sp,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        if (!hasNotificationPermission) {
                            Button(
                                onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisCyanBright)
                            }
                        } else {
                            Text(
                                text = "GRANTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisGreen
                            )
                        }
                    }
                }
            }
        }

        // 3. Microphone Live Input Test Card
        item {
            VoiceCard(title = "MICROPHONE // ACOUSTIC INPUT TEST") {
                Text(
                    text = "Verify microphone capture sensitivity and live RMS input decibel levels:",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )

                // Input Level Gauge
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Live Input Level:",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )
                        Text(
                            text = if (isMicTestActive) {
                                if (liveRmsDb > 2f) "SIGNAL DETECTED (${liveRmsDb.toInt()} dB)" else "LISTENING / AMBIENT"
                            } else {
                                "STANDBY"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isMicTestActive && liveRmsDb > 2f) JarvisGreen else JarvisCyanBright
                        )
                    }

                    // Meter Bar
                    val meterProgress = if (isMicTestActive) (liveRmsDb / 15f).coerceIn(0.05f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { meterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (meterProgress > 0.6f) JarvisGreen else JarvisCyanBright,
                        trackColor = Color(0xFF0C1929)
                    )
                }

                // Live Partial Transcript
                if (isMicTestActive && liveTranscript.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF071424))
                            .border(0.5.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Detected: \"$liveTranscript\"",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextPrimary
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isMicTestActive) {
                        Button(
                            onClick = {
                                if (!hasMicPermission) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@Button
                                }
                                isMicTestActive = true
                                isWakeTestActive = false
                                bridge?.startListening(
                                    onResult = { isMicTestActive = false },
                                    onError = { isMicTestActive = false }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("start_mic_test_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Mic Test", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        Button(
                            onClick = {
                                isMicTestActive = false
                                bridge?.stopListening()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("stop_mic_test_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Test", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 4. TTS Synthesis Test Card
        item {
            VoiceCard(title = "TTS // SYNTHESIS ENGINE TEST") {
                Text(
                    text = "Verify on-device text-to-speech engine and audio output clarity:",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF071424))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "\"Testing JARVIS audio output systems. Vocal synthesizer operating at peak efficiency.\"",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = JarvisCyan
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            bridge?.speak(
                                text = "Testing JARVIS audio output systems. Vocal synthesizer operating at peak efficiency.",
                                speechRate = currentSettings.speechRate,
                                pitch = currentSettings.speechPitch
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("play_tts_test_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = JarvisCyanBright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Test Speech", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JarvisCyanBright)
                    }

                    if (isSpeaking) {
                        Button(
                            onClick = { bridge?.stopSpeaking() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("stop_tts_test_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop TTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 5. Wake Phrase Test Card
        item {
            VoiceCard(title = "WAKE PHRASE // SPEECHRECOGNIZER MATCHER") {
                Text(
                    text = "Say an accepted phrase to test the deterministic phrase matcher:",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )

                // Accepted list badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("\"hey jarvis\"", "\"ok jarvis\"", "\"हे जार्विस\"").forEach { phrase ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B192C))
                                .border(0.5.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = phrase,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyanBright
                            )
                        }
                    }
                }

                // Live matching output box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF071424))
                        .border(
                            1.dp,
                            when {
                                wakeTestResult?.isMatched == true -> JarvisGreen
                                wakeTestResult?.isMatched == false && wakeTestTranscript.isNotBlank() -> JarvisRed
                                else -> JarvisBorderSubtle
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECOGNIZED SPEECH:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextDim
                            )

                            if (wakeTestResult != null && wakeTestTranscript.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (wakeTestResult?.isMatched == true) Color(0x3300E676) else Color(0x33FF1744))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (wakeTestResult?.isMatched == true) "WAKE DETECTED" else "IGNORED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (wakeTestResult?.isMatched == true) JarvisGreen else JarvisRed
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (wakeTestTranscript.isNotBlank()) "\"$wakeTestTranscript\"" else "Press 'Test Wake Phrase' and speak...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (wakeTestTranscript.isNotBlank()) JarvisTextPrimary else JarvisTextDim
                        )

                        if (wakeTestResult?.isMatched == true && wakeTestResult?.commandAfterWake?.isNotBlank() == true) {
                            Text(
                                text = "Extracted Directive: \"${wakeTestResult?.commandAfterWake}\"",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyanBright
                            )
                        }
                    }
                }

                // Wake Test Trigger Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isWakeTestActive) {
                        Button(
                            onClick = {
                                if (!hasMicPermission) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@Button
                                }
                                isWakeTestActive = true
                                isMicTestActive = false
                                wakeTestTranscript = ""
                                wakeTestResult = null

                                bridge?.startListening(
                                    onResult = { spoken ->
                                        isWakeTestActive = false
                                        wakeTestTranscript = spoken
                                        wakeTestResult = JarvisWakePhraseMatcher.match(spoken)
                                    },
                                    onError = {
                                        isWakeTestActive = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("test_wake_phrase_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Wake Phrase", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        Button(
                            onClick = {
                                isWakeTestActive = false
                                bridge?.stopListening()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("cancel_wake_test_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Listening... (Tap to Cancel)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VoiceCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisSurfaceElevated)
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = JarvisCyan
            )
            content()
        }
    }
}
