package com.example.jarvis.model

import java.util.UUID

enum class JarvisState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class RiskLevel {
    SAFE,
    CONFIRMATION,
    RESTRICTED
}

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallName: String? = null,
    val toolRiskLevel: RiskLevel? = null,
    val isStreaming: Boolean = false
)

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

data class JarvisTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Normal", // Low, Normal, High
    val isRecurring: Boolean = false,
    val dueDate: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class JarvisTimer(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean = false
)

enum class ActivityType {
    TOOL_EXECUTION,
    VOICE_EVENT,
    SAFETY_ALERT,
    TASK_EVENT,
    SYSTEM_EVENT
}

enum class ExecutionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    BLOCKED
}

data class ActivityLog(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String,
    val type: ActivityType,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val status: ExecutionStatus = ExecutionStatus.SUCCESS,
    val userId: String = "local_operator",
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkType: String = "Online",
    val volumePercent: Int = 75,
    val isFlashlightOn: Boolean = false,
    val memoryAvailableMB: Long = 2048,
    val currentTimeString: String = ""
)

data class SafetyRequest(
    val id: String = UUID.randomUUID().toString(),
    val toolName: String,
    val actionDescription: String,
    val reason: String = "External interaction or state alteration requires confirmation",
    val riskLevel: RiskLevel,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

enum class AIProviderType {
    GEMINI,
    OPENAI_COMPATIBLE,
    LOCAL_NEURAL_BRAIN
}

enum class VoiceSynthesisEngine(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String
) {
    HYBRID_AUTO(
        id = "hybrid_auto",
        title = "Hybrid Smart (Recommended)",
        subtitle = "Cloud Studio + Offline Fallback",
        description = "Streams Gemini Studio ultra-realistic human speech when online, and instantly falls back to On-Device Neural WaveNet voice when offline."
    ),
    GEMINI_STUDIO(
        id = "gemini_studio",
        title = "Gemini Studio Human",
        subtitle = "Ultra-Realistic Expressive Voice",
        description = "Studio-grade neural human acoustics featuring natural breath pauses, emotional inflections, and cinematic prosody."
    ),
    NEURAL_DEVICE(
        id = "neural_device",
        title = "On-Device Neural",
        subtitle = "Zero Latency & 100% Offline",
        description = "Calibrated Google TTS Neural WaveNet engine with human conversational cadences."
    );

    companion object {
        fun fromId(id: String): VoiceSynthesisEngine =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: HYBRID_AUTO
    }
}

data class ProviderSettings(
    val providerType: AIProviderType = AIProviderType.GEMINI,
    val customApiKey: String = "",
    val customEndpoint: String = "https://api.openai.com/v1",
    val selectedModel: String = "gemini-2.5-flash",
    val systemPrompt: String = "You are JARVIS, an ultra-intelligent, deeply understanding AI assistant combining the conversational depth, eloquence, and reasoning power of ChatGPT with Tony Stark's futuristic personal OS. You are fluent in English, Hindi, Hinglish, and Bengali. Understand user questions with high empathy and analytical depth. When asked questions, provide clear, comprehensive, well-structured, and helpful answers with code, steps, or explanations just like ChatGPT. Adapt naturally to the user's language and tone.",
    val temperature: Float = 0.7f,
    val autoSpeakResponses: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val voiceProfileName: String = "JARVIS Natural",
    val languageCode: String = "auto",
    val continuousWakeEnabled: Boolean = true,
    val continuousConversationEnabled: Boolean = true,
    val lockScreenWakeEnabled: Boolean = true,
    val voiceSynthesisEngine: VoiceSynthesisEngine = VoiceSynthesisEngine.HYBRID_AUTO,
    val geminiVoiceName: String = "Puck"
)

data class VisionScan(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String? = null,
    val fileName: String,
    val fileSizeFormatted: String,
    val analysisResult: String = "",
    val status: String = "Pending", // Ready, Analyzing, Completed
    val timestamp: Long = System.currentTimeMillis()
)
