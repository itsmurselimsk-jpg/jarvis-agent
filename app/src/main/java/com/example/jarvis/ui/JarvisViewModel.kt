package com.example.jarvis.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.auth.AuthManager
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.JarvisState
import com.example.jarvis.model.MessageSender
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.model.SafetyRequest
import com.example.jarvis.privacy.PrivacyAuditor
import com.example.jarvis.provider.JarvisUnifiedAIProvider
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.ui.components.NavTab
import com.example.jarvis.vision.JarvisVisionEngine
import com.example.jarvis.vision.LocalVisionProvider
import com.example.jarvis.vision.VisionActionType
import com.example.jarvis.vision.VisionDerivedAction
import com.example.jarvis.vision.VisionEngine
import com.example.jarvis.vision.VisionProvider
import com.example.jarvis.vision.VisionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SubScreen {
    VOICE,
    TOOLS,
    TASKS,
    AUTOMATION,
    FILES,
    MEMORY,
    ACTIVITY,
    VISION,
    PRIVACY,
    BRIDGE,
    VOICE_SETUP,
    VOICE_SELECTION,
    ACCOUNT,
    ABOUT,
    DIAGNOSTICS,
    NOTIFICATIONS,
    SEARCH,
    PLUGINS,
    ARMOR_THEMES,
    EXPENSES,
    HABITS,
    CODE_STUDIO,
    VOICE_NOTES,
    TIMER_STOPWATCH,
    BACKUP_EXPORT,
    PERMISSION_ONBOARDING
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val repository = JarvisRepository(application)
    val bridge = AndroidBridge(application).apply {
        this.repository = repository
    }
    val aiProvider = JarvisUnifiedAIProvider(repository)
    val privacyAuditor = PrivacyAuditor(application, repository)
    val authManager = AuthManager(application)
    val authState = authManager.authState

    val offlineModeManager = com.example.jarvis.network.OfflineModeManager(application)
    val isOnline = offlineModeManager.isOnline
    val networkType = offlineModeManager.networkType

    private val _safetyRequest = MutableStateFlow<SafetyRequest?>(null)
    val safetyRequest: StateFlow<SafetyRequest?> = _safetyRequest.asStateFlow()

    private val _isLiveVoiceSessionActive = MutableStateFlow(false)
    val isLiveVoiceSessionActive: StateFlow<Boolean> = _isLiveVoiceSessionActive.asStateFlow()

    private val _isContinuousConversationActive = MutableStateFlow(false)
    val isContinuousConversationActive: StateFlow<Boolean> = _isContinuousConversationActive.asStateFlow()

    val isMicMuted = bridge.isMicMuted
    val isSpeakerEnabled = bridge.isSpeakerEnabled

    val brain = AgentBrain(
        repository = repository,
        bridge = bridge,
        aiProvider = aiProvider,
        onConfirmationRequired = { request ->
            _safetyRequest.value = request
        }
    )

    val pluginManager = brain.pluginManager
    val plugins = pluginManager.pluginsState

    val recoveryState = brain.recoveryState
    val currentPlanExplanation = brain.currentPlanExplanation
    val lastExtractedData = brain.lastExtractedData

    val toolContext = ToolContext(repository, bridge)

    private val _jarvisState = MutableStateFlow(JarvisState.IDLE)
    val jarvisState: StateFlow<JarvisState> = _jarvisState.asStateFlow()

    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow<SubScreen?>(null)
    val activeSubScreen: StateFlow<SubScreen?> = _activeSubScreen.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    // Expose repository & bridge flows
    val messages = repository.messages
    val memories = repository.memories
    val tasks = repository.tasks
    val timers = repository.timers
    val activityLogs = repository.activityLogs
    val visionScans = repository.visionScans
    val notifications = repository.notifications
    val settings = repository.settings
    val expenses = repository.expensesFlow
    val habits = repository.habitsFlow
    val telemetry = bridge.telemetry
    val isListening = bridge.isListening
    val liveTranscript = bridge.liveTranscript
    val isSpeaking = bridge.isSpeaking
    val speechSupported = bridge.speechSupported
    val deviceTilt = bridge.deviceTilt
    val voiceRmsDb = bridge.voiceRmsDb

    // Vision and OCR state
    private val _activeVisionResult = MutableStateFlow<VisionResult?>(null)
    val activeVisionResult: StateFlow<VisionResult?> = _activeVisionResult.asStateFlow()

    private val _activeVisionUri = MutableStateFlow<Uri?>(null)
    val activeVisionUri: StateFlow<Uri?> = _activeVisionUri.asStateFlow()

    val visionEngine: VisionEngine = JarvisVisionEngine()
    val visionProvider: VisionProvider = LocalVisionProvider(visionEngine)

    // Document Intelligence state
    private val _activeDocument = MutableStateFlow<com.example.jarvis.document.DocumentModel?>(null)
    val activeDocument: StateFlow<com.example.jarvis.document.DocumentModel?> = _activeDocument.asStateFlow()

    private val _activeDocumentSummary = MutableStateFlow<com.example.jarvis.document.DocumentSummary?>(null)
    val activeDocumentSummary: StateFlow<com.example.jarvis.document.DocumentSummary?> = _activeDocumentSummary.asStateFlow()

    private val _activeFileAnalysis = MutableStateFlow<com.example.jarvis.document.FileAnalysisResult?>(null)
    val activeFileAnalysis: StateFlow<com.example.jarvis.document.FileAnalysisResult?> = _activeFileAnalysis.asStateFlow()

    init {
        // Real-time acoustic barge-in: when user speaks while JARVIS is speaking, immediately listen to user
        bridge.onBargeInTriggered = {
            if (_isLiveVoiceSessionActive.value && !bridge.isMicMuted.value) {
                viewModelScope.launch {
                    _jarvisState.value = JarvisState.LISTENING
                    delay(150)
                    if (_isLiveVoiceSessionActive.value && !bridge.isMicMuted.value) {
                        startLiveListeningLoop()
                    }
                }
            }
        }

        // Speech completion callback for standard non-live continuous conversation mode
        brain.onSpeechCompletedCallback = {
            if (!_isLiveVoiceSessionActive.value && _isContinuousConversationActive.value && !bridge.isMicMuted.value) {
                viewModelScope.launch {
                    delay(400)
                    startListening()
                }
            }
        }

        // Periodic telemetry refresh
        viewModelScope.launch {
            while (true) {
                delay(4000)
                bridge.refreshTelemetry()
            }
        }
    }

    fun setTab(tab: NavTab) {
        _activeSubScreen.value = null
        _currentTab.value = tab
    }

    fun openSubScreen(sub: SubScreen) {
        _activeSubScreen.value = sub
    }

    fun closeSubScreen() {
        _activeSubScreen.value = null
    }

    fun setJarvisState(state: JarvisState) {
        _jarvisState.value = state
        when (state) {
            JarvisState.IDLE -> {
                if (isListening.value) stopListening()
                if (isSpeaking.value) stopSpeaking()
            }
            JarvisState.SPEAKING -> {
                if (!isSpeaking.value) {
                    speakText("JARVIS neural matrix online and fully operational, Sir.")
                }
            }
            JarvisState.LISTENING -> {
                startListening()
            }
            else -> {}
        }
    }

    fun dismissSafetyDialog() {
        _safetyRequest.value = null
    }

    fun startLiveVoiceSession() {
        _isLiveVoiceSessionActive.value = true
        _isContinuousConversationActive.value = true
        brain.isLiveVoiceSessionActive = true
        openSubScreen(SubScreen.VOICE)
        _jarvisState.value = JarvisState.LISTENING
        startLiveListeningLoop()
    }

    fun endLiveVoiceSession() {
        _isLiveVoiceSessionActive.value = false
        _isContinuousConversationActive.value = false
        brain.isLiveVoiceSessionActive = false
        stopListening()
        stopSpeaking()
        _jarvisState.value = JarvisState.IDLE
        if (_activeSubScreen.value == SubScreen.VOICE) {
            closeSubScreen()
        }
    }

    fun startLiveListeningLoop() {
        if (!_isLiveVoiceSessionActive.value || bridge.isMicMuted.value) return

        _jarvisState.value = JarvisState.LISTENING
        bridge.startListening(
            onResult = { spokenText ->
                if (!_isLiveVoiceSessionActive.value) return@startListening
                if (spokenText.isNotBlank()) {
                    _jarvisState.value = JarvisState.THINKING
                    sendLiveVoiceUserMessage(spokenText)
                } else {
                    if (_isLiveVoiceSessionActive.value && !bridge.isMicMuted.value) {
                        viewModelScope.launch {
                            delay(200)
                            if (_isLiveVoiceSessionActive.value &&
                                !bridge.isMicMuted.value &&
                                _jarvisState.value != JarvisState.THINKING &&
                                _jarvisState.value != JarvisState.SPEAKING
                            ) {
                                startLiveListeningLoop()
                            }
                        }
                    }
                }
            },
            onError = { _ ->
                // Keep listening continuously: do not turn off mic on silence timeout or no-match
                if (_isLiveVoiceSessionActive.value &&
                    !bridge.isMicMuted.value &&
                    _jarvisState.value != JarvisState.THINKING &&
                    _jarvisState.value != JarvisState.SPEAKING
                ) {
                    viewModelScope.launch {
                        delay(250)
                        if (_isLiveVoiceSessionActive.value &&
                            !bridge.isMicMuted.value &&
                            _jarvisState.value != JarvisState.THINKING &&
                            _jarvisState.value != JarvisState.SPEAKING
                        ) {
                            startLiveListeningLoop()
                        }
                    }
                }
            }
        )
    }

    fun sendLiveVoiceUserMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add user message
        val userMsg = ChatMessage(sender = MessageSender.USER, text = text)
        repository.addMessage(userMsg)
        repository.logActivity("Live Voice Directive", text.take(40), ActivityType.VOICE_EVENT)

        // 2. Dispatch into AgentLoop
        _jarvisState.value = JarvisState.THINKING
        brain.processUserInput(
            input = text,
            onThinking = { _jarvisState.value = JarvisState.THINKING },
            onSpeaking = {
                _jarvisState.value = JarvisState.SPEAKING
                val messages = repository.messages.value
                val lastJarvis = messages.lastOrNull { it.sender == MessageSender.JARVIS }?.text ?: ""
                _lastResponse.value = lastJarvis
            },
            onIdle = {
                // When speech concludes, automatically and immediately return to LISTENING mode
                if (_isLiveVoiceSessionActive.value) {
                    _jarvisState.value = JarvisState.LISTENING
                    if (!bridge.isMicMuted.value) {
                        viewModelScope.launch {
                            delay(200)
                            if (_isLiveVoiceSessionActive.value && !bridge.isMicMuted.value) {
                                startLiveListeningLoop()
                            }
                        }
                    }
                } else {
                    _jarvisState.value = JarvisState.IDLE
                }
            }
        )
    }

    fun startListening() {
        _jarvisState.value = JarvisState.LISTENING
        bridge.startListening { spokenText ->
            _jarvisState.value = JarvisState.IDLE
            if (spokenText.isNotBlank()) {
                sendUserMessage(spokenText)
            }
        }
    }

    fun interruptAndListen() {
        bridge.stopSpeaking()
        _jarvisState.value = JarvisState.LISTENING
        if (_isLiveVoiceSessionActive.value) {
            viewModelScope.launch {
                delay(150)
                if (_isLiveVoiceSessionActive.value && !bridge.isMicMuted.value) {
                    startLiveListeningLoop()
                }
            }
        } else {
            bridge.interruptAndListen { spokenText ->
                _jarvisState.value = JarvisState.IDLE
                if (spokenText.isNotBlank()) {
                    sendUserMessage(spokenText)
                }
            }
        }
    }

    fun toggleContinuousConversation() {
        _isContinuousConversationActive.value = !_isContinuousConversationActive.value
        if (_isContinuousConversationActive.value) {
            startLiveVoiceSession()
        } else {
            endLiveVoiceSession()
        }
    }

    fun toggleMicMute() {
        bridge.toggleMicMute()
        if (!bridge.isMicMuted.value && _isLiveVoiceSessionActive.value &&
            _jarvisState.value != JarvisState.SPEAKING &&
            _jarvisState.value != JarvisState.THINKING
        ) {
            startLiveListeningLoop()
        }
    }

    fun toggleSpeaker() {
        bridge.toggleSpeaker()
    }

    fun stopListening() {
        bridge.stopListening()
        _jarvisState.value = JarvisState.IDLE
    }

    fun speakText(
        text: String,
        rate: Float? = null,
        pitch: Float? = null,
        locale: java.util.Locale? = null,
        onDone: () -> Unit = {}
    ) {
        _jarvisState.value = JarvisState.SPEAKING
        val r = rate ?: settings.value.speechRate
        val p = pitch ?: settings.value.speechPitch
        bridge.speak(text, r, p, locale) {
            _jarvisState.value = JarvisState.IDLE
            onDone()
        }
    }

    fun triggerWakeSession() {
        val intent = android.content.Intent(getApplication(), com.example.jarvis.service.JarvisVoiceService::class.java).apply {
            action = com.example.jarvis.service.JarvisVoiceService.ACTION_TRIGGER_WAKE
        }
        getApplication<Application>().startService(intent)
    }

    fun stopSpeaking() {
        bridge.stopSpeaking()
        _jarvisState.value = JarvisState.IDLE
    }

    fun toggleFlashlight(on: Boolean) {
        bridge.toggleFlashlight(on)
    }

    fun openAndroidSettings(targetScreen: String = "general") {
        bridge.openAndroidSettings(targetScreen)
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add user message
        val userMsg = ChatMessage(sender = MessageSender.USER, text = text)
        repository.addMessage(userMsg)
        repository.logActivity("User Directive", text.take(40), ActivityType.VOICE_EVENT)

        // 2. Dispatch into AgentLoop
        brain.processUserInput(
            input = text,
            onThinking = { _jarvisState.value = JarvisState.THINKING },
            onSpeaking = { _jarvisState.value = JarvisState.SPEAKING },
            onIdle = { _jarvisState.value = JarvisState.IDLE }
        )
    }

    fun retryLastMessage() {
        val userMsgs = messages.value.filter { it.sender == MessageSender.USER }
        val lastUser = userMsgs.lastOrNull()
        if (lastUser != null) {
            sendUserMessage(lastUser.text)
        }
    }

    fun copyToClipboard(text: String) {
        bridge.copyToClipboard("JARVIS_COPY", text)
        repository.logActivity("Clipboard Copy", text.take(30), ActivityType.TOOL_EXECUTION)
    }

    fun setActiveVisionContext(result: VisionResult, uri: Uri?) {
        _activeVisionResult.value = result
        _activeVisionUri.value = uri
        brain.activeVisionResult = result
        repository.logActivity(
            "Vision Analyzed",
            "Extracted ${result.extractedText.length} chars (Sensitive: ${result.containsSensitiveData})",
            ActivityType.TOOL_EXECUTION
        )
    }

    fun clearVisionContext() {
        _activeVisionResult.value = null
        _activeVisionUri.value = null
        brain.activeVisionResult = null
    }

    fun loadDocument(uri: Uri) {
        viewModelScope.launch {
            val doc = com.example.jarvis.document.UniversalDocumentReader.readUri(
                context = getApplication(),
                uri = uri
            )
            setLoadedDocument(doc)
        }
    }

    fun setLoadedDocument(doc: com.example.jarvis.document.DocumentModel) {
        _activeDocument.value = doc
        val summary = com.example.jarvis.document.DocumentIntelligenceEngine.analyze(doc)
        val analysis = com.example.jarvis.document.AdvancedFileAnalyzer.analyze(doc)
        _activeDocumentSummary.value = summary
        _activeFileAnalysis.value = analysis
        brain.attachDocument(doc)
        repository.logActivity(
            "Document Loaded",
            "${doc.fileName} (${doc.documentType}, ${doc.sizeBytes} bytes)",
            ActivityType.TOOL_EXECUTION
        )
    }

    fun clearActiveDocument() {
        _activeDocument.value = null
        _activeDocumentSummary.value = null
        _activeFileAnalysis.value = null
        brain.clearActiveDocument()
    }

    fun askJarvisAboutDocument(prompt: String) {
        _activeSubScreen.value = null
        _currentTab.value = NavTab.CONVERSATION
        sendUserMessage(prompt)
    }

    fun askJarvisAboutVision(prompt: String) {
        _activeSubScreen.value = null
        _currentTab.value = NavTab.CONVERSATION
        sendUserMessage(prompt)
    }

    fun executeVisionDerivedAction(action: VisionDerivedAction) {
        when (action.type) {
            VisionActionType.DIAL_PHONE -> {
                val assessment = com.example.jarvis.safety.RiskEngine.assessAction("PhoneCall", action.payload, RiskLevel.CONFIRMATION)
                val safetyReq = com.example.jarvis.safety.RiskEngine.buildSafetyRequest(
                    toolName = "PhoneCall",
                    actionPayload = action.payload,
                    reason = assessment.reason,
                    riskLevel = RiskLevel.CONFIRMATION,
                    onConfirm = {
                        bridge.makePhoneCall(action.payload)
                        repository.logActivity("Voice Call Dispatched", action.payload, ActivityType.TOOL_EXECUTION, RiskLevel.CONFIRMATION)
                    },
                    onCancel = {
                        dismissSafetyDialog()
                    }
                )
                _safetyRequest.value = safetyReq
            }
            VisionActionType.SEARCH_PHONE -> {
                openSubScreen(SubScreen.SEARCH)
            }
            VisionActionType.OPEN_URL -> {
                val assessment = com.example.jarvis.safety.RiskEngine.assessAction("OpenUrl", action.payload, RiskLevel.CONFIRMATION)
                val safetyReq = com.example.jarvis.safety.RiskEngine.buildSafetyRequest(
                    toolName = "OpenUrl",
                    actionPayload = action.payload,
                    reason = assessment.reason,
                    riskLevel = RiskLevel.CONFIRMATION,
                    onConfirm = {
                        bridge.openUrl(action.payload)
                        repository.logActivity("URL Launched", action.payload, ActivityType.TOOL_EXECUTION, RiskLevel.CONFIRMATION)
                    },
                    onCancel = {
                        dismissSafetyDialog()
                    }
                )
                _safetyRequest.value = safetyReq
            }
            VisionActionType.SEND_EMAIL -> {
                bridge.openUrl("mailto:${action.payload}")
            }
            VisionActionType.COPY_TEXT -> {
                copyToClipboard(action.payload)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bridge.destroy()
    }
}
