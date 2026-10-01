package com.jarvis.ai.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.ai.model.JarvisState
import com.jarvis.ai.model.DeviceTelemetry
import com.jarvis.ai.ui.screens.*
import com.jarvis.ai.ui.components.SafetyConfirmationDialog

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE) }
    var onboardingCompleted by remember {
        mutableStateOf(prefs.getBoolean("permission_onboarding_completed", false))
    }

    if (!onboardingCompleted) {
        PermissionOnboardingScreen(
            onComplete = {
                prefs.edit().putBoolean("permission_onboarding_completed", true).apply()
                onboardingCompleted = true
            }
        )
        return
    }

    var showChat by remember { mutableStateOf(false) }
    val safetyReq by viewModel.pendingSafetyRequest.collectAsState()

    val messages by viewModel.repository.messages.collectAsState()
    val tasks by viewModel.repository.tasks.collectAsState()
    val memories by viewModel.repository.memories.collectAsState()
    val logs by viewModel.repository.activityLogs.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()
    val expenses by viewModel.repository.expensesFlow.collectAsState(initial = emptyList())
    val habits by viewModel.repository.habitsFlow.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (showChat) {
                ChatScreen(
                    messages = messages,
                    jarvisState = JarvisState.IDLE,
                    onSendMessage = { text -> viewModel.submitUserInput(text) },
                    onCopyMessage = {},
                    onSpeakMessage = {},
                    onRetryMessage = {},
                    onClearChat = {},
                    onBack = { showChat = false }
                )
            } else {
                HomeScreen(
                    jarvisState = JarvisState.IDLE,
                    telemetry = DeviceTelemetry(),
                    isListening = false,
                    isSpeaking = false,
                    liveTranscript = "",
                    lastResponse = "Systems online. How can I assist you?",
                    messages = messages,
                    tasks = tasks,
                    memories = memories,
                    logs = logs,
                    settings = settings,
                    expenses = expenses,
                    habits = habits,
                    rmsDb = 0f,
                    onVoiceClick = {},
                    onChatClick = { showChat = true },
                    onToolsClick = {},
                    onMemoryClick = {},
                    onActivityClick = {},
                    onVisionClick = {},
                    onPrivacyClick = {},
                    onBridgeClick = {}
                )
            }

            if (safetyReq != null) {
                SafetyConfirmationDialog(
                    request = safetyReq!!,
                    onDismiss = { viewModel.dismissSafetyRequest() }
                )
            }
        }
    }
}
