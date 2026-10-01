package com.jarvis.ai.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jarvis.ai.privacy.PrivacyAuditor
import com.jarvis.ai.ui.theme.JarvisAmber
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBorderSubtle
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisGreen
import com.jarvis.ai.ui.theme.JarvisRed
import com.jarvis.ai.ui.theme.JarvisTextDim
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(
    auditor: PrivacyAuditor,
    onOpenSettings: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val telemetry = remember { auditor.getPrivacyTelemetry() }

    val memCount by auditor.memoryCountFlow.collectAsState(initial = 0)
    val taskCount by auditor.taskCountFlow.collectAsState(initial = 0)
    val notifCount by auditor.notificationCountFlow.collectAsState(initial = 0)
    val msgCount by auditor.messageCountFlow.collectAsState(initial = 0)

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var wipeTarget by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PRIVACY & SECURITY ENCLAVE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Text(
                    text = "Strict local isolation. Zero unencrypted credential caching.",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        // 1. Permission Clearances
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "HARDWARE SENSORS & SYSTEM CLEARANCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )

                    PrivacyRowItem(
                        title = "Microphone Sensor (Acoustic Array)",
                        status = if (telemetry.hasMicPermission) "AUTHORIZED (Runtime)" else "STANDBY / NOT GRANTED",
                        isPositive = telemetry.hasMicPermission,
                        actionLabel = if (!telemetry.hasMicPermission) "GRANT" else null,
                        onAction = { onOpenSettings("app_details") }
                    )

                    PrivacyRowItem(
                        title = "Optical Camera Sensor",
                        status = if (telemetry.hasCameraPermission) "AUTHORIZED (Runtime)" else "STANDBY / NOT GRANTED",
                        isPositive = telemetry.hasCameraPermission,
                        actionLabel = if (!telemetry.hasCameraPermission) "GRANT" else null,
                        onAction = { onOpenSettings("app_details") }
                    )

                    PrivacyRowItem(
                        title = "Notification Interceptor Service",
                        status = if (telemetry.isNotificationAccessEnabled) "CONNECTED (Active Listener)" else "DISABLED IN OS",
                        isPositive = telemetry.isNotificationAccessEnabled,
                        actionLabel = if (!telemetry.isNotificationAccessEnabled) "ENABLE" else null,
                        onAction = { onOpenSettings("notifications") }
                    )

                    PrivacyRowItem(
                        title = "Accessibility Agent Service",
                        status = if (telemetry.isAccessibilityServiceEnabled) "CONNECTED (Active Bridge)" else "DISABLED IN OS",
                        isPositive = telemetry.isAccessibilityServiceEnabled,
                        actionLabel = if (!telemetry.isAccessibilityServiceEnabled) "ENABLE" else null,
                        onAction = { onOpenSettings("accessibility") }
                    )

                    PrivacyRowItem(
                        title = "Storage Architecture",
                        status = "ZERO-PERMISSION MEDIA PICKER (Compliant)",
                        isPositive = true,
                        actionLabel = null,
                        onAction = {}
                    )

                    PrivacyRowItem(
                        title = "Active AI Brain Model",
                        status = telemetry.activeProviderName,
                        isPositive = true,
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }
        }

        // Acoustic Shield & Secret Redaction Guard (JARVIS Intelligence Enclave)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07111E))
                    .border(0.5.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "INTELLIGENCE ENCLAVE & ACOUSTIC SHIELD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )

                    PrivacyRowItem(
                        title = "Acoustic Echo Shield",
                        status = "ONLINE (Suppresses self-listening feedback from TTS)",
                        isPositive = true,
                        actionLabel = null,
                        onAction = {}
                    )

                    PrivacyRowItem(
                        title = "Secret Redaction Guard",
                        status = "ACTIVE (AWS, OpenAI, Stripe, JWT & Card scrubbing)",
                        isPositive = true,
                        actionLabel = null,
                        onAction = {}
                    )

                    PrivacyRowItem(
                        title = "Total Redacted Tokens",
                        status = "${com.jarvis.ai.security.PrivacyRedactionGuard.totalRedactions} sensitive items sanitized",
                        isPositive = true,
                        actionLabel = null,
                        onAction = {}
                    )

                    PrivacyRowItem(
                        title = "Compound Task Planner",
                        status = "ONLINE (Multi-step query decomposition & execution)",
                        isPositive = true,
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }
        }

        // 2. Persisted Local Footprint
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ON-DEVICE STORED ASSETS (ROOM DATABASE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )

                    DataMetricRow(label = "Encrypted User Memories (Room)", count = "$memCount items") {
                        wipeTarget = "MEMORIES"
                        showWipeConfirmDialog = true
                    }
                    DataMetricRow(label = "Scheduled Agenda Tasks (Room)", count = "$taskCount tasks") {
                        wipeTarget = "TASKS"
                        showWipeConfirmDialog = true
                    }
                    DataMetricRow(label = "Notification Intercept Logs (Room)", count = "$notifCount events") {
                        wipeTarget = "NOTIFS"
                        showWipeConfirmDialog = true
                    }
                    DataMetricRow(label = "Live Conversation Buffer", count = "$msgCount messages") {
                        wipeTarget = "CHAT"
                        showWipeConfirmDialog = true
                    }
                }
            }
        }

        // 3. Complete Enclave Purge
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14080D))
                    .border(1.dp, JarvisRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = JarvisRed, modifier = Modifier.size(18.dp))
                        Text(
                            text = "EMERGENCY DATA PURGE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Purge all stored conversation history, agenda items, Room memories, and notification logs from this hardware layer immediately.",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            wipeTarget = "ALL"
                            showWipeConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wipe_data_button")
                    ) {
                        Text("PURGE ALL ENCLAVE DATA", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showWipeConfirmDialog) {
        Dialog(onDismissRequest = { showWipeConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0A12))
                    .border(1.5.dp, JarvisRed, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "CONFIRM PURGE PROTOCOL?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to permanently erase $wipeTarget? This action cannot be reversed.",
                        fontSize = 12.sp,
                        color = JarvisTextPrimary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showWipeConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("CANCEL")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    when (wipeTarget) {
                                        "MEMORIES" -> auditor.purgeAllMemories()
                                        "TASKS" -> auditor.purgeAllTasks()
                                        "NOTIFS" -> auditor.purgeAllNotifications()
                                        "CHAT" -> auditor.purgeAllConversation()
                                        else -> auditor.purgeAllLocalData()
                                    }
                                    showWipeConfirmDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisRed, contentColor = Color.White)
                        ) {
                            Text("CONFIRM PURGE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyRowItem(
    title: String,
    status: String,
    isPositive: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, color = JarvisTextPrimary)
            Text(
                text = status,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (isPositive) JarvisGreen else JarvisAmber
            )
        }
        if (actionLabel != null) {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(actionLabel, fontSize = 10.sp, color = JarvisCyan)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) JarvisGreen else JarvisAmber)
            )
        }
    }
}

@Composable
private fun DataMetricRow(
    label: String,
    count: String,
    onPurge: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = JarvisTextSecondary, modifier = Modifier.weight(1f))
        Text(text = count, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = JarvisCyan)
    }
}
