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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.DeviceTelemetry
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary

@Composable
fun AndroidBridgeScreen(
    telemetry: DeviceTelemetry,
    onRefreshTelemetry: () -> Unit,
    onToggleFlashlight: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "ANDROID HARDWARE BRIDGE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )
                    Text(
                        text = "Real-time telemetry bus & device interconnect",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                }

                IconButton(onClick = onRefreshTelemetry, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh telemetry", tint = JarvisCyan)
                }
            }
        }

        // Live Telemetry Grid
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "LIVE SENSOR TELEMETRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )

                    // Battery
                    TelemetryMetricRow(
                        icon = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        label = "Power Reservoir",
                        value = "${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Discharging"})",
                        accentColor = if (telemetry.batteryPercent < 20) JarvisRed else JarvisCyan
                    )

                    // Network
                    TelemetryMetricRow(
                        icon = Icons.Default.Wifi,
                        label = "Network Protocol",
                        value = telemetry.networkType,
                        accentColor = JarvisGreen
                    )

                    // Audio
                    TelemetryMetricRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Audio Stream Level",
                        value = "${telemetry.volumePercent}%",
                        accentColor = JarvisCyan
                    )

                    // Memory
                    TelemetryMetricRow(
                        icon = Icons.Default.Info,
                        label = "Available Runtime Heap",
                        value = "${telemetry.memoryAvailableMB} MB",
                        accentColor = JarvisCyanBright
                    )
                }
            }
        }

        // Direct Hardware Controls (Flashlight, Settings)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DIRECT HARDWARE CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )

                    // Flashlight toggle
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
                                imageVector = if (telemetry.isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = null,
                                tint = if (telemetry.isFlashlightOn) JarvisAmber else JarvisTextSecondary
                            )
                            Column {
                                Text("High-Intensity LED Torch", fontSize = 13.sp, color = JarvisTextPrimary)
                                Text("Direct Camera2 hardware link", fontSize = 10.sp, color = JarvisTextDim)
                            }
                        }

                        Switch(
                            checked = telemetry.isFlashlightOn,
                            onCheckedChange = { onToggleFlashlight(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisAmber,
                                checkedTrackColor = JarvisAmber.copy(alpha = 0.3f),
                                uncheckedThumbColor = JarvisTextDim,
                                uncheckedTrackColor = Color(0xFF162032)
                            )
                        )
                    }

                    // Open Android Settings
                    Button(
                        onClick = onOpenSystemSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_system_settings_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1A2E), contentColor = JarvisCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LAUNCH ANDROID SYSTEM PREFERENCES", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Honest Service Boundaries (No fake phone takeover)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF060B14))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SUBSYSTEM SERVICE BOUNDARIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisAmber
                    )
                    Text(
                        text = "JARVIS enforces transparent system requirements. The following operations require dedicated Android foreground or daemon services:",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 15.sp
                    )

                    BoundaryNoticeItem(
                        service = "Global Screen Touch & Navigation",
                        status = "Native AccessibilityService required"
                    )

                    BoundaryNoticeItem(
                        service = "External Notification Ingestion",
                        status = "Native NotificationListenerService required"
                    )

                    BoundaryNoticeItem(
                        service = "Peripheral Hardware Telemetry",
                        status = "Native Bluetooth Admin service required"
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 12.sp, color = JarvisTextSecondary)
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
    }
}

@Composable
private fun BoundaryNoticeItem(service: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "• $service", fontSize = 11.sp, color = JarvisTextPrimary)
        Text(
            text = status,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = JarvisAmber
        )
    }
}
