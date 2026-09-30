package com.example.jarvis.intent

enum class ConversationIntent {
    GREETING,
    CASUAL_CONVERSATION,
    QUESTION,
    EXPLANATION,
    ADVICE,
    MEMORY_REQUEST,
    DEVICE_INFORMATION,
    DEVICE_ACTION,
    FILE_OPERATION,
    DOCUMENT_ANALYSIS,
    FILE_GENERATION,
    WEB_RESEARCH,
    TASK_REQUEST,
    REMINDER_REQUEST,
    AUTOMATION_REQUEST,
    PLUGIN_REQUEST,
    SYSTEM_STATUS,
    HELP,
    CLARIFICATION,
    UNKNOWN
}

data class IntentDetectionResult(
    val intent: ConversationIntent,
    val confidence: Float = 1.0f,
    val requiresTool: Boolean = false,
    val suggestedToolName: String? = null,
    val missingParameters: List<String> = emptyList(),
    val clarificationQuestion: String? = null,
    val explanation: String = ""
)

object IntentClassifier {

    fun classify(userInput: String, contextHistory: String = ""): IntentDetectionResult {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        if (trimmed.isBlank()) {
            return IntentDetectionResult(
                intent = ConversationIntent.UNKNOWN,
                requiresTool = false,
                explanation = "Empty input"
            )
        }

        // 1. Ambiguous requests needing CLARIFICATION
        if (lower == "phone kar" || lower == "call kar" || lower == "call karo" || lower == "phone karo" || lower == "call") {
            return IntentDetectionResult(
                intent = ConversationIntent.CLARIFICATION,
                requiresTool = false,
                missingParameters = listOf("recipient"),
                clarificationQuestion = "Kisko call karun?",
                explanation = "Phone call requested without recipient contact or number"
            )
        }

        if (lower == "kal yaad dilana" || lower == "remind me tomorrow" || lower == "yaad dilana" || lower == "set reminder") {
            return IntentDetectionResult(
                intent = ConversationIntent.CLARIFICATION,
                requiresTool = false,
                missingParameters = listOf("time", "task"),
                clarificationQuestion = "Kis time pe kya yaad dilana hai bhai?",
                explanation = "Reminder requested without time or topic"
            )
        }

        // 2. Greetings
        val greetingRegex = Regex("""^(hi|hello|hey|hey jarvis|hello jarvis|salam|namaste|hi jarvis|greetings|ki khabor|কেমন আছো|হ্যালো)([\s!.,?]*)$""", RegexOption.IGNORE_CASE)
        if (greetingRegex.matches(trimmed) || lower == "hi" || lower == "hello" || lower == "hey" || lower == "hey jarvis" || lower == "hello jarvis") {
            return IntentDetectionResult(
                intent = ConversationIntent.GREETING,
                requiresTool = false,
                explanation = "User greeting detected"
            )
        }

        // 3. System Status (Explicit Diagnostics)
        if (lower.contains("system status") || lower.contains("diagnostics") || lower.contains("health check") ||
            lower.contains("system health") || lower == "system status bata") {
            return IntentDetectionResult(
                intent = ConversationIntent.SYSTEM_STATUS,
                requiresTool = true,
                suggestedToolName = "Diagnostics",
                explanation = "Explicit system status diagnostic request"
            )
        }

        // 3b. Holographic Orb Core Matrix
        if (lower.contains("orb") || lower.contains("hologram") || lower.contains("holographic") ||
            lower.contains("crimson mode") || lower.contains("overclock core") || lower.contains("spin orb") ||
            lower.contains("core theme") || lower.contains("core mode") || lower.contains("arc reactor")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "OrbCore",
                explanation = "Holographic Cybernetic Core control and telemetry request"
            )
        }

        // 3c. Tactical & Strategic Matrix (Combat Analysis)
        if (lower.contains("tactical") || lower.contains("combat protocol") || lower.contains("threat assessment") ||
            lower.contains("situation report") || lower.contains("sitrep") || lower.contains("mission plan")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "Tactical",
                explanation = "Tactical analysis and strategic contingency matrix request"
            )
        }

        // 3c-2. Stark Operational Protocols (Morning / Night / Perimeter / Surge)
        if (lower.contains("morning protocol") || lower.contains("good morning jarvis") || lower.contains("morning briefing") ||
            lower.contains("night protocol") || lower.contains("sleep protocol") || lower.contains("bedtime protocol") ||
            lower.contains("secure perimeter") || lower.contains("security protocol") || lower.contains("perimeter protocol") ||
            lower.contains("power surge") || lower.contains("overclock reactor") || lower.contains("maximum power")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "StarkProtocol",
                explanation = "Stark Operational Executive Protocol directive"
            )
        }

        // 3d. Translation
        if (lower.contains("translate") || lower.contains("অনুবাদ") || lower.contains("anuvad") ||
            (lower.contains("mean in") && (lower.contains("hindi") || lower.contains("bengali") || lower.contains("english")))) {
            return IntentDetectionResult(
                intent = ConversationIntent.QUESTION,
                requiresTool = true,
                suggestedToolName = "Translation",
                explanation = "Language translation request"
            )
        }

        // 4. File Generation Pipeline
        if (lower.contains("pdf bana") || lower.contains("generate file") || lower.contains("create file") ||
            lower.contains("export file") || lower.contains("pdf") && (lower.contains("create") || lower.contains("generate") || lower.contains("make") || lower.contains("export") || lower.contains("save")) ||
            lower.contains("save as csv") || lower.contains("create a markdown") || lower.contains("save as txt") ||
            lower.contains("generate a json") || lower.contains("export to csv") || lower.contains("export to xlsx") ||
            lower.contains("create a csv") || lower.contains("create a docx") || lower.contains("create a xlsx") ||
            lower.contains("ek pdf bana")) {
            return IntentDetectionResult(
                intent = ConversationIntent.FILE_GENERATION,
                requiresTool = true,
                suggestedToolName = "FileGeneration",
                explanation = "File creation/export request"
            )
        }

        // 5. Document Analysis
        if (lower.contains("ye pdf samjha") || lower.contains("pdf samjha") || lower.contains("analyze document") ||
            lower.contains("summarize document") || lower.contains("inspect document") || lower.contains("ye doc samjha") ||
            lower.contains("summarize this doc") || lower.contains("explain this pdf")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DOCUMENT_ANALYSIS,
                requiresTool = true,
                suggestedToolName = "DocumentIntelligence",
                explanation = "Document or PDF analysis request"
            )
        }

        // 6. Memory Requests
        if (lower.contains("remember") || lower.contains("forget") || lower.contains("memor") ||
            lower.contains("yaad hai") || lower.contains("what did i say")) {
            return IntentDetectionResult(
                intent = ConversationIntent.MEMORY_REQUEST,
                requiresTool = true,
                suggestedToolName = "Memory",
                explanation = "Memory storage/retrieval query"
            )
        }

        // 7. Reminders
        if ((lower.contains("yaad dilana") || lower.contains("remind me") || lower.contains("set reminder")) &&
            (lower.contains("baje") || lower.contains("pm") || lower.contains("am") || lower.contains("at ") || lower.contains("kal"))) {
            return IntentDetectionResult(
                intent = ConversationIntent.REMINDER_REQUEST,
                requiresTool = true,
                suggestedToolName = "Tasks",
                explanation = "Reminder or timed task request"
            )
        }

        // 7b. App Launch Requests
        val isAppLaunchKeyword = lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") ||
                lower.contains(" kholo") || lower.contains(" khol") || lower.contains(" open karo") ||
                lower.contains(" launch karo") || lower.contains(" start karo") || lower.contains(" khol de") ||
                lower.contains(" khol do") || lower.endsWith(" kholo") || lower.endsWith(" khol") ||
                lower.contains(" app open") || lower.contains(" app kholo")
        val isExcludedOpen = lower.contains("open url") || lower.contains("open link") || lower.contains("open file") || lower.contains("open notes")

        if (isAppLaunchKeyword && !isExcludedOpen) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "AppLauncher",
                explanation = "Application launch request"
            )
        }

        // 7c. WhatsApp & SMS Messaging
        if (lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("হোয়াটসঅ্যাপ")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "WhatsAppMessage",
                explanation = "WhatsApp messaging dispatch request"
            )
        }

        if (lower.startsWith("send sms") || lower.startsWith("sms ") || lower.contains("sms bhejo") || lower.contains("text message") || lower.startsWith("send text")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "SmsMessage",
                explanation = "SMS text dispatch request"
            )
        }

        // 7d. Calendar & Agenda
        if (lower.contains("calendar") || lower.contains("schedule meeting") || lower.contains("add event") || lower.contains("appointment") || lower.contains("agenda") || lower.contains("कैलेंडर") || lower.contains("ক্যালেন্ডার")) {
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = "CalendarEvent",
                explanation = "Calendar scheduling or query request"
            )
        }

        // 8. Device Actions & Info
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("wifi") ||
            lower.contains("volume") || lower.contains("bluetooth") || lower.startsWith("call ") || lower.startsWith("dial ")) {
            val suggestedTool = when {
                lower.contains("torch") || lower.contains("flashlight") -> "Flashlight"
                lower.contains("wifi") -> "Wifi"
                lower.contains("volume") -> "Volume"
                lower.contains("bluetooth") -> "Bluetooth"
                lower.startsWith("call") || lower.startsWith("dial") -> "PhoneCall"
                else -> "AndroidSettings"
            }
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_ACTION,
                requiresTool = true,
                suggestedToolName = suggestedTool,
                explanation = "Device state modification action"
            )
        }

        if (lower.contains("battery") || lower.contains("power level") || lower.contains("network status")) {
            val suggestedTool = when {
                lower.contains("battery") -> "Battery"
                else -> "NetworkStatus"
            }
            return IntentDetectionResult(
                intent = ConversationIntent.DEVICE_INFORMATION,
                requiresTool = true,
                suggestedToolName = suggestedTool,
                explanation = "Device status inspection query"
            )
        }

        // 10. Web Research
        if (lower.contains("latest news") || lower.contains("search the web") || lower.contains("research about") ||
            lower.contains("verify this claim") || lower.contains("official information") || lower.contains("compare these sources") ||
            lower.contains("reliable sources say") || lower.startsWith("google ") || lower.startsWith("web search")) {
            return IntentDetectionResult(
                intent = ConversationIntent.WEB_RESEARCH,
                requiresTool = true,
                suggestedToolName = "WebResearch",
                explanation = "Web research or live information query"
            )
        }

        // 11. Task Requests
        if (lower.startsWith("add task") || lower.startsWith("create task") || lower.contains("my tasks") ||
            lower.contains("list tasks") || lower.contains("pending tasks")) {
            return IntentDetectionResult(
                intent = ConversationIntent.TASK_REQUEST,
                requiresTool = true,
                suggestedToolName = "Tasks",
                explanation = "Task list management request"
            )
        }

        // 12. File Operations
        if (lower.contains("open notes file") || lower.contains("list my files") || lower.contains("my files") ||
            lower.startsWith("open file") || lower.startsWith("browse files")) {
            return IntentDetectionResult(
                intent = ConversationIntent.FILE_OPERATION,
                requiresTool = true,
                suggestedToolName = "UniversalSearch",
                explanation = "File browsing or opening request"
            )
        }

        // 13. Automation & Plugins
        if (lower.contains("run battery protocol") || lower.contains("execute workflow") || lower.contains("run automation")) {
            return IntentDetectionResult(
                intent = ConversationIntent.AUTOMATION_REQUEST,
                requiresTool = true,
                suggestedToolName = "Automation",
                explanation = "Automation workflow trigger"
            )
        }

        if (lower.contains("via plugin") || lower.contains("call plugin") || lower.contains("plugin API")) {
            return IntentDetectionResult(
                intent = ConversationIntent.PLUGIN_REQUEST,
                requiresTool = true,
                suggestedToolName = "Plugin",
                explanation = "Plugin execution request"
            )
        }

        // 14. Casual Conversation & Chatter
        if (lower == "kya haal hai?" || lower == "kya haal hai" || lower == "valo ache" || lower == "bhalo achi" ||
            lower == "kya kar raha hai?" || lower == "kya kar raha hai" || lower == "how are you" ||
            lower == "tell me a joke" || lower == "what's up" || lower == "whats up" || lower == "ki sob" || lower == "kemon achis") {
            return IntentDetectionResult(
                intent = ConversationIntent.CASUAL_CONVERSATION,
                requiresTool = false,
                explanation = "Casual user chatter"
            )
        }

        // 15. Help & Explanation & Advice & Questions
        if (lower == "mujhe help chahiye" || lower.contains("i need help") || lower.contains("help me")) {
            return IntentDetectionResult(
                intent = ConversationIntent.HELP,
                requiresTool = false,
                explanation = "User assistance request"
            )
        }

        if (lower.contains("samajh nahi aa raha") || lower.startsWith("help me understand") || lower.startsWith("explain ")) {
            return IntentDetectionResult(
                intent = ConversationIntent.EXPLANATION,
                requiresTool = false,
                explanation = "Conceptual explanation request"
            )
        }

        if (lower.startsWith("what should i do") || lower.startsWith("suggest ") || lower.contains("advice me")) {
            return IntentDetectionResult(
                intent = ConversationIntent.ADVICE,
                requiresTool = false,
                explanation = "Advice request"
            )
        }

        if (lower.startsWith("what is") || lower.startsWith("why is") || lower.startsWith("how does") || lower.startsWith("who is")) {
            return IntentDetectionResult(
                intent = ConversationIntent.QUESTION,
                requiresTool = false,
                explanation = "General question query"
            )
        }

        return IntentDetectionResult(
            intent = ConversationIntent.UNKNOWN,
            requiresTool = false,
            explanation = "General conversational query"
        )
    }
}
