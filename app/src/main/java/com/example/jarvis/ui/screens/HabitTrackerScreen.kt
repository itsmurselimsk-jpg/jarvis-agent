package com.example.jarvis.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.storage.db.HabitEntity
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HabitTrackerScreen(
    repository: JarvisRepository,
    onBack: () -> Unit
) {
    val habits by repository.habitsFlow.collectAsState(initial = emptyList())
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var habitNameInput by remember { mutableStateOf("") }
    var habitTargetInput by remember { mutableStateOf("7") }

    val todayMidnight = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000L))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "HABIT & STREAK MATRIX",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeTheme.accentColor
                    )
                    Text(
                        text = "DAILY REPETITION PROTOCOL",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextSecondary
                    )
                }
            }

            IconButton(
                onClick = { showAddDialog = !showAddDialog },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(activeTheme.primaryColor.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = activeTheme.accentColor
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header stats banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    activeTheme.primaryColor.copy(alpha = 0.25f),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .border(1.dp, activeTheme.borderColor, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE HABIT PROTOCOLS",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextSecondary
                            )
                            Text(
                                text = "${habits.size} Habits Monitored",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = activeTheme.accentColor
                            )
                            val completedToday = habits.count { it.lastCompletedDateMillis >= todayMidnight }
                            Text(
                                text = "$completedToday of ${habits.size} completed today",
                                fontSize = 12.sp,
                                color = if (completedToday == habits.size && habits.isNotEmpty()) JarvisGreen else JarvisAmber
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = JarvisAmber,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            // Quick Add Habit Dialog
            if (showAddDialog) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0D1526))
                            .border(1.dp, activeTheme.borderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "INITIALIZE NEW HABIT PROTOCOL",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = activeTheme.accentColor
                            )

                            OutlinedTextField(
                                value = habitNameInput,
                                onValueChange = { habitNameInput = it },
                                label = { Text("Habit Name (e.g., Pushups, Reading)", color = JarvisTextDim) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeTheme.accentColor,
                                    unfocusedBorderColor = activeTheme.borderColor,
                                    focusedTextColor = JarvisTextPrimary,
                                    unfocusedTextColor = JarvisTextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = habitTargetInput,
                                onValueChange = { habitTargetInput = it },
                                label = { Text("Target Days per Week (1-7)", color = JarvisTextDim) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeTheme.accentColor,
                                    unfocusedBorderColor = activeTheme.borderColor,
                                    focusedTextColor = JarvisTextPrimary,
                                    unfocusedTextColor = JarvisTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    val target = habitTargetInput.toIntOrNull() ?: 7
                                    if (habitNameInput.isNotBlank()) {
                                        scope.launch {
                                            repository.addHabit(
                                                name = habitNameInput.trim(),
                                                targetDays = target
                                            )
                                            habitNameInput = ""
                                            showAddDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeTheme.primaryColor,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = "ACTIVATE PROTOCOL",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Habits List
            if (habits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = JarvisTextDim,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No habit protocols registered.",
                                fontSize = 13.sp,
                                color = JarvisTextSecondary
                            )
                            Text(
                                text = "Say 'Hey Jarvis, track daily coding streak' or tap '+' above",
                                fontSize = 11.sp,
                                color = JarvisTextDim
                            )
                        }
                    }
                }
            } else {
                items(habits) { habit ->
                    val isDoneToday = habit.lastCompletedDateMillis >= todayMidnight

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDoneToday) Color(0x2210B981) else Color(0xFF0B1324))
                            .border(
                                width = 1.dp,
                                color = if (isDoneToday) JarvisGreen.copy(alpha = 0.5f) else activeTheme.borderColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.completeHabitToday(habit)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isDoneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Complete Habit",
                                        tint = if (isDoneToday) JarvisGreen else activeTheme.accentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = habit.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDoneToday) JarvisGreen else JarvisTextPrimary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = JarvisAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${habit.streakDays} day streak (Goal: ${habit.targetDaysPerWeek} days/wk)",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = JarvisAmber
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        repository.deleteHabit(habit.id)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = JarvisRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
