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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.automation.AutomationOrchestrator
import com.example.jarvis.automation.Workflow
import com.example.jarvis.automation.WorkflowResult
import com.example.jarvis.automation.WorkflowStatus
import com.example.jarvis.automation.WorkflowStep
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisPurpleHighlight
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutomationScreen(
    orchestrator: AutomationOrchestrator? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val workflowsState by (orchestrator?.workflowsState?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    var isExecutingMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var executionResultsMap by remember { mutableStateOf<Map<String, WorkflowResult>>(emptyMap()) }

    // Fallback default sample workflows if orchestrator is not yet wired
    val displayWorkflows = if (workflowsState.isNotEmpty()) workflowsState else listOf(
        Workflow(
            id = "wf_system_health",
            name = "System Health & Telemetry Audit",
            description = "Reads battery, network, and memory states, then formats an executive report.",
            steps = listOf(
                WorkflowStep("s1", "Collect Device Telemetry", "DeviceInfo", "extract status"),
                WorkflowStep("s2", "Check Security Logs", "ActivityLog", "summarize recent activity"),
                WorkflowStep("s3", "Generate System Report", "FileGeneration", "save as md called report.md", dependencies = listOf("s1", "s2"))
            )
        ),
        Workflow(
            id = "wf_battery_alert",
            name = "Battery Conservation Protocol",
            description = "Triggers low-power alert and notifies user when battery drops below 15%.",
            steps = listOf(
                WorkflowStep("b1", "Evaluate Battery Percentage", "DeviceInfo", "get battery level"),
                WorkflowStep("b2", "Notify User", "Notifications", "send notification Low Battery Warning", dependencies = listOf("b1"))
            )
        )
    )

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
                                Color(0xEE0C152B),
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
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Automation",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "AUTOMATION & WORKFLOW MATRIX",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyanBright,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "DAG Execution Engine • Triggers • Steps • Timeline",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }
        }

        // 2. Workflows Timeline List
        items(displayWorkflows, key = { it.id }) { workflow ->
            val isExecuting = isExecutingMap[workflow.id] ?: false
            val lastResult = executionResultsMap[workflow.id]

            WorkflowTimelineCard(
                workflow = workflow,
                isExecuting = isExecuting,
                lastResult = lastResult,
                onRunWorkflow = {
                    coroutineScope.launch {
                        isExecutingMap = isExecutingMap + (workflow.id to true)
                        if (orchestrator != null) {
                            val res = orchestrator.runWorkflow(workflow.id)
                            executionResultsMap = executionResultsMap + (workflow.id to res)
                        } else {
                            kotlinx.coroutines.delay(1200)
                        }
                        isExecutingMap = isExecutingMap + (workflow.id to false)
                    }
                }
            )
        }
    }
}

@Composable
private fun WorkflowTimelineCard(
    workflow: Workflow,
    isExecuting: Boolean,
    lastResult: WorkflowResult?,
    onRunWorkflow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE0B1222))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workflow.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTextPrimary
                    )
                    Text(
                        text = workflow.description,
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                }

                Button(
                    onClick = onRunWorkflow,
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("run_workflow_${workflow.id}")
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "RUN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Trigger Pill
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisPurpleHighlight.copy(alpha = 0.2f))
                        .border(0.5.dp, JarvisPurpleHighlight.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TRIGGER: ${workflow.trigger::class.simpleName ?: "Manual"}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisPurpleHighlight
                    )
                }
            }

            // Futuristic Step Timeline Node UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                workflow.steps.forEachIndexed { index, step ->
                    val isLast = index == workflow.steps.size - 1

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Timeline Dot & Vertical Connector Line
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(JarvisCyanBright)
                            )
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(JarvisCyan.copy(alpha = 0.3f))
                                )
                            }
                        }

                        // Step Info
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B14))
                                .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${index + 1}. ${step.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisTextPrimary
                                    )
                                    Text(
                                        text = "Tool: ${step.toolName} • Input: ${step.inputTemplate}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = JarvisTextDim
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(JarvisCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "READY",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = JarvisCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Execution Result Banner (if run)
            lastResult?.let { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (res.status == WorkflowStatus.COMPLETED) JarvisGreen.copy(alpha = 0.15f) else JarvisRed.copy(alpha = 0.15f))
                        .border(1.dp, if (res.status == WorkflowStatus.COMPLETED) JarvisGreen else JarvisRed, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "LAST RUN: ${res.status.name} (${res.totalTimeMs} ms) • ${res.message}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (res.status == WorkflowStatus.COMPLETED) JarvisGreen else JarvisRed
                    )
                }
            }
        }
    }
}
