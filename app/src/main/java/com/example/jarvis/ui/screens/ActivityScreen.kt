package com.example.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.ActivityLog
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    logs: List<ActivityLog>,
    onClearLogs: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<ActivityType?>(null) }

    val filtered = if (selectedFilter == null) logs else logs.filter { it.type == selectedFilter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OPERATING LAYER ACTIVITY LOG",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Text(
                    text = "Real-time dispatch & safety audits",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(onClick = onClearLogs, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs", tint = JarvisTextDim)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterPill(
                    label = "ALL",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
            }
            items(ActivityType.values()) { type ->
                FilterPill(
                    label = type.name.replace("_", " "),
                    isSelected = selectedFilter == type,
                    onClick = { selectedFilter = type }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Log Entries
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No recorded activity logs.", color = JarvisTextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            } else {
                items(filtered, key = { it.id }) { log ->
                    ActivityLogItem(log = log)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else Color(0xFF090F1D))
            .border(0.5.dp, if (isSelected) JarvisCyan else JarvisBorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) JarvisCyan else JarvisTextSecondary
        )
    }
}

@Composable
private fun ActivityLogItem(log: ActivityLog) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = timeFormat.format(Date(log.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090E1A))
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Type dot
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        when (log.type) {
                            ActivityType.TOOL_EXECUTION -> JarvisCyan
                            ActivityType.VOICE_EVENT -> JarvisGreen
                            ActivityType.SAFETY_ALERT -> if (log.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber
                            ActivityType.TASK_EVENT -> JarvisCyan
                            ActivityType.SYSTEM_EVENT -> Color(0xFF818CF8)
                        }
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisTextPrimary
                    )
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )
                }

                Text(
                    text = log.detail,
                    fontSize = 11.sp,
                    color = JarvisTextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF0F1E33))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = log.type.name.replace("_", " "),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan
                        )
                    }
                    if (log.riskLevel != RiskLevel.SAFE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(JarvisAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "RISK: ${log.riskLevel.name}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
