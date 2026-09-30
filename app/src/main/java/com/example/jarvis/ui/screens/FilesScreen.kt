package com.example.jarvis.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisPurpleHighlight
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedFileModel(
    val file: File,
    val name: String,
    val extension: String,
    val sizeFormatted: String,
    val dateFormatted: String
)

@Composable
fun FilesScreen(
    onGenerateFilePrompt: (String) -> Unit = {},
    onCopyToClipboard: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "PDF", "DOCX", "XLSX", "CSV", "JSON", "TXT", "MD")
    var quickGenInput by remember { mutableStateOf("") }

    // Sample/Discovered generated files list
    val mockGeneratedFiles = remember {
        listOf(
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/jarvis_report_2026.pdf"),
                name = "jarvis_system_audit.pdf",
                extension = "pdf",
                sizeFormatted = "142 KB",
                dateFormatted = "Today, 10:15 AM"
            ),
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/executive_summary.docx"),
                name = "executive_summary.docx",
                extension = "docx",
                sizeFormatted = "88 KB",
                dateFormatted = "Today, 09:30 AM"
            ),
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/telemetry_metrics.xlsx"),
                name = "telemetry_metrics.xlsx",
                extension = "xlsx",
                sizeFormatted = "54 KB",
                dateFormatted = "Yesterday, 04:20 PM"
            ),
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/activity_export.csv"),
                name = "activity_export.csv",
                extension = "csv",
                sizeFormatted = "12 KB",
                dateFormatted = "Yesterday, 02:10 PM"
            ),
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/device_config.json"),
                name = "device_config.json",
                extension = "json",
                sizeFormatted = "4.2 KB",
                dateFormatted = "18 Sep, 11:00 PM"
            ),
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/notes_archive.txt"),
                name = "notes_archive.txt",
                extension = "txt",
                sizeFormatted = "8.1 KB",
                dateFormatted = "18 Sep, 08:45 AM"
            ),
            GeneratedFileModel(
                file = File("/data/user/0/com.example/files/README.md"),
                name = "system_architecture.md",
                extension = "md",
                sizeFormatted = "15.4 KB",
                dateFormatted = "17 Sep, 03:20 PM"
            )
        )
    }

    val filteredFiles = remember(selectedFilter) {
        if (selectedFilter == "ALL") mockGeneratedFiles
        else mockGeneratedFiles.filter { it.extension.equals(selectedFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xEE0D162A),
                                Color(0xCC080D1A)
                            )
                        )
                    )
                    .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.12f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Files",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "DOCUMENT & FILE HUB",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyanBright,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "PDF • DOCX • XLSX • CSV • JSON • TXT • MD",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }
        }

        // 2. Quick File Generator Input
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC090F1E))
                    .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QUICK FILE GENERATION PIPELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickGenInput,
                            onValueChange = { quickGenInput = it },
                            placeholder = {
                                Text(
                                    "e.g., Save as pdf called summary.pdf",
                                    fontSize = 12.sp,
                                    color = JarvisTextDim
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("file_gen_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisBorderSubtle,
                                focusedContainerColor = Color(0xFF070B14),
                                unfocusedContainerColor = Color(0xFF070B14),
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (quickGenInput.isNotBlank()) {
                                    onGenerateFilePrompt(quickGenInput)
                                    quickGenInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("file_gen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Generate",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Format Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) JarvisCyan.copy(alpha = 0.2f) else Color(0xFF0D1526)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) JarvisCyan else JarvisBorderSubtle,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) JarvisCyanBright else JarvisTextSecondary
                        )
                    }
                }
            }
        }

        // 4. File Cards List
        items(filteredFiles, key = { it.name }) { fileModel ->
            FileCardItem(
                fileModel = fileModel,
                onCopyPath = { onCopyToClipboard(fileModel.file.absolutePath) }
            )
        }
    }
}

@Composable
private fun FileCardItem(
    fileModel: GeneratedFileModel,
    onCopyPath: () -> Unit
) {
    val ext = fileModel.extension.lowercase()
    val (formatColor, icon) = when (ext) {
        "pdf" -> JarvisRed to Icons.Default.PictureAsPdf
        "docx" -> JarvisElectricBlue to Icons.Default.Description
        "xlsx" -> JarvisGreen to Icons.Default.TableChart
        "csv" -> JarvisAmber to Icons.Default.TableChart
        "json" -> JarvisPurpleHighlight to Icons.AutoMirrored.Filled.InsertDriveFile
        "txt" -> JarvisCyan to Icons.AutoMirrored.Filled.InsertDriveFile
        "md" -> JarvisCyanBright to Icons.Default.Description
        else -> JarvisCyan to Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE0B1222))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(formatColor.copy(alpha = 0.15f))
                        .border(1.dp, formatColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = fileModel.extension,
                        tint = formatColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileModel.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTextPrimary,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(formatColor.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = fileModel.extension.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = formatColor
                            )
                        }

                        Text(
                            text = "${fileModel.sizeFormatted} • ${fileModel.dateFormatted}",
                            fontSize = 11.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }

            IconButton(onClick = onCopyPath) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Path",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
