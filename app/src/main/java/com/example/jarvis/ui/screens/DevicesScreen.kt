package com.example.jarvis.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary

@Composable
fun DevicesScreen(
    telemetry: DeviceTelemetry,
    deviceTilt: Pair<Float, Float> = Pair(0f, 0f),
    onRefreshTelemetry: () -> Unit,
    onToggleFlashlight: (Boolean) -> Unit,
    onOpenSystemSettings: (String?) -> Unit = {},
    onLaunchApp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var currentVolume by remember {
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        mutableFloatStateOf(cur.toFloat() / max.toFloat())
    }

    var isFlashlightOn by remember(telemetry.isFlashlightOn) {
        mutableStateOf(telemetry.isFlashlightOn)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("devices_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEVICES // HARDWARE MATRIX",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = JarvisCyanBright
                    )
                    Text(
                        text = "Holographic Android device telemetry & physical controls",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                }

                IconButton(
                    onClick = onRefreshTelemetry,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x2B00E5FF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 1. Primary Hardware Switchboard (Flashlight, Mute, System Settings)
        item {
            HolographicSectionCard(title = "TACTICAL HARDWARE INTERFACE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Flashlight Card
                    HolographicToggleCard(
                        title = "Flashlight",
                        subtitle = if (isFlashlightOn) "BEAM ACTIVE" else "STANDBY",
                        icon = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        isActive = isFlashlightOn,
                        activeColor = JarvisCyanBright,
                        modifier = Modifier.weight(1f),
                        onToggle = {
                            isFlashlightOn = it
                            onToggleFlashlight(it)
                        }
                    )

                    // Volume Mute Card
                    val isMuted = currentVolume <= 0.05f
                    HolographicToggleCard(
                        title = "Audio Output",
                        subtitle = if (isMuted) "MUTED" else "${(currentVolume * 100).toInt()}% GAIN",
                        icon = if (isMuted) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        isActive = !isMuted,
                        activeColor = JarvisElectricBlue,
                        modifier = Modifier.weight(1f),
                        onToggle = { active ->
                            if (active) {
                                currentVolume = 0.65f
                                audioManager?.let { am ->
                                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.65f).toInt(), AudioManager.FLAG_SHOW_UI)
                                }
                            } else {
                                currentVolume = 0f
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Media Volume Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66060F20))
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MEDIA AUDIO GAIN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan
                        )
                        Text(
                            text = "${(currentVolume * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyanBright
                        )
                    }

                    Slider(
                        value = currentVolume,
                        onValueChange = { newVal ->
                            currentVolume = newVal
                            audioManager?.let { am ->
                                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * newVal).toInt(), 0)
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyanBright,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }

        // 2. Network & Connectivity Bus (Wi-Fi, Bluetooth, Location, NFC)
        item {
            HolographicSectionCard(title = "CONNECTIVITY & NETWORK UPLINK") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeviceStatusBadge(
                        title = "Network",
                        status = telemetry.networkType.uppercase(),
                        icon = Icons.Default.Wifi,
                        color = if (telemetry.networkType.contains("none", ignoreCase = true)) JarvisRed else JarvisGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSystemSettings("wifi") }
                    )

                    DeviceStatusBadge(
                        title = "Bluetooth",
                        status = "READY",
                        icon = Icons.Default.Bluetooth,
                        color = JarvisElectricBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSystemSettings("bluetooth") }
                    )

                    DeviceStatusBadge(
                        title = "Display",
                        status = "AUTO BRIGHT",
                        icon = Icons.Default.BrightnessMedium,
                        color = JarvisAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSystemSettings("display") }
                    )
                }
            }
        }

        // 3. Power & Battery Core Telemetry
        item {
            HolographicSectionCard(title = "POWER MATRIX & THERMALS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isCharging = telemetry.isCharging
                    val batteryColor = when {
                        isCharging -> JarvisGreen
                        telemetry.batteryPercent > 40 -> JarvisCyanBright
                        telemetry.batteryPercent > 20 -> JarvisAmber
                        else -> JarvisRed
                    }

                    DeviceStatusBadge(
                        title = "Battery",
                        status = "${telemetry.batteryPercent}% ${if (isCharging) "⚡" else ""}",
                        icon = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        color = batteryColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSystemSettings("battery") }
                    )

                    DeviceStatusBadge(
                        title = "Memory Free",
                        status = "${telemetry.memoryAvailableMB} MB",
                        icon = Icons.Default.Speed,
                        color = JarvisCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onRefreshTelemetry() }
                    )

                    DeviceStatusBadge(
                        title = "Status",
                        status = if (isCharging) "CHARGING" else "DISCHARGING",
                        icon = Icons.Default.Thermostat,
                        color = if (isCharging) JarvisGreen else JarvisTextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSystemSettings("battery") }
                    )
                }
            }
        }

        // 4. Sensors & Gyroscope Telemetry
        item {
            HolographicSectionCard(title = "SPATIAL SENSORS & GYROSCOPE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x66060F20))
                            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = "Pitch", tint = JarvisCyan, modifier = Modifier.size(16.dp))
                                Text("PITCH AXIS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
                            }
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", deviceTilt.first)}°",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyanBright
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x66060F20))
                            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Explore, contentDescription = "Roll", tint = JarvisElectricBlue, modifier = Modifier.size(16.dp))
                                Text("ROLL AXIS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
                            }
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", deviceTilt.second)}°",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisElectricBlue
                            )
                        }
                    }
                }
            }
        }

        // 5. Tactical Application Launcher Matrix
        item {
            HolographicSectionCard(title = "APPLICATIONS & DISPATCH MATRIX") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppShortcutButton(
                        name = "WhatsApp",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = Color(0xFF25D366),
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp("com.whatsapp") }
                    )

                    AppShortcutButton(
                        name = "Camera",
                        icon = Icons.Default.CameraAlt,
                        color = JarvisCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp("camera") }
                    )

                    AppShortcutButton(
                        name = "Maps",
                        icon = Icons.Default.Map,
                        color = JarvisAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchApp("maps") }
                    )

                    AppShortcutButton(
                        name = "Settings",
                        icon = Icons.Default.Settings,
                        color = JarvisTextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSystemSettings(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HolographicSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xDE081124),
                        Color(0xF2040914)
                    )
                )
            )
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            color = JarvisCyan
        )
        content()
    }
}

@Composable
private fun HolographicToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    val bg = if (isActive) Color(0x3300E5FF) else Color(0x330A1424)
    val border = if (isActive) activeColor else JarvisBorderSubtle

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onToggle(!isActive) }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) activeColor else JarvisTextDim,
                    modifier = Modifier.size(22.dp)
                )

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = activeColor,
                        uncheckedThumbColor = JarvisTextDim,
                        uncheckedTrackColor = Color(0xFF131E30)
                    ),
                    modifier = Modifier.size(36.dp, 20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = JarvisTextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) activeColor else JarvisTextDim
            )
        }
    }
}

@Composable
private fun DeviceStatusBadge(
    title: String,
    status: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x66060F20))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = title,
                fontSize = 10.sp,
                color = JarvisTextSecondary
            )

            Text(
                text = status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}

@Composable
private fun AppShortcutButton(
    name: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x66060F20))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = name,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = JarvisTextPrimary
            )
        }
    }
}
