package com.example.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.diagnostics.DiagnosticItem
import com.example.jarvis.diagnostics.DiagnosticStatus
import com.example.jarvis.diagnostics.JarvisDiagnostics
import com.example.jarvis.diagnostics.SystemTelemetryManager
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager
import com.example.jarvis.voice.StarkSoundEngine
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    repository: JarvisRepository,
    bridge: AndroidBridge
) {
    val context = LocalContext.current
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val telemetryManager = remember { SystemTelemetryManager(context) }

    val battery by telemetryManager.batteryState.collectAsState()
    val memory by telemetryManager.memoryState.collectAsState()
    val storage by telemetryManager.storageState.collectAsState()
    val network by telemetryManager.networkState.collectAsState()
    val sensor by telemetryManager.sensorState.collectAsState()

    DisposableEffect(Unit) {
        telemetryManager.startListening()
        onDispose {
            telemetryManager.stopListening()
        }
    }

    var diagnosticItems by remember { mutableStateOf<List<DiagnosticItem>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    fun refresh() {
        isRunning = true
        telemetryManager.refreshSnapshot()
        diagnosticItems = JarvisDiagnostics.runAllDiagnostics(context, repository, bridge)
        isRunning = false
        StarkSoundEngine.playCyberBeep()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val workingCount = diagnosticItems.count { it.status == DiagnosticStatus.WORKING }
    val attentionCount = diagnosticItems.size - workingCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JARVIS SYSTEM TELEMETRY & DIAGNOSTICS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeTheme.accentColor
                    )
                    Text(
                        text = "Real-time hardware & sensor matrix audit",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                }

                Button(
                    onClick = { refresh() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor, contentColor = Color.Black),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("run_diagnostics_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("AUDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Live Hardware Matrix (2x2 Grid)
        item {
            Text(
                text = "REAL-TIME HARDWARE TELEMETRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = activeTheme.accentColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Battery & RAM Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D1526))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = JarvisGreen, modifier = Modifier.size(18.dp))
                            Text("BATTERY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisGreen)
                        }
                        Text("${battery.levelPercent}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                        Text("${battery.temperatureCelsius}°C • ${battery.voltageMv}mV", fontSize = 10.sp, color = JarvisTextSecondary)
                        Text(battery.powerSource, fontSize = 9.sp, color = JarvisTextDim, fontFamily = FontFamily.Monospace)
                    }
                }

                // RAM Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D1526))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = JarvisCyanBright, modifier = Modifier.size(18.dp))
                            Text("SYSTEM RAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisCyanBright)
                        }
                        Text("${memory.ramUsagePercent}% Used", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                        LinearProgressIndicator(
                            progress = { memory.ramUsagePercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = if (memory.ramUsagePercent > 85) JarvisRed else JarvisCyanBright,
                            trackColor = Color(0xFF1E293B)
                        )
                        Text(String.format(Locale.US, "%.1f / %.1f GB", memory.usedRamGb, memory.totalRamGb), fontSize = 10.sp, color = JarvisTextSecondary)
                    }
                }
            }
        }

        // Storage & Sensors Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Storage Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D1526))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SdStorage, contentDescription = null, tint = JarvisAmber, modifier = Modifier.size(18.dp))
                            Text("STORAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisAmber)
                        }
                        Text("${storage.usagePercent}% Used", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                        LinearProgressIndicator(
                            progress = { storage.usagePercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = JarvisAmber,
                            trackColor = Color(0xFF1E293B)
                        )
                        Text(String.format(Locale.US, "%.0f / %.0f GB", storage.usedStorageGb, storage.totalStorageGb), fontSize = 10.sp, color = JarvisTextSecondary)
                    }
                }

                // Sensors & Network Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D1526))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CompassCalibration, contentDescription = null, tint = activeTheme.accentColor, modifier = Modifier.size(18.dp))
                            Text("SENSORS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = activeTheme.accentColor)
                        }
                        Text("${sensor.compassAzimuthDegrees.toInt()}° Azimuth", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                        Text("Light: ${sensor.lightLux.toInt()} Lux", fontSize = 10.sp, color = JarvisTextSecondary)
                        Text("IP: ${network.ipAddress}", fontSize = 9.sp, color = JarvisTextDim, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Summary Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurfaceElevated)
                    .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${diagnosticItems.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyanBright
                        )
                        Text("Subsystems", fontSize = 10.sp, color = JarvisTextDim)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$workingCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisGreen
                        )
                        Text("Operational", fontSize = 10.sp, color = JarvisTextDim)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$attentionCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (attentionCount > 0) JarvisAmber else JarvisGreen
                        )
                        Text("Notice / Config", fontSize = 10.sp, color = JarvisTextDim)
                    }
                }
            }
        }

        // Grouped Items
        val grouped = diagnosticItems.groupBy { it.category }
        grouped.forEach { (category, items) ->
            item {
                Text(
                    text = category.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(items) { item ->
                DiagnosticRow(item)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DiagnosticRow(item: DiagnosticItem) {
    val (statusColor, statusIcon) = when (item.status) {
        DiagnosticStatus.WORKING -> Pair(JarvisGreen, Icons.Default.CheckCircle)
        DiagnosticStatus.PERMISSION_REQUIRED -> Pair(JarvisAmber, Icons.Default.Warning)
        DiagnosticStatus.CONFIGURATION_REQUIRED -> Pair(JarvisAmber, Icons.Default.Info)
        DiagnosticStatus.ANDROID_RESTRICTION -> Pair(Color(0xFF9E9E9E), Icons.Default.Info)
        DiagnosticStatus.BROKEN -> Pair(JarvisRed, Icons.Default.Error)
        DiagnosticStatus.NOT_IMPLEMENTED -> Pair(Color.Gray, Icons.Default.Info)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurfaceElevated)
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.component,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
                Text(
                    text = item.details,
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = item.status.name.replace("_", " "),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }
            }
        }
    }
}
