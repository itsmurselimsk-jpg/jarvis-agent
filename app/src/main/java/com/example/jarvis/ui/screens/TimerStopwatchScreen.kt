package com.example.jarvis.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.tactical.FlashlightController
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager
import com.example.jarvis.voice.StarkSoundEngine
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TimerStopwatchScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val flashlightController = remember { FlashlightController(context) }
    val isTorchOn by flashlightController.isTorchOn.collectAsState()
    val isStrobeActive by flashlightController.isStrobeActive.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            flashlightController.stopStrobe()
            flashlightController.setTorchState(false)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Timer, 1: Stopwatch, 2: Tactical Flashlight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        // Top Header
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
                    text = "STARK CHRONOMETER & TACTICAL",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Text(
                    text = "ARC REACTOR PRECISION TIMING",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF090E1A),
            contentColor = activeTheme.accentColor,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = activeTheme.accentColor
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; StarkSoundEngine.playCyberBeep() },
                text = { Text("ARC TIMER", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; StarkSoundEngine.playCyberBeep() },
                text = { Text("STOPWATCH", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2; StarkSoundEngine.playCyberBeep() },
                text = { Text("TACTICAL TORCH", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
            )
        }

        when (selectedTab) {
            0 -> ArcTimerView(activeTheme = activeTheme)
            1 -> StarkStopwatchView(activeTheme = activeTheme)
            2 -> TacticalTorchView(
                isTorchOn = isTorchOn,
                isStrobeActive = isStrobeActive,
                onToggleTorch = {
                    StarkSoundEngine.playArcCharge()
                    flashlightController.toggleTorch()
                },
                onToggleStrobe = {
                    StarkSoundEngine.playWarningTone()
                    flashlightController.toggleStrobe(6)
                }
            )
        }
    }
}

@Composable
private fun ArcTimerView(activeTheme: com.example.jarvis.ui.theme.MarkArmorTheme) {
    var totalSeconds by remember { mutableIntStateOf(300) } // Default 5 mins
    var remainingSeconds by remember { mutableIntStateOf(300) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                isRunning = false
                StarkSoundEngine.playConfirmationChime()
            }
        }
    }

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Arc Reactor Timer Graphic
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(top = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track
                drawCircle(
                    color = activeTheme.primaryColor.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // Active Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(activeTheme.primaryColor, activeTheme.accentColor, activeTheme.glowColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Text(
                    text = if (isRunning) "ACTIVE COUNTDOWN" else "STANDBY",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary
                )
            }
        }

        // Quick Preset Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(60 to "1m", 180 to "3m", 300 to "5m", 600 to "10m", 1500 to "25m").forEach { (sec, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable {
                            totalSeconds = sec
                            remainingSeconds = sec
                            isRunning = false
                            StarkSoundEngine.playCyberBeep()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeTheme.accentColor
                    )
                }
            }
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    remainingSeconds = totalSeconds
                    isRunning = false
                    StarkSoundEngine.playCyberBeep()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = JarvisTextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("RESET", color = JarvisTextSecondary, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = {
                    isRunning = !isRunning
                    if (isRunning) StarkSoundEngine.playArcCharge() else StarkSoundEngine.playCyberBeep()
                },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) JarvisAmber else activeTheme.primaryColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "PAUSE ARC" else "ENGAGE ARC",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StarkStopwatchView(activeTheme: com.example.jarvis.ui.theme.MarkArmorTheme) {
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(isRunning) {
        var lastTime = System.currentTimeMillis()
        while (isRunning) {
            delay(30)
            val now = System.currentTimeMillis()
            elapsedMillis += (now - lastTime)
            lastTime = now
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Timer display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, activeTheme.borderColor, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val totalSec = elapsedMillis / 1000
            val mins = totalSec / 60
            val secs = totalSec % 60
            val millis = (elapsedMillis % 1000) / 10

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d.%02d", mins, secs, millis),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Text(
                    text = "PRECISION STOPWATCH",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isRunning = !isRunning
                    if (isRunning) StarkSoundEngine.playArcCharge() else StarkSoundEngine.playCyberBeep()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) JarvisAmber else activeTheme.primaryColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isRunning) "STOP" else "START",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    if (isRunning) {
                        laps.add(0, elapsedMillis)
                        StarkSoundEngine.playCyberBeep()
                    }
                },
                enabled = isRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = activeTheme.accentColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("LAP SPLIT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    isRunning = false
                    elapsedMillis = 0L
                    laps.clear()
                    StarkSoundEngine.playCyberBeep()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = JarvisRed
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("RESET", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Laps list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(laps) { index, lapTime ->
                val totalSec = lapTime / 1000
                val mins = totalSec / 60
                val secs = totalSec % 60
                val millis = (lapTime % 1000) / 10

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B1324))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAP #${laps.size - index}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = JarvisTextSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "%02d:%02d.%02d", mins, secs, millis),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = activeTheme.accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalTorchView(
    isTorchOn: Boolean,
    isStrobeActive: Boolean,
    onToggleTorch: () -> Unit,
    onToggleStrobe: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Large Torch Arc Button
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    if (isTorchOn) Brush.radialGradient(listOf(Color(0xFFFFF7ED), Color(0xFFF59E0B), Color(0xFF0F172A)))
                    else Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF090E1A)))
                )
                .border(
                    width = 3.dp,
                    color = if (isTorchOn) JarvisAmber else Color(0xFF334155),
                    shape = CircleShape
                )
                .clickable { onToggleTorch() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Torch Toggle",
                tint = if (isTorchOn) Color.Black else Color.Gray,
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = if (isTorchOn) "TACTICAL BEACON ACTIVE" else "TORCH STANDBY",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isTorchOn) JarvisAmber else JarvisTextSecondary
        )

        // Strobe SOS Mode Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isStrobeActive) Color(0x33EF4444) else Color(0xFF0F172A))
                .border(
                    1.dp,
                    if (isStrobeActive) JarvisRed else Color(0xFF334155),
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS Strobe",
                        tint = if (isStrobeActive) JarvisRed else JarvisAmber
                    )
                    Column {
                        Text(
                            text = "SOS EMERGENCY STROBE",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (isStrobeActive) JarvisRed else JarvisTextPrimary
                        )
                        Text(
                            text = "6 Hz High-Frequency Light Pulse",
                            fontSize = 11.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }

                Button(
                    onClick = onToggleStrobe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStrobeActive) JarvisRed else Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isStrobeActive) "DISENGAGE" else "STROBE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
