package com.jarvis.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jarvis.ai.brain.AgentBrain
import com.jarvis.ai.bridge.AndroidBridge
import com.jarvis.ai.model.ChatMessage
import com.jarvis.ai.model.MessageSender
import com.jarvis.ai.model.SafetyRequest
import com.jarvis.ai.provider.JarvisUnifiedAIProvider
import com.jarvis.ai.storage.JarvisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    val repository = JarvisRepository(application)
    val bridge = AndroidBridge(application).apply {
        this.repository = this@JarvisViewModel.repository
    }
    private val aiProvider = JarvisUnifiedAIProvider(repository)

    val brain = AgentBrain(
        repository = repository,
        bridge = bridge,
        aiProvider = aiProvider,
        onConfirmationRequired = { req ->
            _pendingSafetyRequest.value = req
        }
    )

    private val _pendingSafetyRequest = MutableStateFlow<SafetyRequest?>(null)
    val pendingSafetyRequest: StateFlow<SafetyRequest?> = _pendingSafetyRequest.asStateFlow()

    fun dismissSafetyRequest() {
        _pendingSafetyRequest.value = null
    }

    fun submitUserInput(text: String) {
        if (text.isBlank()) return
        repository.addMessage(ChatMessage(sender = MessageSender.USER, text = text))
        brain.processUserInput(
            input = text,
            onThinking = {},
            onSpeaking = {},
            onIdle = {}
        )
    }
}
