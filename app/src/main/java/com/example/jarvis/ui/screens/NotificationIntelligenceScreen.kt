package com.example.jarvis.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.jarvis.notification.JarvisNotification
import com.example.jarvis.notification.JarvisNotificationListenerService
import com.example.jarvis.notification.NotificationCategory
import com.example.jarvis.notification.NotificationIntelligenceEngine
import com.example.jarvis.notification.NotificationPriority
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

@Composable
fun NotificationIntelligenceScreen(
    repository: JarvisRepository,
    onOpenNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    val notifications by repository.notifications.collectAsState()
    val isAccessGranted = remember { JarvisNotificationListenerService.isNotificationAccessEnabled(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf<NotificationCategory?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Filtered notification list
    val filteredList = remember(notifications, searchQuery, selectedFilterCategory) {
        var res = notifications
        if (selectedFilterCategory != null) {
            res = res.filter { it.category == selectedFilterCategory }
        }
        if (searchQuery.isNotBlank()) {
            res = NotificationIntelligenceEngine.search(res, searchQuery)
        }
        res
    }

    val highPriorityCount = remember(notifications) {
        notifications.count { it.priority == NotificationPriority.HIGH }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("notification_intelligence_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Notification Access Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_access_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAccessGranted) JarvisGreen.copy(alpha = 0.5f) else JarvisAmber.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isAccessGranted) JarvisGreen else JarvisAmber)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAccessGranted) "NOTIFICATION ACCESS ACTIVE" else "ACCESS NOT GRANTED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAccessGranted) JarvisGreen else JarvisAmber
                            )
                        }

                        if (!isAccessGranted) {
                            Button(
                                onClick = onOpenNotificationSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisAmber.copy(alpha = 0.2f),
                                    contentColor = JarvisAmber
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("enable_access_button")
                            ) {
                                Text("ENABLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAccessGranted) {
                            "JARVIS locally intercepts and classifies alerts to protect privacy and provide concise briefings without cloud upload."
                        } else {
                            "Android Notification Access is required for JARVIS to read incoming notifications. Tap 'ENABLE' to open system settings."
                        },
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. Metrics & Telemetry Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Count
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisSurfaceElevated)
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("TOTAL BUFFERED", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = JarvisTextDim)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${notifications.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyanBright
                        )
                    }
                }

                // High Priority
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisSurfaceElevated)
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("HIGH PRIORITY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = JarvisTextDim)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$highPriorityCount",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (highPriorityCount > 0) JarvisAmber else JarvisGreen
                        )
                    }
                }

                // Privacy Indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisSurfaceElevated)
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("LOCAL ENCLAVE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = JarvisTextDim)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "100% LOCAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisGreen
                        )
                    }
                }
            }
        }

        // 3. Search & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notification text, app, or tag...", color = JarvisTextDim, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = JarvisTextDim, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedContainerColor = JarvisSurfaceElevated,
                        unfocusedContainerColor = JarvisSurfaceElevated
                    ),
                    singleLine = true
                )

                // Category Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterCategories = listOf(
                        null to "ALL",
                        NotificationCategory.IMPORTANT to "IMPORTANT",
                        NotificationCategory.SECURITY to "SECURITY",
                        NotificationCategory.FINANCE to "FINANCE",
                        NotificationCategory.MESSAGE to "MESSAGES"
                    )

                    for ((cat, label) in filterCategories) {
                        val isSelected = selectedFilterCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceElevated)
                                .border(
                                    0.5.dp,
                                    if (isSelected) JarvisCyan else JarvisBorderSubtle,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedFilterCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
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
                }
            }
        }

        // 4. Action Row: Summary & Purge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERCEPTED STREAM (${filteredList.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextDim
                )

                if (notifications.isNotEmpty()) {
                    Button(
                        onClick = { repository.clearNotifications() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisRed.copy(alpha = 0.15f),
                            contentColor = JarvisRed
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("clear_notifications_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PURGE BUFFER", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 5. Notification Items List
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisSurfaceElevated)
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = JarvisTextDim, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No notifications match '$searchQuery'" else "No intercepted notifications in buffer.",
                            fontSize = 12.sp,
                            color = JarvisTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                NotificationItemCard(
                    notification = item,
                    onDelete = { repository.deleteNotification(item.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: JarvisNotification,
    onDelete: () -> Unit
) {
    val categoryColor = when (notification.category) {
        NotificationCategory.SECURITY -> JarvisRed
        NotificationCategory.FINANCE -> JarvisAmber
        NotificationCategory.IMPORTANT -> JarvisAmber
        NotificationCategory.MESSAGE -> JarvisCyan
        NotificationCategory.REMINDER -> JarvisCyanBright
        NotificationCategory.SYSTEM -> Color(0xFF9E9E9E)
        NotificationCategory.SOCIAL -> Color(0xFFAB47BC)
        NotificationCategory.PROMOTION -> Color(0xFF78909C)
        NotificationCategory.OTHER -> JarvisTextDim
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(JarvisSurfaceElevated)
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Header Row: App Name, Category Tag, Priority & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.appTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyanBright,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = notification.category.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = categoryColor
                        )
                    }

                    if (notification.priority == NotificationPriority.HIGH) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisRed.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HIGH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisRed
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.formattedDate,
                        fontSize = 10.sp,
                        color = JarvisTextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Delete item", tint = JarvisTextDim, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Title
            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JarvisTextPrimary
                )
            }

            // Text / Preview
            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    fontSize = 11.sp,
                    color = JarvisTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
