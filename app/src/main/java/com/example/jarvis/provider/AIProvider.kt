package com.example.jarvis.provider

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.jarvis.model.AIProviderType
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.vision.HeuristicVisionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ToolDecision(
    val useTool: Boolean,
    val toolName: String? = null,
    val toolInput: String? = null,
    val reasoning: String? = null
)

interface AIProvider {
    suspend fun generateResponse(
        prompt: String,
        systemInstruction: String,
        onChunkReceived: (String) -> Unit
    ): String

    suspend fun decideTool(
        userInput: String,
        availableTools: List<Pair<String, String>>,
        contextHistory: String
    ): ToolDecision

    suspend fun analyzeImage(
        prompt: String,
        bitmap: Bitmap
    ): String
}

class GeminiAIProvider(
    private val repository: JarvisRepository
) : AIProvider {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(
        prompt: String,
        systemInstruction: String,
        onChunkReceived: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val settings = repository.settings.value
        val effectiveApiKey = settings.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "MY_GEMINI_API_KEY") {
            return@withContext LocalNeuralBrainProvider.generateLocalResponse(prompt, onChunkReceived)
        }

        val model = if (settings.selectedModel.isNotBlank() && settings.selectedModel != "gemini-3.5-flash") {
            settings.selectedModel
        } else {
            "gemini-2.5-flash"
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveApiKey"

        val rootJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", prompt))
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        rootJson.put("contents", contentsArray)

        if (systemInstruction.isNotBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().apply { put(JSONObject().put("text", systemInstruction)) }
            sysObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysObj)
        }

        val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Gemini API error (HTTP ${response.code}): $responseBody")
        }

        val parsed = JSONObject(responseBody)
        val fullText = parsed.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: "JARVIS: No output generated."

        simulateStream(fullText, onChunkReceived)
        return@withContext fullText
    }

    override suspend fun decideTool(
        userInput: String,
        availableTools: List<Pair<String, String>>,
        contextHistory: String
    ): ToolDecision = withContext(Dispatchers.IO) {
        val settings = repository.settings.value
        val effectiveApiKey = settings.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }

        if (effectiveApiKey.isBlank() || effectiveApiKey == "MY_GEMINI_API_KEY") {
            return@withContext LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
        }

        try {
            val toolsCatalog = availableTools.joinToString("\n") { "• ${it.first}: ${it.second}" }
            val planningPrompt = """
                You are the JARVIS Executive Tool Planner.
                Given the user's input and the registered tool catalogue, decide if a tool should be executed.
                
                REGISTERED TOOLS:
                $toolsCatalog
                
                USER INPUT: "$userInput"
                
                Respond ONLY in strict JSON format:
                {
                  "useTool": true | false,
                  "toolName": "<Exact tool name from list or null>",
                  "toolInput": "<Refined argument or query to pass into tool>",
                  "reasoning": "<Short explanation>"
                }
            """.trimIndent()

            val model = "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveApiKey"

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val partsArray = JSONArray().apply { put(JSONObject().put("text", planningPrompt)) }
            contentsArray.put(JSONObject().put("parts", partsArray))
            rootJson.put("contents", contentsArray)

            // Generation config requesting JSON
            val genConfig = JSONObject().put("responseMimeType", "application/json")
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
            }

            val parsed = JSONObject(body)
            val jsonText = parsed.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val repairedDecision = com.example.jarvis.recovery.ResponseRepair.parseToolDecisionWithRepair(jsonText, userInput)
            repairedDecision ?: LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
        } catch (_: Exception) {
            LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
        }
    }

    override suspend fun analyzeImage(prompt: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val settings = repository.settings.value
        val effectiveApiKey = settings.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
        if (effectiveApiKey.isBlank() || effectiveApiKey == "MY_GEMINI_API_KEY") {
            return@withContext HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
        }

        try {
            val model = "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveApiKey"

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Bytes = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().put("text", prompt.ifBlank { "Describe this image for JARVIS logs." }))
            partsArray.put(JSONObject().put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Bytes)
            }))

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
            }

            val parsed = JSONObject(responseBody)
            parsed.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
        } catch (e: Exception) {
            HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
        }
    }

    private suspend fun simulateStream(fullText: String, onChunkReceived: (String) -> Unit) {
        val words = fullText.split(" ")
        val sb = StringBuilder()
        for (w in words) {
            sb.append(w).append(" ")
            onChunkReceived(sb.toString().trimEnd())
            delay(15)
        }
    }
}

class OpenAiCompatibleAIProvider(
    private val repository: JarvisRepository
) : AIProvider {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(
        prompt: String,
        systemInstruction: String,
        onChunkReceived: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val settings = repository.settings.value
        val endpoint = settings.customEndpoint.ifBlank { "https://api.openai.com/v1" }
        val cleanEndpoint = if (endpoint.endsWith("/")) endpoint.dropLast(1) else endpoint
        val url = "$cleanEndpoint/chat/completions"

        val rootJson = JSONObject()
        rootJson.put("model", settings.selectedModel.ifBlank { "gpt-4o-mini" })
        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })
        rootJson.put("messages", messages)

        val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.customApiKey}")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("OpenAI endpoint error (${response.code}): $bodyString")
        }

        val parsed = JSONObject(bodyString)
        val text = parsed.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content") ?: "JARVIS: No output."

        onChunkReceived(text)
        return@withContext text
    }

    override suspend fun decideTool(
        userInput: String,
        availableTools: List<Pair<String, String>>,
        contextHistory: String
    ): ToolDecision {
        // Fallback or use local planning for ultra-reliable latency
        return LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
    }

    override suspend fun analyzeImage(prompt: String, bitmap: Bitmap): String {
        return HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
    }
}

object LocalNeuralBrainProvider {

    suspend fun generateLocalResponse(prompt: String, onChunkReceived: (String) -> Unit): String {
        val cleanText = prompt.substringAfterLast("User: ").substringBefore("\n").trim()
        val textToProcess = if (cleanText.isNotBlank()) cleanText else prompt
        val intentResult = com.example.jarvis.intent.IntentClassifier.classify(textToProcess)

        val response = if (intentResult.intent == com.example.jarvis.intent.ConversationIntent.SYSTEM_STATUS) {
            "All JARVIS subsystems are online. Core matrices, battery, network, Wi-Fi, and autonomous memory stores are operating normally."
        } else {
            JarvisAutonomousBrain.generateAutonomousResponse(textToProcess)
        }

        // Stream text smoothly with realistic cadence
        val words = response.split(" ")
        val sb = StringBuilder()
        for (w in words) {
            sb.append(w).append(" ")
            onChunkReceived(sb.toString().trimEnd())
            delay(15)
        }
        return response
    }

    fun decideToolLocal(userInput: String, availableTools: List<Pair<String, String>>): ToolDecision {
        val intentResult = com.example.jarvis.intent.IntentClassifier.classify(userInput)
        if (!intentResult.requiresTool) {
            return ToolDecision(false, null, userInput, "Intent '${intentResult.intent}' is purely conversational")
        }
        if (intentResult.suggestedToolName != null) {
            return ToolDecision(true, intentResult.suggestedToolName, userInput, intentResult.explanation)
        }

        val lower = userInput.lowercase().trim()

        return when {
            // Flashlight / Torch
            lower.contains("flashlight") || lower.contains("torch") || lower.contains("ফ্ল্যাশলাইট") || lower.contains("फ्लैशलाइट") || lower.contains("লাইট") ->
                ToolDecision(true, "Flashlight", userInput, "Flashlight keyword detected")

            // WhatsApp Messaging
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("व्हाट्सएप") ->
                ToolDecision(true, "WhatsAppMessage", userInput, "WhatsApp dispatch request")

            // SMS Messaging
            lower.startsWith("send sms") || lower.startsWith("sms ") || lower.contains("sms bhejo") || lower.contains("text message") || lower.startsWith("send text") ->
                ToolDecision(true, "SmsMessage", userInput, "SMS messaging dispatch request")

            // Calendar & Events
            lower.contains("calendar") || lower.contains("schedule meeting") || lower.contains("add event") || lower.contains("appointment") || lower.contains("agenda") || lower.contains("कैलेंडर") || lower.contains("ক্যালেন্ডার") ->
                ToolDecision(true, "CalendarEvent", userInput, "Calendar scheduling and agenda query")

            // YouTube Search & Playback
            lower.contains("youtube") || lower.contains("ইউটিউব") || lower.contains("यूट्यूब") ->
                ToolDecision(true, "YouTubeSearch", userInput, "YouTube video search and launch")

            // Phone Call
            lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("phone ") ||
            lower.contains("call koro") || lower.contains("call karo") || lower.contains("কল করো") || lower.contains("ফোন করো") || lower.contains("कॉल करो") ->
                ToolDecision(true, "PhoneCall", userInput, "Voice phone call dispatch")

            // Battery
            lower.contains("battery") || lower.contains("power level") || lower.contains("charging") || lower.contains("ব্যাটারি") || lower.contains("बैटरी") ->
                ToolDecision(true, "Battery", userInput, "Battery inspection query")

            // Network / Internet
            lower.contains("network") || lower.contains("internet connection") || lower.contains("online status") ->
                ToolDecision(true, "NetworkStatus", userInput, "Network connectivity check")

            // Wi-Fi
            lower.contains("wifi") || lower.contains("wi-fi") ->
                ToolDecision(true, "Wifi", userInput, "Wi-Fi radio query")

            // Bluetooth
            lower.contains("bluetooth") ->
                ToolDecision(true, "Bluetooth", userInput, "Bluetooth radio status query")

            // Volume
            lower.contains("volume") || lower.contains("sound level") || lower.contains("louder") || lower.contains("quieter") ||
            lower.contains("ভলিউম") || lower.contains("আওয়াজ") || lower.contains("आवाज़") || lower.contains("aawaz") ->
                ToolDecision(true, "Volume", userInput, "Audio volume request")

            // Brightness
            lower.contains("brightness") || lower.contains("screen light") ->
                ToolDecision(true, "Brightness", userInput, "Display brightness query")

            // Media
            lower.contains("play music") || lower.contains("pause music") || lower.contains("next song") || lower.contains("skip track") ||
            lower.contains("play this") || lower.contains("play ") || lower.contains("pause") || lower.contains("গান চালাও") || lower.contains("गाना बजाओ") ||
            lower.contains("chalao") || lower.contains("bajao") ->
                ToolDecision(true, "MediaControl", userInput, "Media key event request")

            // App Launcher
            lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") ||
            lower.contains("kholo") || lower.contains("khol") || lower.contains("open karo") ||
            lower.contains("launch karo") || lower.contains("start karo") || lower.contains("खोलो") || lower.contains("খোলো") ->
                ToolDecision(true, "AppLauncher", userInput, "Application launch request")

            // Open URL
            lower.startsWith("open url") || lower.startsWith("browse to") || lower.contains("http://") || lower.contains("https://") ->
                ToolDecision(true, "OpenUrl", userInput, "External web URL navigation")

            // Android Settings
            lower.contains("setting") ->
                ToolDecision(true, "AndroidSettings", userInput, "Android settings launch request")

            // Clipboard
            lower.contains("clipboard") || lower.startsWith("copy ") || lower.contains("paste") ->
                ToolDecision(true, "Clipboard", userInput, "Clipboard buffer interaction")

            // Diagnostics / Self Test
            lower.contains("diagnostics") || lower.contains("self test") || lower.contains("system status") || lower.contains("health check") || lower.contains("run diagnostics") ->
                ToolDecision(true, "Diagnostics", userInput, "Comprehensive subsystem diagnostic self-test")

            // Holographic Orb Core Matrix
            lower.contains("orb") || lower.contains("hologram") || lower.contains("holographic") ||
            lower.contains("crimson mode") || lower.contains("overclock core") || lower.contains("spin orb") ||
            lower.contains("core theme") || lower.contains("core mode") || lower.contains("arc reactor") ->
                ToolDecision(true, "OrbCore", userInput, "Holographic Cybernetic Core control and telemetry")

            // Tactical & Strategic Matrix (Combat Protocols)
            lower.contains("tactical") || lower.contains("combat protocol") || lower.contains("threat assessment") ||
            lower.contains("situation report") || lower.contains("sitrep") || lower.contains("mission plan") ->
                ToolDecision(true, "Tactical", userInput, "Tactical situation analysis and strategic contingency planning")

            // Device Info / Hardware
            lower.contains("device info") || lower.contains("hardware") || lower.contains("specifications") || lower.contains("system telemetry") ->
                ToolDecision(true, "DeviceInfo", userInput, "Hardware diagnostics query")

            // Memory
            lower.startsWith("remember") || lower.startsWith("forget") || lower.contains("memory") || lower.contains("memories") ->
                ToolDecision(true, "Memory", userInput, "Room memory management")

            // Tasks / Reminders
            lower.startsWith("add task") || lower.startsWith("create task") || lower.startsWith("remind me") || lower.contains("my agenda") || lower.contains("my tasks") ->
                ToolDecision(true, "Tasks", userInput, "Room task scheduling")

            // Timer
            lower.startsWith("set timer") || lower.startsWith("timer for") || lower.contains("countdown") ->
                ToolDecision(true, "Timer", userInput, "Countdown timer armed")

            // Calculator
            lower.startsWith("calculate") || lower.matches(Regex(""".*[\d\s]+[\+\-\*\/][\d\s]+.*""")) || lower.contains("% of") ->
                ToolDecision(true, "Calculator", userInput, "Mathematical calculation request")

            // Date / Time
            lower.contains("what time") || lower.contains("current time") || lower.contains("what's the date") || lower.contains("today's date") ->
                ToolDecision(true, "DateTime", userInput, "Chrono telemetry request")

            // Accessibility Agent
            lower.startsWith("inspect screen") || lower.startsWith("tap on") || lower.contains("accessibility") || lower.startsWith("scroll") ->
                ToolDecision(true, "AccessibilityAgent", userInput, "Accessibility agent dispatch")

            // Notifications & Missed Alerts
            lower.contains("notification") || lower.contains("unread messages") || lower.contains("what did i miss") ||
            lower.contains("missed notifications") || lower.contains("important alerts") || lower.contains("security alerts") ||
            (lower.startsWith("what came from") || lower.startsWith("notifications from")) ->
                ToolDecision(true, "Notifications", userInput, "Notification intelligence inspection")

            // Vision & OCR extraction
            lower.contains("read this image") || lower.contains("read image") || lower.contains("read this") ||
            lower.contains("what does this image say") || lower.contains("what does this screenshot say") ||
            lower.contains("what's written here") || lower.contains("whats written here") ||
            lower.contains("explain this screenshot") || lower.contains("find the phone number in this image") ||
            lower.contains("is there an otp") || lower.contains("translate this image") ||
            lower.contains("summarize this image") || lower.contains("ছবির লেখা পড়ে") || lower.contains("ছবির লেখা") ||
            lower.contains("तस्वीर में क्या लिखा है") || lower.contains("ocr") ->
                ToolDecision(true, "VisionOcr", userInput, "Advanced Vision and OCR extraction")

            // Structured Deep Research Engine (Multi-round sub-question planning & evidence graphs)
            lower.startsWith("do deep research") || lower.startsWith("deep research") ||
            lower.contains("research this thoroughly") || lower.contains("research thoroughly") ||
            lower.startsWith("investigate this topic") || lower.startsWith("investigate and analyze") ||
            lower.contains("compare multiple sources") || lower.contains("find evidence for this claim") ||
            lower.contains("research and verify this") || lower.contains("give me a detailed research report") ||
            lower.contains("detailed research report") || lower.contains("exhaustive research") ||
            lower.startsWith("investigate ") ->
                ToolDecision(true, "DeepResearch", userInput, "Structured multi-round deep research with sub-question planning and evidence extraction")

            // Advanced Web Research & Source Verification
            lower.startsWith("research") || lower.contains("research about") || lower.contains("research on") ||
            lower.startsWith("search the web") || lower.contains("search the web for") ||
            lower.startsWith("find the latest") || lower.startsWith("find official information") ||
            lower.startsWith("check whether this is true") || lower.startsWith("check whether") ||
            lower.startsWith("verify this claim") || lower.startsWith("verify claim") || lower.startsWith("verify whether") ||
            lower.contains("compare these sources") || lower.contains("compare sources") ||
            lower.contains("what do reliable sources say") || lower.contains("what do sources say") ||
            lower.startsWith("search for") || lower.startsWith("google ") || lower.startsWith("web search") ->
                ToolDecision(true, "WebResearch", userInput, "Safe web research, multi-source corroboration, and source verification")

            // Universal Phone Search (Local apps, contacts, tasks, memories, notifications)
            lower.startsWith("search phone") || lower.startsWith("search on phone") || lower.startsWith("phone search") ||
            lower.startsWith("find on phone") || lower.startsWith("find in phone") || lower.startsWith("search my phone") ||
            lower.contains("search contact") || lower.contains("find contact") || lower.contains("search app") ||
            lower.contains("find app") || lower.contains("search memory") || lower.contains("search tasks") ||
            lower.startsWith("find ") || lower.startsWith("locate ") || lower.startsWith("lookup ") ||
            lower.contains("ফোনে সার্চ") || lower.contains("ফোন সার্চ") || lower.contains("খোঁজো") || lower.contains("ढूंढो") ->
                ToolDecision(true, "UniversalSearch", userInput, "Local universal phone index search")

            // Document Intelligence
            lower.contains("analyze document") || lower.contains("document summary") || lower.contains("document entities") ||
            lower.contains("summarize document") || lower.contains("document stats") || lower.contains("inspect document") ->
                ToolDecision(true, "DocumentIntelligence", userInput, "Document intelligence and analysis")

            // File Generation Pipeline (TXT, Markdown, CSV, JSON, PDF, DOCX, XLSX)
            lower.startsWith("generate file") || lower.startsWith("create file") || lower.startsWith("export file") ||
            lower.startsWith("save as") || lower.startsWith("export as") || lower.startsWith("make a file") ||
            lower.contains("create a pdf") || lower.contains("generate a pdf") || lower.contains("export to pdf") || lower.contains("save as pdf") ||
            lower.contains("create a csv") || lower.contains("generate a csv") || lower.contains("export to csv") || lower.contains("save as csv") ||
            lower.contains("create a docx") || lower.contains("generate a docx") || lower.contains("export to docx") || lower.contains("save as docx") || lower.contains("word document") ||
            lower.contains("create a xlsx") || lower.contains("generate a xlsx") || lower.contains("export to xlsx") || lower.contains("save as xlsx") || lower.contains("excel file") || lower.contains("spreadsheet") ||
            lower.contains("create a json") || lower.contains("generate a json") || lower.contains("export to json") || lower.contains("save as json") ||
            lower.contains("create a markdown") || lower.contains("generate markdown") || lower.contains("save as markdown") || lower.contains("export to markdown") ||
            lower.contains("create a text file") || lower.contains("generate text file") || lower.contains("save as txt") || lower.contains("export as txt") ->
                ToolDecision(true, "FileGeneration", userInput, "Structured file generation pipeline")

            // Advanced Code Analysis, Review, Explanation & Stack Trace Debugging
            lower.startsWith("analyze code") || lower.startsWith("review code") || lower.startsWith("explain code") ||
            lower.contains("explain this code") || lower.contains("what does this function do") ||
            lower.contains("find the bug") || lower.contains("find bug") || lower.contains("review this code") ||
            lower.contains("how can i improve this") || lower.contains("why is this error happening") ||
            lower.contains("debug this error") || lower.contains("analyze stack trace") || lower.contains("debug stack trace") ||
            lower.contains("code analysis") || lower.contains("code review") || lower.contains("check this code") ||
            lower.contains("analyze snippet") || (userInput.contains("```") && (lower.contains("bug") || lower.contains("error") || lower.contains("review") || lower.contains("explain"))) ->
                ToolDecision(true, "CodeAnalysis", userInput, "Advanced safe static code and stack trace analysis")

            // Dedicated High-Fidelity Translation Engine
            lower.startsWith("translate") || lower.contains("translate this") || lower.contains("translate to") ||
            lower.contains("translate into") || lower.contains("translate from") || lower.contains("translate english to") ||
            lower.contains("translate bangla to") || lower.contains("translate hindi to") ||
            lower.contains("mein translate karo") || lower.contains("me translate karo") || lower.contains("me translate kijiye") ||
            lower.contains("what does this mean in") || lower.contains("how do you say") ||
            userInput.contains("অনুবাদ করো") || userInput.contains("অনুবাদ করুন") || userInput.contains("অনুবাদ") ->
                ToolDecision(true, "Translation", userInput, "Dedicated multi-language translation engine")

            // Connected Services & Cloud Workspace Plugins
            lower.contains("cloud record") || lower.contains("workspace record") || lower.contains("cloud workspace") ||
            lower.startsWith("search cloud") || lower.startsWith("search workspace") || lower.startsWith("find cloud record") ||
            (lower.contains("search") && lower.contains("connected service")) ->
                ToolDecision(true, "SearchCloudRecords", userInput, "Connected cloud workspace search")

            lower.startsWith("create cloud record") || lower.startsWith("add cloud record") || lower.startsWith("save to cloud") ||
            lower.startsWith("new cloud record") ->
                ToolDecision(true, "CreateCloudRecord", userInput, "Create record in connected cloud workspace")

            lower.startsWith("delete cloud record") || lower.startsWith("remove cloud record") ->
                ToolDecision(true, "DeleteCloudRecord", userInput, "Delete record from connected cloud workspace")

            lower.contains("cloud account") || lower.contains("cloud storage quota") || lower.contains("workspace profile") ->
                ToolDecision(true, "GetCloudAccountProfile", userInput, "Retrieve connected cloud profile")

            lower.contains("cloud note") || lower.startsWith("search cloud notes") || lower.startsWith("search notes records") ||
            (lower.contains("search notes") && lower.contains("cloud")) ->
                ToolDecision(true, "SearchNotesRecords", userInput, "Search connected cloud notes")

            lower.startsWith("create cloud note") || lower.startsWith("add cloud note") || lower.startsWith("new cloud note") ->
                ToolDecision(true, "CreateNotesRecord", userInput, "Create note in connected cloud notebook")

            lower.startsWith("delete cloud note") || lower.startsWith("remove cloud note") ->
                ToolDecision(true, "DeleteNotesRecord", userInput, "Delete note from connected cloud notebook")

            // Automation Orchestration Engine
            lower.contains("workflow") || lower.contains("automation") || lower.contains("run automation") ||
            lower.contains("create workflow") || lower.contains("list workflows") || lower.contains("show workflows") ->
                ToolDecision(true, "AutomationEngine", userInput, "Automation Orchestration Engine")

            // Weather
            lower.contains("weather") || lower.contains("forecast") ->
                ToolDecision(true, "Weather", userInput, "Meteorological inquiry")

            else ->
                ToolDecision(useTool = false, toolName = null, toolInput = null, reasoning = "General conversational or semantic reasoning query")
        }
    }
}

class JarvisUnifiedAIProvider(
    private val repository: JarvisRepository
) : AIProvider {

    private val gemini = GeminiAIProvider(repository)
    private val openAi = OpenAiCompatibleAIProvider(repository)

    private fun getActiveProvider(): AIProvider {
        val settings = repository.settings.value
        val effectiveKey = settings.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }

        return when {
            settings.providerType == AIProviderType.OPENAI_COMPATIBLE && settings.customApiKey.isNotBlank() -> openAi
            settings.providerType == AIProviderType.GEMINI && effectiveKey.isNotBlank() && effectiveKey != "MY_GEMINI_API_KEY" -> gemini
            else -> object : AIProvider {
                override suspend fun generateResponse(
                    prompt: String,
                    systemInstruction: String,
                    onChunkReceived: (String) -> Unit
                ): String = LocalNeuralBrainProvider.generateLocalResponse(prompt, onChunkReceived)

                override suspend fun decideTool(
                    userInput: String,
                    availableTools: List<Pair<String, String>>,
                    contextHistory: String
                ): ToolDecision = LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)

                override suspend fun analyzeImage(prompt: String, bitmap: Bitmap): String =
                    HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
            }
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        systemInstruction: String,
        onChunkReceived: (String) -> Unit
    ): String {
        val retryPolicy = com.example.jarvis.recovery.RetryPolicy(maxAttempts = 3, initialBackoffMs = 250L, maxBackoffMs = 2000L)
        val result = com.example.jarvis.recovery.executeWithRetry(
            policy = retryPolicy,
            operationName = "AIProvider:generateResponse",
            source = "AI_PROVIDER"
        ) {
            getActiveProvider().generateResponse(prompt, systemInstruction, onChunkReceived)
        }

        return result.getOrElse { throwable ->
            val error = com.example.jarvis.recovery.ErrorClassifier.classify(throwable, source = "AI_PROVIDER")
            repository.logActivity(
                title = "AI Recovery Engaged",
                detail = "${error.category}: ${error.userSafeMessage}",
                type = com.example.jarvis.model.ActivityType.SYSTEM_EVENT
            )
            val local = LocalNeuralBrainProvider.generateLocalResponse(prompt, onChunkReceived)
            "$local\n\n*(Note: Cloud link temporarily unavailable [${error.category}]. Operating via onboard neural engine.)*"
        }
    }

    override suspend fun decideTool(
        userInput: String,
        availableTools: List<Pair<String, String>>,
        contextHistory: String
    ): ToolDecision {
        return try {
            getActiveProvider().decideTool(userInput, availableTools, contextHistory)
        } catch (_: Exception) {
            LocalNeuralBrainProvider.decideToolLocal(userInput, availableTools)
        }
    }

    override suspend fun analyzeImage(prompt: String, bitmap: Bitmap): String {
        return try {
            getActiveProvider().analyzeImage(prompt, bitmap)
        } catch (_: Exception) {
            HeuristicVisionProvider.analyzeLocal(bitmap, prompt)
        }
    }
}
