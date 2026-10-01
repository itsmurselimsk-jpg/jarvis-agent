package com.jarvis.ai.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.jarvis.ai.notification.JarvisNotificationListenerService
import com.jarvis.ai.ui.theme.JarvisAmber
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBorderSubtle
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisCyanBright
import com.jarvis.ai.ui.theme.JarvisGreen
import com.jarvis.ai.ui.theme.JarvisSurfaceElevated
import com.jarvis.ai.ui.theme.JarvisTextDim
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary

/**
 * Protocol permission model representing each deep capability.
 */
data class PermissionProtocol(
    val id: String,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val isCrucial: Boolean,
    val explanation: String,
    val benefit: String,
    val isGranted: (Context) -> Boolean,
    val onRequest: (Context, () -> Unit) -> Unit
)

/**
 * Beautiful, first-launch Permission Onboarding Screen for JARVIS.
 * Transparently articulates why each capability is requested, with real-time
 * lifecycle detection, glowing indicators, and seamless settings dispatch.
 */
@Composable
fun PermissionOnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh trigger that fires whenever user returns from Android System Settings
    var refreshCounter by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Launchers
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshCounter++
    }

    val notifPostLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshCounter++
    }

    // List of system protocols
    val protocols = remember(refreshCounter) {
        listOf(
            PermissionProtocol(
                id = "microphone",
                title = "Vocal Core & Wake Word",
                category = "AUDIO SUBSYSTEM",
                icon = Icons.Default.Mic,
                isCrucial = true,
                explanation = "Requires low-latency microphone access to stream live voice conversations, detect speech waveforms, and respond to 'Hey JARVIS' wake phrases.",
                benefit = "Enables full hands-free vocal dialogue and acoustic interruption.",
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { _, _ ->
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            ),
            PermissionProtocol(
                id = "overlay",
                title = "Holographic Arc Overlay",
                category = "DISPLAY SUBSYSTEM",
                icon = Icons.Default.Layers,
                isCrucial = true,
                explanation = "Allows drawing the floating Stark Arc Orb and Heads-Up Display directly over third-party applications, games, and system menus.",
                benefit = "Instant voice trigger orb floating on top of any active screen.",
                isGranted = { ctx ->
                    Settings.canDrawOverlays(ctx)
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    try {
                        ctx.startActivity(intent)
                    } catch (_: Exception) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            ),
            PermissionProtocol(
                id = "accessibility",
                title = "Cybernetic Bridge (Screen & UI)",
                category = "AUTOMATION SUBSYSTEM",
                icon = Icons.Default.AccessibilityNew,
                isCrucial = true,
                explanation = "Powers automated UI navigation, button clicks, reading screen content on demand, and executing complex device automation protocols.",
                benefit = "Autonomous task execution like 'Open Settings and turn on Hotspot'.",
                isGranted = { ctx ->
                    JarvisAccessibilityService.isServiceEnabled(ctx)
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionProtocol(
                id = "notification_listener",
                title = "Neural Telemetry & Screening",
                category = "INTELLIGENCE SUBSYSTEM",
                icon = Icons.Default.NotificationsActive,
                isCrucial = false,
                explanation = "Reads incoming notifications to synthesize executive briefings, detect high-priority VIP messages (WhatsApp, SMS, Slack), and filter spam.",
                benefit = "Proactive alerts when an urgent communication arrives.",
                isGranted = { ctx ->
                    JarvisNotificationListenerService.isNotificationAccessEnabled(ctx)
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionProtocol(
                id = "post_notifications",
                title = "System Foreground Dispatch",
                category = "NOTIFICATION SUBSYSTEM",
                icon = Icons.Default.Shield,
                isCrucial = false,
                explanation = "Maintains persistent background telemetry and displays proactive alarm reminders and wake word status.",
                benefit = "Keeps voice and automation services active in the background.",
                isGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                },
                onRequest = { _, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPostLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        )
    }

    val grantedCount = protocols.count { it.isGranted(context) }
    val crucialGranted = protocols.filter { it.isCrucial }.all { it.isGranted(context) }
    val progress by animateFloatAsState(
        targetValue = grantedCount.toFloat() / protocols.size.toFloat(),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "onboarding_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("permission_onboarding_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Cybernetic HUD Emblem
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Arc Reactor Emblem
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        JarvisCyanBright.copy(alpha = 0.35f),
                                        Color(0x1100E5FF),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, JarvisCyanBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Core",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SYSTEM INITIALIZATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = JarvisCyan
                    )

                    Text(
                        text = "Security & Capabilities Onboarding",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = JarvisTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "JARVIS requires deep operating system bridges to see your screen, speak in real time, and execute commands on your behalf.",
                        fontSize = 12.sp,
                        color = JarvisTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progress Overview Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisSurfaceElevated)
                            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PROTOCOLS VERIFIED",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = JarvisTextSecondary
                                )
                                Text(
                                    text = "$grantedCount / ${protocols.size} GRANTED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (crucialGranted) JarvisGreen else JarvisAmber
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (crucialGranted) JarvisGreen else JarvisCyanBright,
                                trackColor = Color(0x3310243C)
                            )
                        }
                    }
                }
            }

            // Protocol Cards List
            items(protocols, key = { it.id }) { protocol ->
                val isGranted = protocol.isGranted(context)
                ProtocolCard(
                    protocol = protocol,
                    isGranted = isGranted,
                    onEnable = { protocol.onRequest(context) {} }
                )
            }

            // Bottom Spacer to prevent overlap with sticky launch bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Sticky Bottom Initialization Action Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, JarvisBackground, JarvisBackground)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("permission_onboarding_completed", true).apply()
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("initialize_core_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (crucialGranted) JarvisCyanBright else Color(0x4400E5FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (crucialGranted) "INITIALIZE JARVIS CORE" else "CONTINUE WITH CONFIGURED PROTOCOLS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = if (crucialGranted) Color.Black else JarvisCyanBright
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (crucialGranted) Color.Black else JarvisCyanBright
                    )
                }
            }
        }
    }
}

/**
 * Individual Capability Permission Card with interactive status and expandable details.
 */
@Composable
private fun ProtocolCard(
    protocol: PermissionProtocol,
    isGranted: Boolean,
    onEnable: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isGranted) JarvisGreen.copy(alpha = 0.5f) else JarvisBorderSubtle,
        label = "border_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisSurfaceElevated)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Category Badge + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = protocol.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = JarvisCyan
                    )

                    if (protocol.isCrucial) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33FFB300))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "CRUCIAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisAmber
                            )
                        }
                    }
                }

                // Granted or Pending Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGranted) Color(0x3300E676) else Color(0x22FFA000))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isGranted) JarvisGreen else JarvisAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isGranted) "AUTHORIZED" else "PENDING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isGranted) JarvisGreen else JarvisAmber
                        )
                    }
                }
            }

            // Main Capability Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) Color(0x2200E676) else Color(0x2200E5FF))
                        .border(1.dp, if (isGranted) JarvisGreen else JarvisCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = protocol.icon,
                        contentDescription = protocol.title,
                        tint = if (isGranted) JarvisGreen else JarvisCyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = protocol.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTextPrimary
                    )
                    Text(
                        text = protocol.explanation,
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // Benefit Accent Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33091424))
                    .border(0.5.dp, Color(0x2200E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "BENEFIT:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyanBright
                    )
                    Text(
                        text = protocol.benefit,
                        fontSize = 10.sp,
                        color = JarvisTextDim
                    )
                }
            }

            // Action Button
            if (!isGranted) {
                Button(
                    onClick = onEnable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "GRANT PERMISSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = JarvisCyanBright
                    )
                }
            }
        }
    }
}
