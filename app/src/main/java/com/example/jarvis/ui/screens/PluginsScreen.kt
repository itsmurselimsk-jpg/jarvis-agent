package com.example.jarvis.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jarvis.plugin.Plugin
import com.example.jarvis.plugin.PluginCapability
import com.example.jarvis.plugin.PluginHealthCheck
import com.example.jarvis.plugin.PluginManager
import com.example.jarvis.plugin.PluginState
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisSurface
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PluginsScreen(
    pluginManager: PluginManager,
    onBack: (() -> Unit)? = null
) {
    val plugins by pluginManager.pluginsState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedPluginForConfig by remember { mutableStateOf<Plugin?>(null) }
    var healthTestResults by remember { mutableStateOf<Map<String, PluginHealthCheck>>(emptyMap()) }
    var testingPluginId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .testTag("plugins_screen")
    ) {
        // Subsystem Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CONNECTED SERVICES & PLUGINS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )
                    Text(
                        text = "Sandboxed external service adapters & encrypted integrations",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextSecondary
                    )
                }
            }

            // Summary badge
            val enabledCount = plugins.count { pluginManager.registry.isPluginEnabled(it.manifest.id) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F2634))
                    .border(0.5.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$enabledCount/${plugins.size} ACTIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyanBright
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Notice Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D1B2A))
                .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = JarvisGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SANDBOXED: Zero shell/OS execution. Output is treated as untrusted data. Credentials encrypted via Keystore.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Installed Plugins List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(plugins, key = { it.manifest.id }) { plugin ->
                val isEnabled = pluginManager.registry.isPluginEnabled(plugin.manifest.id)
                val status = pluginManager.registry.getPluginStatus(plugin.manifest.id)
                val lastHealthCheck = healthTestResults[plugin.manifest.id]
                val isTesting = testingPluginId == plugin.manifest.id

                PluginItemCard(
                    plugin = plugin,
                    isEnabled = isEnabled,
                    status = status,
                    lastHealthCheck = lastHealthCheck,
                    isTesting = isTesting,
                    onToggleEnabled = { enabled ->
                        pluginManager.togglePluginEnabled(plugin.manifest.id, enabled)
                    },
                    onTestConnection = {
                        scope.launch {
                            testingPluginId = plugin.manifest.id
                            val check = pluginManager.testConnection(plugin.manifest.id)
                            healthTestResults = healthTestResults + (plugin.manifest.id to check)
                            testingPluginId = null
                        }
                    },
                    onConfigure = {
                        selectedPluginForConfig = plugin
                    },
                    onRevokeAccess = {
                        pluginManager.revokeAccess(plugin.manifest.id)
                        healthTestResults = healthTestResults - plugin.manifest.id
                    }
                )
            }
        }
    }

    // Configure Credential Dialog
    selectedPluginForConfig?.let { plugin ->
        ConfigurePluginDialog(
            plugin = plugin,
            currentHasCredential = !pluginManager.registry.getDecryptedCredential(plugin.manifest.id).isNullOrBlank(),
            onDismiss = { selectedPluginForConfig = null },
            onSaveCredential = { rawSecret ->
                pluginManager.saveCredential(plugin.manifest.id, rawSecret)
                selectedPluginForConfig = null
            }
        )
    }
}

@Composable
private fun PluginItemCard(
    plugin: Plugin,
    isEnabled: Boolean,
    status: com.example.jarvis.plugin.PluginStatus,
    lastHealthCheck: PluginHealthCheck?,
    isTesting: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onConfigure: () -> Unit,
    onRevokeAccess: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plugin_card_${plugin.manifest.id}"),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) JarvisCyan.copy(alpha = 0.4f) else JarvisBorderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Icon, Title, Version, and Enable Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (plugin.manifest.category == com.example.jarvis.plugin.PluginCategory.PRODUCTIVITY) Icons.Default.Cloud else Icons.Default.Extension,
                        contentDescription = null,
                        tint = if (isEnabled) JarvisCyan else JarvisTextDim,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.manifest.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isEnabled) JarvisTextPrimary else JarvisTextDim
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v${plugin.manifest.version}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextSecondary
                            )
                        }
                        Text(
                            text = "${plugin.manifest.providerName} • ${plugin.manifest.category.name}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextSecondary
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisCyanBright,
                        checkedTrackColor = JarvisCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = JarvisTextDim,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("switch_${plugin.manifest.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = plugin.manifest.description,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Capabilities Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(plugin.manifest.capabilities.toList()) { cap ->
                    CapabilityBadge(cap)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status & Diagnostics Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF090E17))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when {
                        !isEnabled -> JarvisTextDim
                        status.isHealthy -> JarvisGreen
                        else -> JarvisRed
                    }
                    val statusText = when {
                        !isEnabled -> "DISABLED"
                        status.isHealthy -> "HEALTHY"
                        else -> "ERROR (${status.lastErrorCode ?: "UNKNOWN"})"
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }

                if (status.lastSyncTimestamp != null) {
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(status.lastSyncTimestamp))
                    Text(
                        text = "Last Sync: $timeStr",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextSecondary
                    )
                }
            }

            // Health check result banner if present
            lastHealthCheck?.let { check ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (check.isHealthy) Color(0xFF0D2818) else Color(0xFF330C0C))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (check.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (check.isHealthy) JarvisGreen else JarvisRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${check.message} (${check.latencyMs}ms)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (check.isHealthy) JarvisGreen else JarvisRed
                    )
                }
            }

            status.lastErrorMessage?.let { errMsg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Error: $errMsg",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Connection
                Button(
                    onClick = onTestConnection,
                    enabled = isEnabled && !isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162A3D)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("test_btn_${plugin.manifest.id}")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = JarvisCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TEST",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan
                        )
                    }
                }

                // Configure Credentials
                if (plugin.manifest.requiresCredentials) {
                    Button(
                        onClick = onConfigure,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A332B)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("config_btn_${plugin.manifest.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = JarvisGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CONFIG",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = JarvisGreen
                        )
                    }
                }

                // Revoke / Disconnect
                OutlinedButton(
                    onClick = onRevokeAccess,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisRed),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, JarvisRed.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("revoke_btn_${plugin.manifest.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Revoke",
                        tint = JarvisRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityBadge(capability: PluginCapability) {
    val color = when (capability) {
        PluginCapability.READ, PluginCapability.SEARCH -> JarvisCyan
        PluginCapability.CREATE, PluginCapability.WRITE, PluginCapability.UPDATE -> JarvisGreen
        PluginCapability.DELETE -> JarvisRed
        PluginCapability.ACCOUNT_DATA, PluginCapability.SENSITIVE_DATA -> JarvisAmber
        PluginCapability.NETWORK -> Color(0xFF64B5F6)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = capability.name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun ConfigurePluginDialog(
    plugin: Plugin,
    currentHasCredential: Boolean,
    onDismiss: () -> Unit,
    onSaveCredential: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("configure_plugin_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIGURE: ${plugin.manifest.displayName.uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enter API token / authentication secret for this service. Secrets are encrypted using Android Keystore AES-256-GCM and never logged in plaintext.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    placeholder = {
                        Text(
                            text = plugin.manifest.credentialPlaceholder ?: "Enter authentication secret...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("credential_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderSubtle,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true
                )

                if (currentHasCredential) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Existing credentials currently stored in Keystore",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisGreen
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisTextSecondary)
                    ) {
                        Text("CANCEL", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (rawInput.isNotBlank()) {
                                onSaveCredential(rawInput.trim())
                            }
                        },
                        enabled = rawInput.isNotBlank(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        modifier = Modifier.testTag("save_credential_btn")
                    ) {
                        Text(
                            text = "SAVE ENCRYPTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
