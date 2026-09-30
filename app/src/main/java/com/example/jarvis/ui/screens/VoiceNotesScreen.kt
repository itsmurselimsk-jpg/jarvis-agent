package com.example.jarvis.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager
import com.example.jarvis.voice.StarkSoundEngine
import com.example.jarvis.voice.VoiceMemoManager
import java.util.Locale

@Composable
fun VoiceNotesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val memoManager = remember { VoiceMemoManager(context) }

    val isRecording by memoManager.isRecording.collectAsState()
    val recordDuration by memoManager.recordingDurationSeconds.collectAsState()
    val currentRmsDb by memoManager.currentRmsDb.collectAsState()
    val playingPath by memoManager.currentlyPlayingPath.collectAsState()
    val playbackProgress by memoManager.playbackProgress.collectAsState()
    val memoList by memoManager.memoList.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            memoManager.stopPlayback()
            memoManager.stopRecording()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            memoManager.startRecording()
            StarkSoundEngine.playArcCharge()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = activeTheme.accentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "STARK VOICE LOGS & AUDIO VAULT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Text(
                    text = "HIGH-FIDELITY RAW AUDIO ARCHIVE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Recorder Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isRecording) JarvisRed.copy(alpha = 0.25f) else activeTheme.primaryColor.copy(alpha = 0.20f),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isRecording) JarvisRed else activeTheme.borderColor,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mic Orb Button
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "mic_pulse"
                        )

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) JarvisRed.copy(alpha = pulseAlpha)
                                    else activeTheme.primaryColor.copy(alpha = 0.3f)
                                )
                                .border(
                                    2.dp,
                                    if (isRecording) JarvisRed else activeTheme.accentColor,
                                    CircleShape
                                )
                                .clickable {
                                    if (isRecording) {
                                        memoManager.stopRecording()
                                        StarkSoundEngine.playConfirmationChime()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            memoManager.startRecording()
                                            StarkSoundEngine.playArcCharge()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Record",
                                tint = if (isRecording) Color.White else activeTheme.accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        if (isRecording) {
                            val mins = recordDuration / 60
                            val secs = recordDuration % 60
                            Text(
                                text = "RECORDING: ${String.format(Locale.US, "%02d:%02d", mins, secs)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisRed
                            )
                            Text(
                                text = "Live Signal Amplitude: ${String.format(Locale.US, "%.1f", currentRmsDb)} dB",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextSecondary
                            )
                        } else {
                            Text(
                                text = "TAP TO INITIATE AUDIO LOG",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = activeTheme.accentColor
                            )
                            Text(
                                text = "Crystal clear AAC compressed voice recordings",
                                fontSize = 11.sp,
                                color = JarvisTextDim
                            )
                        }
                    }
                }
            }

            // Audio list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARCHIVED VOICE LOGS (${memoList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeTheme.accentColor
                    )
                }
            }

            if (memoList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = JarvisTextDim,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No voice logs recorded in vault.",
                                fontSize = 13.sp,
                                color = JarvisTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(memoList) { memo ->
                    val isThisPlaying = playingPath == memo.file.absolutePath

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isThisPlaying) Color(0xFF131D33) else Color(0xFF0B1324))
                            .border(
                                width = 1.dp,
                                color = if (isThisPlaying) activeTheme.accentColor else activeTheme.borderColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            memoManager.togglePlay(memo)
                                            StarkSoundEngine.playCyberBeep()
                                        },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(activeTheme.primaryColor.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = activeTheme.accentColor
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = memo.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = JarvisTextPrimary
                                        )
                                        Text(
                                            text = "${memo.durationSeconds}s • ${(memo.sizeBytes / 1024)} KB",
                                            fontSize = 11.sp,
                                            color = JarvisTextSecondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        memoManager.deleteMemo(memo)
                                        StarkSoundEngine.playWarningTone()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = JarvisRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (isThisPlaying) {
                                LinearProgressIndicator(
                                    progress = { playbackProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = activeTheme.accentColor,
                                    trackColor = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
