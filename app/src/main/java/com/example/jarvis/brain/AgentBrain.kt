package com.example.jarvis.brain

import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.extraction.ExtractedData
import com.example.jarvis.extraction.InformationExtractionEngine
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.MessageSender
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.model.SafetyRequest
import com.example.jarvis.provider.AIProvider
import com.example.jarvis.provider.LocalNeuralBrainProvider
import com.example.jarvis.recovery.ErrorCategory
import com.example.jarvis.recovery.ErrorClassifier
import com.example.jarvis.recovery.RecoveryState
import com.example.jarvis.recovery.RecoveryStatus
import com.example.jarvis.recovery.RetryPolicy
import com.example.jarvis.recovery.ToolRecovery
import com.example.jarvis.recovery.executeWithRetry
import com.example.jarvis.safety.RiskAssessment
import com.example.jarvis.safety.RiskEngine
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name.lowercase()] = tool
    }

    fun getTool(name: String): Tool? = tools[name.lowercase()]

    fun getAllTools(): List<Tool> = tools.values.toList()

    fun getToolDefinitions(): List<Pair<String, String>> =
        tools.values.map { Pair(it.name, it.description) }
}

class AgentBrain(
    val repository: JarvisRepository,
    val bridge: AndroidBridge,
    private val aiProvider: AIProvider,
    private val onConfirmationRequired: (SafetyRequest) -> Unit
) {
    val registry = ToolRegistry()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentPlanExplanation = MutableStateFlow<String?>(null)
    val currentPlanExplanation: StateFlow<String?> = _currentPlanExplanation.asStateFlow()

    private val _recoveryState = MutableStateFlow(RecoveryState())
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

    private val _lastExtractedData = MutableStateFlow<ExtractedData?>(null)
    val lastExtractedData: StateFlow<ExtractedData?> = _lastExtractedData.asStateFlow()

    // Short-term conversation context buffer: stores last N turns without sending entire history to provider
    private val conversationBuffer = mutableListOf<Pair<String, String>>()
    private val maxBufferTurns = 4

    // Recent tool execution tracking
    private var lastExecutedToolName: String? = null
    private var lastExecutedToolResult: String? = null

    var onSpeechCompletedCallback: (() -> Unit)? = null
    var isLiveVoiceSessionActive: Boolean = false

    val pluginManager = com.example.jarvis.plugin.PluginManager(bridge.getApplicationContext(), repository)

    val automationOrchestrator: com.example.jarvis.automation.AutomationOrchestrator by lazy {
        com.example.jarvis.automation.AutomationOrchestrator(repository, bridge, this)
    }

    init {
        // Register all real Android tools
        registry.register(BatteryTool())
        registry.register(NetworkStatusTool())
        registry.register(WifiTool())
        registry.register(BluetoothTool())
        registry.register(VolumeTool())
        registry.register(BrightnessTool())
        registry.register(FlashlightTool())
        registry.register(MediaControlTool())
        registry.register(AppLauncherTool())
        registry.register(OpenUrlTool())
        registry.register(AndroidSettingsTool())
        registry.register(ClipboardTool())
        registry.register(DeviceInfoTool())
        registry.register(MemoryTool())
        registry.register(TasksTool())
        registry.register(TimerTool())
        registry.register(CalculatorTool())
        registry.register(DateTimeTool())
        registry.register(AccessibilityTool())
        registry.register(NotificationTool())
        registry.register(WebSearchTool())
        registry.register(WeatherTool())
        registry.register(YouTubeSearchTool())
        registry.register(PhoneCallTool())
        registry.register(DiagnosticsTool())
        registry.register(UniversalSearchTool())
        registry.register(VisionOcrTool())
        registry.register(DocumentIntelligenceTool())
        registry.register(FileGenerationTool())
        registry.register(CodeAnalysisTool())
        registry.register(TranslationTool())
        registry.register(WebResearchTool())
        registry.register(DeepResearchTool())
        registry.register(OrbCoreTool())
        registry.register(TacticalTool())
        registry.register(StarkProtocolTool())
        registry.register(WhatsAppMessagingTool())
        registry.register(SmsMessagingTool())
        registry.register(CalendarTool())
        registry.register(CodeStudioTool())
        registry.register(MeetingTranscriberTool())
        registry.register(SmartExpenseBudgetTool())
        registry.register(HabitStreakTrackerTool())
        registry.register(IoTRemoteBridgeTool())
        registry.register(TacticalSosBeaconTool())
        registry.register(PersonalityStudioTool())
        registry.register(DeepWebResearchDossierTool())
        registry.register(BiometricVaultAuditTool())
        registry.register(OmniCognitiveSuperBrainTool())
        registry.register(com.example.jarvis.automation.AutomationTool(automationOrchestrator))
        pluginManager.syncToolsWithBrain(this)
    }

    fun syncPluginTools() {
        pluginManager.syncToolsWithBrain(this)
    }

    private val fileGenerationPipeline by lazy {
        com.example.jarvis.generation.FileGenerationPipeline(bridge.getApplicationContext())
    }

    // Active vision / image OCR context if user recently scanned or inspected an image
    var activeVisionResult: com.example.jarvis.vision.VisionResult? = null

    // Active document context if user recently loaded a file
    var activeDocument: com.example.jarvis.document.DocumentModel? = null
    var activeDocumentSummary: com.example.jarvis.document.DocumentSummary? = null
    var activeFileAnalysis: com.example.jarvis.document.FileAnalysisResult? = null
    var previousDocument: com.example.jarvis.document.DocumentModel? = null
    var previousFileAnalysis: com.example.jarvis.document.FileAnalysisResult? = null

    fun attachDocument(document: com.example.jarvis.document.DocumentModel) {
        if (activeDocument != null && activeDocument !== document) {
            previousDocument = activeDocument
            previousFileAnalysis = activeFileAnalysis
        }
        activeDocument = document
        activeDocumentSummary = com.example.jarvis.document.DocumentIntelligenceEngine.analyze(document)
        activeFileAnalysis = com.example.jarvis.document.AdvancedFileAnalyzer.analyze(document)
    }

    fun clearActiveDocument() {
        previousDocument = activeDocument
        previousFileAnalysis = activeFileAnalysis
        activeDocument = null
        activeDocumentSummary = null
        activeFileAnalysis = null
    }

    /**
     * Canonical AgentLoop:
     * User Input
     * → Context
     * → Memory Retrieval
     * → AI Planning
     * → Tool Selection
     * → Safety Check
     * → Confirmation if required
     * → Tool Execution
     * → Result Verification
     * → Final Response
     * → TTS
     */
    fun processUserInput(
        input: String,
        onThinking: () -> Unit,
        onSpeaking: () -> Unit,
        onIdle: () -> Unit
    ) {
        scope.launch {
            onThinking()

            // 1. Context Assembly (Compact & Bounded)
            bridge.refreshTelemetry()
            val telemetry = bridge.telemetry.value
            val contextBuilder = StringBuilder()
            contextBuilder.appendLine("DEVICE TELEMETRY: Battery ${telemetry.batteryPercent}%, Net: ${telemetry.networkType}, Audio: ${telemetry.volumePercent}%")

            // Active tasks summary (max 3 pending)
            val pendingTasks = repository.tasks.value.filter { !it.isCompleted }.take(3)
            if (pendingTasks.isNotEmpty()) {
                val taskList = pendingTasks.joinToString(", ") { it.title }
                contextBuilder.appendLine("ACTIVE PENDING TASKS: $taskList")
            }

            // Recent tool result if available
            if (lastExecutedToolName != null && lastExecutedToolResult != null) {
                val briefResult = lastExecutedToolResult!!.take(120).replace("\n", " ")
                contextBuilder.appendLine("RECENT TOOL EXECUTED: $lastExecutedToolName -> $briefResult")
            }

            // Short-term conversation context (last turns)
            if (conversationBuffer.isNotEmpty()) {
                contextBuilder.appendLine("RECENT DIALOGUE CONTEXT:")
                conversationBuffer.takeLast(3).forEach { (user, jarvis) ->
                    contextBuilder.appendLine("User: $user")
                    contextBuilder.appendLine("JARVIS: ${jarvis.take(100)}")
                }
            }

            // Structured Information Extraction from User Input
            val extractedInfo = InformationExtractionEngine.extractFromUserMessage(input)
            _lastExtractedData.value = extractedInfo
            if (!extractedInfo.isEmpty()) {
                val entitySummaries = mutableListOf<String>()
                if (extractedInfo.phoneNumbers.isNotEmpty()) entitySummaries.add("Phones: " + extractedInfo.phoneNumbers.joinToString { it.phoneNumber })
                if (extractedInfo.emails.isNotEmpty()) entitySummaries.add("Emails: " + extractedInfo.emails.joinToString { it.email })
                if (extractedInfo.urls.isNotEmpty()) entitySummaries.add("URLs: " + extractedInfo.urls.joinToString { it.url })
                if (extractedInfo.dates.isNotEmpty()) entitySummaries.add("Dates: " + extractedInfo.dates.joinToString { it.normalizedIso ?: it.rawText })
                if (extractedInfo.times.isNotEmpty()) entitySummaries.add("Times: " + extractedInfo.times.joinToString { it.normalizedTime ?: it.rawText })
                if (extractedInfo.currencies.isNotEmpty()) entitySummaries.add("Amounts: " + extractedInfo.currencies.joinToString { "${it.currencySymbol}${it.amount}" })
                if (extractedInfo.identifiers.isNotEmpty()) entitySummaries.add("IDs: " + extractedInfo.identifiers.joinToString { it.id })
                if (entitySummaries.isNotEmpty()) {
                    contextBuilder.appendLine("STRUCTURED INPUT ENTITIES: ${entitySummaries.joinToString(" | ")}")
                }
            }

            // Active Vision / Image OCR Context if available
            val vision = activeVisionResult
            if (vision != null && vision.success && vision.extractedText.isNotBlank()) {
                val sanitizedText = if (vision.containsSensitiveData) {
                    com.example.jarvis.vision.SensitiveDataFilter.redactSensitiveData(vision.extractedText)
                } else {
                    vision.extractedText
                }
                contextBuilder.appendLine("ACTIVE OCR VISION CONTEXT:")
                contextBuilder.appendLine("Extracted Text: \"${sanitizedText.take(400)}\"")

                // Structured extraction from OCR text
                val ocrExtracted = InformationExtractionEngine.extractFromOcr(vision)
                val ocrEntities = mutableListOf<String>()
                if (ocrExtracted.phoneNumbers.isNotEmpty()) ocrEntities.add("Phones: " + ocrExtracted.phoneNumbers.joinToString { it.phoneNumber })
                if (ocrExtracted.emails.isNotEmpty()) ocrEntities.add("Emails: " + ocrExtracted.emails.joinToString { it.email })
                if (ocrExtracted.urls.isNotEmpty()) ocrEntities.add("URLs: " + ocrExtracted.urls.joinToString { it.url })
                if (ocrExtracted.currencies.isNotEmpty()) ocrEntities.add("Amounts: " + ocrExtracted.currencies.joinToString { "${it.currencySymbol}${it.amount}" })
                if (ocrExtracted.identifiers.isNotEmpty()) ocrEntities.add("IDs: " + ocrExtracted.identifiers.joinToString { it.id })
                if (ocrEntities.isNotEmpty()) {
                    contextBuilder.appendLine("OCR DETECTED ENTITIES: ${ocrEntities.joinToString(" | ")}")
                }

                if (vision.containsSensitiveData) {
                    contextBuilder.appendLine("SENSITIVITY ALERT: Contains sensitive tokens (${vision.sensitiveEntitiesDetected.joinToString()}). Must NOT be stored in persistent long-term memory.")
                }
            }

            // Active Document Context if available
            val doc = activeDocument
            if (doc != null && doc.isSuccessful()) {
                val summary = activeDocumentSummary ?: com.example.jarvis.document.DocumentIntelligenceEngine.analyze(doc)
                val analysis = activeFileAnalysis ?: com.example.jarvis.document.AdvancedFileAnalyzer.analyze(doc)
                contextBuilder.appendLine(com.example.jarvis.document.DocumentIntelligenceEngine.formatContextForBrain(doc, summary, analysis))
            }

            val contextString = contextBuilder.toString().trimEnd()

            // Extract facts into long-term knowledge graph
            com.example.jarvis.memory.KnowledgeGraphEngine.extractFactsFromConversation(input)

            // 1b. Intent Classification & Language Style
            val intentResult = com.example.jarvis.intent.IntentClassifier.classify(input)
            val langStyle = com.example.jarvis.personality.JarvisPersonality.detectLanguageStyle(input)

            // Check and execute compound multi-step task if detected
            if (com.example.jarvis.brain.CompoundTaskPlanner.isCompoundQuery(input)) {
                val subSteps = com.example.jarvis.brain.CompoundTaskPlanner.decomposeIntoSubSteps(input)
                if (subSteps.size > 1) {
                    _currentPlanExplanation.value = "Executing multi-step compound query (${subSteps.size} steps)..."
                    val executionResults = com.example.jarvis.brain.CompoundTaskPlanner.executeSubSteps(
                        subSteps = subSteps,
                        registry = registry,
                        context = ToolContext(
                            repository = repository,
                            bridge = bridge,
                            activeVisionResult = activeVisionResult,
                            activeDocument = activeDocument,
                            activeDocumentSummary = activeDocumentSummary,
                            activeFileAnalysis = activeFileAnalysis,
                            previousDocument = previousDocument,
                            previousFileAnalysis = previousFileAnalysis,
                            fileGenerationPipeline = fileGenerationPipeline,
                            aiProvider = aiProvider
                        ),
                        lang = langStyle
                    )
                    val report = com.example.jarvis.brain.CompoundTaskPlanner.formatCompoundExecutionReport(executionResults, langStyle)
                    recordTurn(input, report)
                    repository.addMessage(ChatMessage(sender = MessageSender.JARVIS, text = report))
                    deliverFinalResponse(report, onSpeaking, onIdle)
                    return@launch
                }
            }

            if (intentResult.intent == com.example.jarvis.intent.ConversationIntent.CLARIFICATION) {
                val clarQuestion = intentResult.clarificationQuestion ?: "Kya aap thoda clear bol sakte hain?"
                repository.addMessage(ChatMessage(sender = MessageSender.JARVIS, text = clarQuestion))
                deliverFinalResponse(clarQuestion, onSpeaking, onIdle)
                return@launch
            }

            // 2. Memory Retrieval with Recall Gating & Knowledge Graph
            val relevantMemories = if (com.example.jarvis.memory.RecallGatingEngine.shouldRecall(input, conversationBuffer)) {
                val dbMem = retrieveRelevantMemories(input)
                val graphDigest = com.example.jarvis.memory.KnowledgeGraphEngine.getRelevantKnowledgeDigest(input)
                listOfNotNull(dbMem.takeIf { it.isNotBlank() }, graphDigest).joinToString("\n\n")
            } else {
                ""
            }

            // 3. AI Planning & Tool Selection
            _currentPlanExplanation.value = "Analyzing intent & selecting tools..."
            val decision = if (!intentResult.requiresTool) {
                com.example.jarvis.provider.ToolDecision(false, null, input, "Conversational intent: ${intentResult.intent}")
            } else {
                aiProvider.decideTool(
                    userInput = input,
                    availableTools = registry.getToolDefinitions(),
                    contextHistory = "$contextString\n$relevantMemories"
                )
            }

            val selectedTool = if (decision.useTool && decision.toolName != null) {
                registry.getTool(decision.toolName)
            } else null

            // Refine toolInput with extracted structured parameters if tool input is missing or general
            val toolInput = when {
                selectedTool?.name.equals("PhoneCall", ignoreCase = true) &&
                        extractedInfo.phoneNumbers.isNotEmpty() &&
                        (decision.toolInput == null || !decision.toolInput.any { it.isDigit() }) -> {
                    extractedInfo.phoneNumbers.first().phoneNumber
                }
                selectedTool?.name.equals("OpenUrl", ignoreCase = true) &&
                        extractedInfo.urls.isNotEmpty() &&
                        (decision.toolInput == null || !decision.toolInput.startsWith("http")) -> {
                    extractedInfo.urls.first().url
                }
                else -> decision.toolInput ?: input
            }

            if (selectedTool != null) {
                _currentPlanExplanation.value = "Selected Tool: ${selectedTool.name} (Risk: ${selectedTool.riskLevel})"

                // 4. Safety Check
                val assessment = RiskEngine.assessAction(
                    actionName = selectedTool.name,
                    actionPayload = toolInput,
                    baseRisk = selectedTool.riskLevel
                )

                when (assessment.level) {
                    RiskLevel.RESTRICTED -> {
                        // Strictly rejected
                        _currentPlanExplanation.value = "Security Alert: Action Restricted"
                        val rejectionMsg = "Security protocol override: ${assessment.reason}"
                        repository.addMessage(ChatMessage(sender = MessageSender.SYSTEM, text = rejectionMsg, toolRiskLevel = RiskLevel.RESTRICTED))
                        repository.logActivity("Safety Restricted", assessment.actionSummary, ActivityType.SAFETY_ALERT, RiskLevel.RESTRICTED)
                        deliverFinalResponse(rejectionMsg, onSpeaking, onIdle)
                        return@launch
                    }

                    RiskLevel.CONFIRMATION -> {
                        // Prompt user confirmation in UI before executing
                        _currentPlanExplanation.value = "Awaiting user authorization for ${selectedTool.name}..."
                        val safetyReq = RiskEngine.buildSafetyRequest(
                            toolName = selectedTool.name,
                            actionPayload = toolInput,
                            reason = assessment.reason,
                            riskLevel = RiskLevel.CONFIRMATION,
                            onConfirm = {
                                scope.launch {
                                    executeAndDeliverTool(selectedTool, toolInput, input, onSpeaking, onIdle)
                                }
                            },
                            onCancel = {
                                scope.launch {
                                    val cancelMsg = "Operation cancelled by user clearance override."
                                    repository.addMessage(ChatMessage(sender = MessageSender.JARVIS, text = cancelMsg))
                                    _currentPlanExplanation.value = null
                                    onIdle()
                                }
                            }
                        )
                        onConfirmationRequired(safetyReq)
                        return@launch
                    }

                    RiskLevel.SAFE -> {
                        // Execute immediately
                        executeAndDeliverTool(selectedTool, toolInput, input, onSpeaking, onIdle)
                    }
                }
            } else {
                // Conversational reasoning via AIProvider
                _currentPlanExplanation.value = "Synthesizing response with AI brain..."
                val streamingMessage = ChatMessage(sender = MessageSender.JARVIS, text = "", isStreaming = true)
                repository.addMessage(streamingMessage)

                val prompt = com.example.jarvis.security.PrivacyRedactionGuard.redact(buildString {
                    if (relevantMemories.isNotBlank()) {
                        appendLine(relevantMemories)
                    }
                    appendLine(contextString)
                    appendLine("User: $input")
                })

                val dynamicSystemInstruction = if (repository.settings.value.systemPrompt.isNotBlank() &&
                    !repository.settings.value.systemPrompt.startsWith("You are JARVIS, an advanced")
                ) {
                    repository.settings.value.systemPrompt
                } else {
                    com.example.jarvis.personality.JarvisPersonality.getSystemPrompt(
                        langStyle,
                        com.example.jarvis.memory.KnowledgeGraphEngine.getRelevantKnowledgeDigest(input)
                    )
                }

                val finalResponse = try {
                    val retryPolicy = RetryPolicy(maxAttempts = 3, initialBackoffMs = 250L, maxBackoffMs = 2000L)
                    val retryResult = executeWithRetry(
                        policy = retryPolicy,
                        operationName = "AIProvider:generateResponse",
                        source = "AI_PROVIDER",
                        onRetry = { attempt, error, _ ->
                            _recoveryState.value = RecoveryState(
                                status = RecoveryStatus.RETRYING,
                                lastError = error,
                                attempt = attempt,
                                maxAttempts = 3,
                                message = "Re-establishing link (attempt $attempt/3)..."
                            )
                            _currentPlanExplanation.value = "Re-establishing link (attempt $attempt/3)..."
                        }
                    ) {
                        aiProvider.generateResponse(
                            prompt = prompt,
                            systemInstruction = dynamicSystemInstruction,
                            onChunkReceived = { chunk ->
                                repository.updateStreamingMessage(chunk)
                            }
                        )
                    }

                    retryResult.getOrElse { throwable ->
                        val error = ErrorClassifier.classify(throwable, source = "AI_PROVIDER")
                        val status = if (error.category == ErrorCategory.RATE_LIMIT) {
                            RecoveryStatus.TEMPORARILY_UNAVAILABLE
                        } else {
                            RecoveryStatus.FAILED
                        }
                        _recoveryState.value = RecoveryState(
                            status = status,
                            lastError = error,
                            message = error.userSafeMessage
                        )
                        repository.logActivity(
                            title = "AI Recovery Engaged",
                            detail = "${error.category}: ${error.userSafeMessage}",
                            type = ActivityType.SYSTEM_EVENT
                        )
                        val localFallback = LocalNeuralBrainProvider.generateLocalResponse(prompt) { chunk ->
                            repository.updateStreamingMessage(chunk)
                        }
                        "$localFallback\n\n*(Note: Cloud link temporarily unavailable [${error.category}]. Operating via onboard neural engine.)*"
                    }
                } catch (t: Throwable) {
                    val safeError = ErrorClassifier.classify(t, source = "AgentBrain")
                    safeError.userSafeMessage
                }

                _recoveryState.value = RecoveryState(status = RecoveryStatus.IDLE)
                repository.finalizeStreamingMessage(finalResponse)
                recordTurn(input, finalResponse)
                deliverFinalResponse(finalResponse, onSpeaking, onIdle)
            }
        }
    }

    private suspend fun executeAndDeliverTool(
        tool: Tool,
        toolInput: String,
        originalUserInput: String,
        onSpeaking: () -> Unit,
        onIdle: () -> Unit
    ) {
        val toolContext = ToolContext(
            repository = repository,
            bridge = bridge,
            activeVisionResult = activeVisionResult,
            activeDocument = activeDocument,
            activeDocumentSummary = activeDocumentSummary,
            activeFileAnalysis = activeFileAnalysis,
            previousDocument = previousDocument,
            previousFileAnalysis = previousFileAnalysis,
            fileGenerationPipeline = fileGenerationPipeline,
            aiProvider = aiProvider
        )

        // 5. Safe Tool Execution with idempotency & error recovery
        val result = ToolRecovery.executeSafely(
            tool = tool,
            input = toolInput,
            context = toolContext,
            onRetryAttempt = { attempt, error ->
                _recoveryState.value = RecoveryState(
                    status = RecoveryStatus.RETRYING,
                    lastError = error,
                    attempt = attempt,
                    maxAttempts = 2,
                    message = "Retrying ${tool.name} (attempt $attempt)..."
                )
                _currentPlanExplanation.value = "Retrying ${tool.name} (attempt $attempt)..."
            }
        )

        // 6. Result Verification
        val isVerified = if (result.success) {
            tool.verify(result, toolContext)
        } else {
            false
        }

        val langStyle = com.example.jarvis.personality.JarvisPersonality.detectLanguageStyle(originalUserInput)

        val verifiedOutput = if (isVerified) {
            com.example.jarvis.personality.JarvisPersonality.formatToolSuccessResponse(tool.name, result.output, langStyle)
        } else if (result.success) {
            "${result.output}\n\n*(Post-action verification alert: Device hardware state did not reflect expected change.)*"
        } else {
            com.example.jarvis.personality.JarvisPersonality.formatToolFailureResponse(tool.name, result.output, langStyle)
        }

        _recoveryState.value = RecoveryState(status = RecoveryStatus.IDLE)

        // Record in chat & update context
        lastExecutedToolName = tool.name
        lastExecutedToolResult = verifiedOutput
        recordTurn(originalUserInput, verifiedOutput)

        repository.addMessage(
            ChatMessage(
                sender = MessageSender.JARVIS,
                text = verifiedOutput,
                toolCallName = tool.name,
                toolRiskLevel = tool.riskLevel
            )
        )

        deliverFinalResponse(verifiedOutput, onSpeaking, onIdle)
    }

    private fun recordTurn(userInput: String, assistantOutput: String) {
        conversationBuffer.add(Pair(userInput, assistantOutput))
        while (conversationBuffer.size > maxBufferTurns) {
            conversationBuffer.removeAt(0)
        }
    }

    private fun deliverFinalResponse(
        text: String,
        onSpeaking: () -> Unit,
        onIdle: () -> Unit
    ) {
        _currentPlanExplanation.value = null
        val settings = repository.settings.value
        val shouldSpeak = isLiveVoiceSessionActive || settings.autoSpeakResponses
        if (shouldSpeak) {
            onSpeaking()
            bridge.speak(
                text = text,
                speechRate = settings.speechRate,
                pitch = settings.speechPitch,
                onDone = {
                    onIdle()
                    onSpeechCompletedCallback?.invoke()
                }
            )
        } else {
            onIdle()
            onSpeechCompletedCallback?.invoke()
        }
    }

    private fun retrieveRelevantMemories(query: String): String {
        val memories = repository.memories.value
        if (memories.isEmpty()) return ""

        val cleaned = query.lowercase().trim()
        val stopWords = setOf("the", "a", "an", "is", "are", "was", "were", "what", "where", "who", "how", "when", "why", "my", "your", "me", "you", "i", "to", "in", "on", "for", "with", "about", "tell", "jarvis", "please", "can")
        val queryTokens = cleaned.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 && !stopWords.contains(it) }

        // Scored retrieval
        val scored = memories.map { mem ->
            var score = 0
            val titleLower = mem.title.lowercase()
            val contentLower = mem.content.lowercase()

            if (titleLower.contains(cleaned) || contentLower.contains(cleaned)) {
                score += 10
            }

            for (token in queryTokens) {
                if (titleLower.contains(token)) score += 3
                if (contentLower.contains(token)) score += 2
            }

            Pair(mem, score)
        }.filter { it.second > 0 }
         .sortedByDescending { it.second }
         .take(4)

        return if (scored.isNotEmpty()) {
            "RELEVANT LONG-TERM MEMORIES:\n" + scored.joinToString("\n") { "• [${it.first.title}] ${it.first.content}" }
        } else ""
    }
}
