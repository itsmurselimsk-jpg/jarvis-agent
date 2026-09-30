package com.example.jarvis.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.service.JarvisVoiceService
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

@Composable
fun VoiceSetupScreen(
    currentSettings: ProviderSettings,
    onUpdateSettings: (ProviderSettings) -> Unit,
    onTestWakeTrigger: () -> Unit
) {
    val context = LocalContext.current
    val isServiceRunning by JarvisVoiceService.isServiceRunning.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var canDrawOverlay by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    var isBatteryOptimizedExempt by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else true
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "ALWAYS-AVAILABLE 'HEY JARVIS' WAKE ENGINE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Text(
                    text = "Acoustic grid active across all apps, games, and standby modes",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        // Service Master Control Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isServiceRunning) Color(0xFF071F2C) else Color(0xFF140D17))
                    .border(
                        1.dp,
                        if (isServiceRunning) JarvisCyan else JarvisBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) JarvisGreen else JarvisAmber)
                            )
                            Column {
                                Text(
                                    text = if (isServiceRunning) "ACOUSTIC GRID ONLINE" else "BACKGROUND GRID IDLE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isServiceRunning) JarvisCyanBright else JarvisTextSecondary
                                )
                                Text(
                                    text = if (isServiceRunning) "Foreground microphonic loop active" else "Service offline",
                                    fontSize = 10.sp,
                                    color = JarvisTextDim
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isServiceRunning) {
                                    JarvisVoiceService.stop(context)
                                } else {
                                    if (!hasMicPermission) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        JarvisVoiceService.start(context)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServiceRunning) Color(0xFF3B1017) else JarvisCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_voice_service_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isServiceRunning) JarvisRed else Color.Black
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isServiceRunning) "Deactivate" else "Activate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) JarvisRed else Color.Black
                            )
                        }
                    }

                    // Test Wake Word Button
                    OutlinedButton(
                        onClick = onTestWakeTrigger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_wake_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, JarvisCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Simulate Voice Wake ('Hey JARVIS')",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan
                        )
                    }
                }
            }
        }

        // Diagnostics / Permissions
        item {
            Text(
                text = "SYSTEM PERMISSIONS & HARDWARE BRIDGES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary
            )
        }

        // 1. Microphone
        item {
            PermissionCard(
                title = "Acoustic Microphone Stream",
                detail = "Mandatory for passive wake-word detection and voice directives",
                icon = Icons.Default.Mic,
                isGranted = hasMicPermission,
                actionLabel = "Grant",
                onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        }

        // 2. Notification
        item {
            PermissionCard(
                title = "Foreground Notification Anchor",
                detail = "Prevents Android OS from killing the voice process during games & idle",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPermission,
                actionLabel = "Grant",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        // 3. Floating Overlay
        item {
            PermissionCard(
                title = "Display Over Other Apps (HUD)",
                detail = "Renders glowing JARVIS pill over games, YouTube, or full-screen apps",
                icon = Icons.Default.Layers,
                isGranted = canDrawOverlay,
                actionLabel = "Configure",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }
            )
        }

        // 4. Battery Optimization Exemption
        item {
            PermissionCard(
                title = "Battery Unrestricted Mode",
                detail = "Ensures wake-word detection does not sleep when screen is locked",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = isBatteryOptimizedExempt,
                actionLabel = "Allow",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val alt = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(alt)
                        }
                    }
                }
            )
        }

        // Voice Behavior Switches
        item {
            Text(
                text = "NEURAL LISTENING BEHAVIORS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Continuous Wake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continuous Acoustic Wake",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JarvisTextPrimary
                            )
                            Text(
                                text = "Keep listening loop armed across reboots and app switches",
                                fontSize = 10.sp,
                                color = JarvisTextDim
                            )
                        }
                        Switch(
                            checked = currentSettings.continuousWakeEnabled,
                            onCheckedChange = { onUpdateSettings(currentSettings.copy(continuousWakeEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = Color(0xFF0E384D)
                            )
                        )
                    }

                    // Continuous Conversation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continuous Conversation Follow-ups",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JarvisTextPrimary
                            )
                            Text(
                                text = "Automatically listen for next question after speaking response",
                                fontSize = 10.sp,
                                color = JarvisTextDim
                            )
                        }
                        Switch(
                            checked = currentSettings.continuousConversationEnabled,
                            onCheckedChange = { onUpdateSettings(currentSettings.copy(continuousConversationEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = Color(0xFF0E384D)
                            )
                        )
                    }

                    // Lock Screen Wake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock Screen Acoustic Response",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JarvisTextPrimary
                            )
                            Text(
                                text = "Respond to wake phrases even when device display is sleeping",
                                fontSize = 10.sp,
                                color = JarvisTextDim
                            )
                        }
                        Switch(
                            checked = currentSettings.lockScreenWakeEnabled,
                            onCheckedChange = { onUpdateSettings(currentSettings.copy(lockScreenWakeEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = Color(0xFF0E384D)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    detail: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF090E1A))
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) JarvisGreen else JarvisAmber,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisTextPrimary
                    )
                    Text(
                        text = detail,
                        fontSize = 10.sp,
                        color = JarvisTextDim
                    )
                }
            }

            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = JarvisGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisGreen
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B263B)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyan
                    )
                }
            }
        }
    }
}
