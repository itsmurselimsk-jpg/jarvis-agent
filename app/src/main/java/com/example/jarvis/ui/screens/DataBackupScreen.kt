package com.example.jarvis.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.jarvis.storage.BackupStats
import com.example.jarvis.storage.JarvisBackupManager
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager
import com.example.jarvis.voice.StarkSoundEngine
import kotlinx.coroutines.launch

@Composable
fun DataBackupScreen(
    repository: JarvisRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val backupManager = remember { JarvisBackupManager(context, repository) }
    val scope = rememberCoroutineScope()

    var backupStats by remember { mutableStateOf<BackupStats?>(null) }
    var importJsonText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    fun refreshStats() {
        scope.launch {
            backupStats = backupManager.createFullBackupJson()
        }
    }

    LaunchedEffect(Unit) {
        refreshStats()
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
                    text = "STARK ENCRYPTED DATA VAULT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Text(
                    text = "JSON BACKUP & PROTOCOL RESTORE",
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
            // Stats Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(activeTheme.primaryColor.copy(alpha = 0.20f), Color(0xFF0F172A))
                            )
                        )
                        .border(1.dp, activeTheme.borderColor, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE DATABASE FOOTPRINT",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextSecondary
                            )
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = activeTheme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        backupStats?.let { stats ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${stats.memoriesCount} Memory Notes", fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                                    Text("${stats.tasksCount} Tasks", fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                                    Text("${stats.expensesCount} Expenses", fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("${stats.habitsCount} Habit Streaks", fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                                    Text("${stats.chatMessagesCount} Messages", fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Export Actions
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0D1526))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "EXPORT BACKUP ARCHIVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = activeTheme.accentColor
                        )
                        Text(
                            text = "Export complete local Room database into raw readable JSON.",
                            fontSize = 11.sp,
                            color = JarvisTextDim
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    backupStats?.let { stats ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("JARVIS_BACKUP", stats.jsonString)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Full Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        StarkSoundEngine.playConfirmationChime()
                                        statusMessage = "Backup JSON successfully copied to clipboard!"
                                        isSuccess = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeTheme.primaryColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("COPY JSON", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { refreshStats(); StarkSoundEngine.playCyberBeep() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Refresh", tint = activeTheme.accentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("REFRESH", color = activeTheme.accentColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Restore from JSON
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0D1526))
                        .border(1.dp, activeTheme.borderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "RESTORE DATABASE FROM JSON",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = activeTheme.accentColor
                        )

                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            label = { Text("Paste JSON Backup String Here", color = JarvisTextDim) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeTheme.accentColor,
                                unfocusedBorderColor = activeTheme.borderColor,
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    scope.launch {
                                        val res = backupManager.restoreFromJson(importJsonText.trim())
                                        if (res.success) {
                                            statusMessage = "Successfully imported ${res.importedMemories} memory notes, ${res.importedTasks} tasks, ${res.importedExpenses} expenses, and ${res.importedHabits} habits!"
                                            isSuccess = true
                                            importJsonText = ""
                                            refreshStats()
                                            StarkSoundEngine.playConfirmationChime()
                                        } else {
                                            statusMessage = "Import Failed: ${res.errorMessage}"
                                            isSuccess = false
                                            StarkSoundEngine.playWarningTone()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (importJsonText.isNotBlank()) activeTheme.primaryColor else Color(0xFF1E293B),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTORE ARCHIVE DATA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Status message
            statusMessage?.let { msg ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSuccess) Color(0x2210B981) else Color(0x22EF4444))
                            .border(1.dp, if (isSuccess) JarvisGreen else JarvisRed, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSuccess) JarvisGreen else JarvisRed
                        )
                    }
                }
            }
        }
    }
}
