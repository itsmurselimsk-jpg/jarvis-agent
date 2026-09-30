package com.example.jarvis.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.jarvis.model.ActivityLog
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.ExecutionStatus
import com.example.jarvis.model.JarvisTask
import com.example.jarvis.model.JarvisTimer
import com.example.jarvis.model.MemoryItem
import com.example.jarvis.model.MessageSender
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.model.VisionScan
import com.example.jarvis.model.VoiceSynthesisEngine
import com.example.jarvis.notification.JarvisNotification
import com.example.jarvis.notification.NotificationCategory
import com.example.jarvis.notification.NotificationPriority
import com.example.jarvis.security.EncryptedStorage
import com.example.jarvis.security.SensitiveDataFilter
import com.example.jarvis.storage.db.ActivityLogEntity
import com.example.jarvis.storage.db.JarvisDatabase
import com.example.jarvis.storage.db.MemoryEntity
import com.example.jarvis.storage.db.NotificationLogEntity
import com.example.jarvis.storage.db.TaskEntity
import com.example.jarvis.tasks.JarvisAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class JarvisRepository(private val context: Context) {

    private val db = JarvisDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_operating_layer_prefs", Context.MODE_PRIVATE)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _tasks = MutableStateFlow<List<JarvisTask>>(emptyList())
    val tasks: StateFlow<List<JarvisTask>> = _tasks.asStateFlow()

    private val _timers = MutableStateFlow<List<JarvisTimer>>(emptyList())
    val timers: StateFlow<List<JarvisTimer>> = _timers.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _visionScans = MutableStateFlow<List<VisionScan>>(emptyList())
    val visionScans: StateFlow<List<VisionScan>> = _visionScans.asStateFlow()

    private val _notifications = MutableStateFlow<List<JarvisNotification>>(emptyList())
    val notifications: StateFlow<List<JarvisNotification>> = _notifications.asStateFlow()

    private val _settings = MutableStateFlow(ProviderSettings())
    val settings: StateFlow<ProviderSettings> = _settings.asStateFlow()

    init {
        loadSettings()
        loadMessages()
        observeRoomEntities()
        initDefaultTimersAndLogs()
    }

    private fun loadSettings() {
        val encryptedKey = prefs.getString("encrypted_custom_api_key", "") ?: ""
        val decryptedKey = if (encryptedKey.isNotEmpty()) EncryptedStorage.decrypt(encryptedKey) else ""
        val endpoint = prefs.getString("custom_endpoint", "https://api.openai.com/v1") ?: ""
        var model = prefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        if (model == "gemini-3.5-flash") {
            model = "gemini-2.5-flash"
            prefs.edit().putString("selected_model", "gemini-2.5-flash").apply()
        }
        val autoSpeak = prefs.getBoolean("auto_speak", true)
        val speechRate = prefs.getFloat("speech_rate", 1.0f)
        val speechPitch = prefs.getFloat("speech_pitch", 1.0f)
        val voiceProfileName = prefs.getString("voice_profile_name", "JARVIS Natural") ?: "JARVIS Natural"
        val languageCode = prefs.getString("language_code", "auto") ?: "auto"
        val continuousWake = prefs.getBoolean("continuous_wake_enabled", true)
        val continuousConversation = prefs.getBoolean("continuous_conversation_enabled", true)
        val lockScreenWake = prefs.getBoolean("lock_screen_wake_enabled", true)
        val voiceEngineId = prefs.getString("voice_synthesis_engine", VoiceSynthesisEngine.HYBRID_AUTO.id) ?: VoiceSynthesisEngine.HYBRID_AUTO.id
        val voiceSynthesisEngine = VoiceSynthesisEngine.fromId(voiceEngineId)
        val geminiVoiceName = prefs.getString("gemini_voice_name", "Puck") ?: "Puck"

        _settings.value = ProviderSettings(
            customApiKey = decryptedKey,
            customEndpoint = endpoint,
            selectedModel = model,
            autoSpeakResponses = autoSpeak,
            speechRate = speechRate,
            speechPitch = speechPitch,
            voiceProfileName = voiceProfileName,
            languageCode = languageCode,
            continuousWakeEnabled = continuousWake,
            continuousConversationEnabled = continuousConversation,
            lockScreenWakeEnabled = lockScreenWake,
            voiceSynthesisEngine = voiceSynthesisEngine,
            geminiVoiceName = geminiVoiceName
        )
    }

    private fun observeRoomEntities() {
        scope.launch {
            db.memoryDao().getAllMemories().collect { entities ->
                if (entities.isEmpty()) {
                    // Seed initial operational memories if fresh
                    val defaults = listOf(
                        MemoryEntity(
                            title = "Primary User Protocol",
                            content = "User preference: Conciseness prioritized, proactive telemetry alerts enabled.",
                            category = "Preference"
                        ),
                        MemoryEntity(
                            title = "Security Clearance",
                            content = "Level 5 Executive Override active for local tool dispatch.",
                            category = "Security"
                        )
                    )
                    defaults.forEach { db.memoryDao().insertMemory(it) }
                } else {
                    _memories.value = entities.map {
                        MemoryItem(
                            id = it.id,
                            title = it.title,
                            content = it.content,
                            category = it.category,
                            timestamp = it.timestamp
                        )
                    }
                }
            }
        }

        scope.launch {
            db.taskDao().getAllTasks().collect { entities ->
                if (entities.isEmpty()) {
                    val defaults = listOf(
                        TaskEntity(
                            title = "Diagnostic Review of Quantum Core",
                            notes = "Check latency on neural tool router and telemetry bus.",
                            priority = "High"
                        ),
                        TaskEntity(
                            title = "Calibrate Vocal Modulation Profile",
                            notes = "Adjust speech synthesizer cadence.",
                            priority = "Normal"
                        )
                    )
                    defaults.forEach { db.taskDao().insertTask(it) }
                } else {
                    _tasks.value = entities.map {
                        JarvisTask(
                            id = it.id,
                            title = it.title,
                            notes = it.notes,
                            isCompleted = it.isCompleted,
                            priority = it.priority,
                            isRecurring = it.isRecurring,
                            dueDate = it.dueDate,
                            timestamp = it.timestamp
                        )
                    }
                }
            }
        }

        scope.launch {
            db.notificationDao().getRecentNotifications().collect { entities ->
                _notifications.value = entities.map {
                    val cat = try { NotificationCategory.valueOf(it.category) } catch (_: Exception) { NotificationCategory.OTHER }
                    val prio = try { NotificationPriority.valueOf(it.priority) } catch (_: Exception) { NotificationPriority.NORMAL }
                    JarvisNotification(
                        id = it.id,
                        packageName = it.packageName,
                        appTitle = it.appTitle,
                        title = it.title,
                        text = it.text,
                        timestamp = it.timestamp,
                        category = cat,
                        priority = prio,
                        isOngoing = it.isOngoing,
                        groupKey = it.groupKey
                    )
                }
            }
        }
    }

    private fun loadMessages() {
        val rawMessages = prefs.getString("saved_messages", null)
        if (!rawMessages.isNullOrBlank()) {
            try {
                val array = JSONArray(rawMessages)
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChatMessage(
                            id = obj.optString("id"),
                            sender = MessageSender.valueOf(obj.optString("sender", MessageSender.JARVIS.name)),
                            text = obj.optString("text"),
                            timestamp = obj.optLong("timestamp"),
                            toolCallName = if (obj.has("toolCallName")) obj.optString("toolCallName") else null
                        )
                    )
                }
                _messages.value = list
            } catch (_: Exception) {}
        } else {
            _messages.value = listOf(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = "JARVIS Core operational, Sir. All sensory arrays, tool executors, and device bridges are active. What shall we coordinate today?"
                )
            )
        }
    }

    private fun initDefaultTimersAndLogs() {
        _timers.value = listOf(
            JarvisTimer(
                label = "Focus Interval",
                totalSeconds = 1500,
                remainingSeconds = 1500
            )
        )
        _activityLogs.value = listOf(
            ActivityLog(
                title = "JARVIS Subsystem Initialization",
                detail = "Quantum telemetry, Room database, and hardware bridges online.",
                type = ActivityType.SYSTEM_EVENT
            )
        )
    }

    fun addMessage(message: ChatMessage) {
        val updated = _messages.value + message
        _messages.value = updated
        saveMessages(updated)
    }

    fun updateStreamingMessage(text: String) {
        val current = _messages.value
        if (current.isNotEmpty() && current.last().isStreaming) {
            val last = current.last().copy(text = text)
            _messages.value = current.dropLast(1) + last
        }
    }

    fun finalizeStreamingMessage(text: String) {
        val current = _messages.value
        if (current.isNotEmpty() && current.last().isStreaming) {
            val last = current.last().copy(text = text, isStreaming = false)
            val updated = current.dropLast(1) + last
            _messages.value = updated
            saveMessages(updated)
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        prefs.edit().remove("saved_messages").apply()
        logActivity("Conversation Cleared", "Purged local conversation history.", ActivityType.SYSTEM_EVENT)
    }

    private fun saveMessages(list: List<ChatMessage>) {
        val array = JSONArray()
        list.takeLast(100).forEach { msg ->
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("sender", msg.sender.name)
            obj.put("text", msg.text)
            obj.put("timestamp", msg.timestamp)
            msg.toolCallName?.let { obj.put("toolCallName", it) }
            array.put(obj)
        }
        prefs.edit().putString("saved_messages", array.toString()).apply()
    }

    fun addMemory(title: String, content: String, category: String = "General"): Boolean {
        if (SensitiveDataFilter.containsSensitiveData(content)) {
            logActivity("Security Guard Alert", "Memory rejected: contained unencrypted credentials/card numbers.", ActivityType.SAFETY_ALERT, RiskLevel.RESTRICTED)
            return false
        }
        val entity = MemoryEntity(
            title = title,
            content = content,
            category = category
        )
        scope.launch {
            db.memoryDao().insertMemory(entity)
        }
        logActivity("Memory Encoded", "Stored in Room: $title", ActivityType.SYSTEM_EVENT)
        return true
    }

    fun searchMemories(query: String): List<MemoryItem> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) return _memories.value
        val tokens = trimmed.split(" ").filter { it.length > 2 }
        return _memories.value.filter { memory ->
            val inTitle = memory.title.contains(trimmed, ignoreCase = true)
            val inContent = memory.content.contains(trimmed, ignoreCase = true)
            val inCategory = memory.category.contains(trimmed, ignoreCase = true)
            val tokenMatch = tokens.isNotEmpty() && tokens.any { token ->
                memory.title.contains(token, ignoreCase = true) || memory.content.contains(token, ignoreCase = true)
            }
            inTitle || inContent || inCategory || tokenMatch
        }
    }

    fun deleteMemoriesMatching(query: String): Int {
        val matches = searchMemories(query)
        if (matches.isNotEmpty()) {
            scope.launch {
                matches.forEach { db.memoryDao().deleteMemoryById(it.id) }
            }
            logActivity("Memory Purged", "Deleted ${matches.size} items matching '$query'", ActivityType.SYSTEM_EVENT)
        }
        return matches.size
    }

    fun deleteMemory(id: String) {
        scope.launch {
            db.memoryDao().deleteMemoryById(id)
        }
        logActivity("Memory Deleted", "Purged memory ID: $id", ActivityType.SYSTEM_EVENT)
    }

    fun clearAllMemories() {
        scope.launch {
            db.memoryDao().clearAllMemories()
            _memories.value = emptyList()
        }
        logActivity("Memory Store Purged", "All memories deleted from Room.", ActivityType.SAFETY_ALERT, RiskLevel.CONFIRMATION)
    }

    fun reloadMemories() {
        scope.launch {
            val list = db.memoryDao().getRecentMemoriesSync(100)
            _memories.value = list.map {
                MemoryItem(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    category = it.category,
                    timestamp = it.timestamp
                )
            }
        }
    }

    fun addTask(title: String, notes: String = "", priority: String = "Normal", dueDate: String? = null, dueTimestamp: Long? = null) {
        val entity = TaskEntity(
            title = title,
            notes = notes,
            priority = priority,
            dueDate = dueDate,
            dueTimestamp = dueTimestamp
        )
        scope.launch {
            db.taskDao().insertTask(entity)
            if (dueTimestamp != null && dueTimestamp > System.currentTimeMillis()) {
                JarvisAlarmScheduler.scheduleTaskReminder(context, entity.id, title, dueTimestamp)
            }
        }
        logActivity("Task Scheduled", title, ActivityType.TASK_EVENT)
    }

    fun toggleTask(id: String) {
        scope.launch {
            val tasks = _tasks.value
            val target = tasks.find { it.id == id } ?: return@launch
            val updated = target.copy(isCompleted = !target.isCompleted)
            db.taskDao().updateTask(
                TaskEntity(
                    id = updated.id,
                    title = updated.title,
                    notes = updated.notes,
                    isCompleted = updated.isCompleted,
                    priority = updated.priority,
                    isRecurring = updated.isRecurring,
                    dueDate = updated.dueDate,
                    timestamp = updated.timestamp
                )
            )
        }
    }

    fun deleteTask(id: String) {
        scope.launch {
            db.taskDao().deleteTaskById(id)
            JarvisAlarmScheduler.cancelReminder(context, id)
        }
        logActivity("Task Deleted", "Removed task ID: $id", ActivityType.TASK_EVENT)
    }

    fun reloadTasks() {
        scope.launch {
            val list = db.taskDao().getScheduledTasksSync()
            _tasks.value = list.map {
                JarvisTask(
                    id = it.id,
                    title = it.title,
                    notes = it.notes,
                    isCompleted = it.isCompleted,
                    priority = it.priority,
                    isRecurring = it.isRecurring,
                    dueDate = it.dueDate,
                    timestamp = it.timestamp
                )
            }
        }
    }

    fun updateTimer(id: String, remaining: Int, isRunning: Boolean) {
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(remainingSeconds = remaining, isRunning = isRunning) else it
        }
    }

    fun addTimer(label: String, seconds: Int) {
        val timer = JarvisTimer(label = label, totalSeconds = seconds, remainingSeconds = seconds)
        _timers.value = listOf(timer) + _timers.value
        logActivity("Timer Created", "$label for ${seconds}s", ActivityType.TASK_EVENT)
    }

    fun deleteTimer(id: String) {
        _timers.value = _timers.value.filter { it.id != id }
    }

    fun logActivity(title: String, detail: String, type: ActivityType, risk: RiskLevel = RiskLevel.SAFE) {
        val entry = ActivityLog(title = title, detail = detail, type = type, riskLevel = risk)
        _activityLogs.value = listOf(entry) + _activityLogs.value.take(49)
    }

    fun clearActivityLogs() {
        _activityLogs.value = emptyList()
    }

    fun addVisionScan(scan: VisionScan) {
        _visionScans.value = listOf(scan) + _visionScans.value
        logActivity("Vision File Analyzed", scan.fileName, ActivityType.TOOL_EXECUTION)
    }

    fun updateSettings(newSettings: ProviderSettings) {
        _settings.value = newSettings
        val encryptedKey = if (newSettings.customApiKey.isNotEmpty()) {
            EncryptedStorage.encrypt(newSettings.customApiKey)
        } else ""

        prefs.edit().apply {
            putString("encrypted_custom_api_key", encryptedKey)
            remove("custom_api_key") // Remove any old plaintext key
            putString("custom_endpoint", newSettings.customEndpoint)
            putString("selected_model", newSettings.selectedModel)
            putBoolean("auto_speak", newSettings.autoSpeakResponses)
            putFloat("speech_rate", newSettings.speechRate)
            putFloat("speech_pitch", newSettings.speechPitch)
            putString("voice_profile_name", newSettings.voiceProfileName)
            putString("language_code", newSettings.languageCode)
            putBoolean("continuous_wake_enabled", newSettings.continuousWakeEnabled)
            putBoolean("continuous_conversation_enabled", newSettings.continuousConversationEnabled)
            putBoolean("lock_screen_wake_enabled", newSettings.lockScreenWakeEnabled)
            putString("voice_synthesis_engine", newSettings.voiceSynthesisEngine.id)
            putString("gemini_voice_name", newSettings.geminiVoiceName)
            apply()
        }
        logActivity("Settings Updated", "AI Provider & speech config securely committed.", ActivityType.SYSTEM_EVENT)
    }

    fun clearNotifications() {
        scope.launch {
            db.notificationDao().clearAllNotifications()
            com.example.jarvis.notification.JarvisNotificationListenerService.clearLiveBuffer()
            _notifications.value = emptyList()
        }
        logActivity("Notification History Purged", "Cleared all stored notification intelligence records.", ActivityType.TOOL_EXECUTION)
    }

    fun deleteNotification(id: String) {
        scope.launch {
            db.notificationDao().deleteNotificationById(id)
            _notifications.value = _notifications.value.filterNot { it.id == id }
        }
    }

    suspend fun getRecentNotificationsSync(limit: Int = 50): List<JarvisNotification> {
        return db.notificationDao().getRecentNotificationsSync(limit).map {
            val cat = try { NotificationCategory.valueOf(it.category) } catch (_: Exception) { NotificationCategory.OTHER }
            val prio = try { NotificationPriority.valueOf(it.priority) } catch (_: Exception) { NotificationPriority.NORMAL }
            JarvisNotification(
                id = it.id,
                packageName = it.packageName,
                appTitle = it.appTitle,
                title = it.title,
                text = it.text,
                timestamp = it.timestamp,
                category = cat,
                priority = prio,
                isOngoing = it.isOngoing,
                groupKey = it.groupKey
            )
        }
    }

    suspend fun searchNotificationsSync(query: String): List<JarvisNotification> {
        return db.notificationDao().searchNotificationsSync(query).map {
            val cat = try { NotificationCategory.valueOf(it.category) } catch (_: Exception) { NotificationCategory.OTHER }
            val prio = try { NotificationPriority.valueOf(it.priority) } catch (_: Exception) { NotificationPriority.NORMAL }
            JarvisNotification(
                id = it.id,
                packageName = it.packageName,
                appTitle = it.appTitle,
                title = it.title,
                text = it.text,
                timestamp = it.timestamp,
                category = cat,
                priority = prio,
                isOngoing = it.isOngoing,
                groupKey = it.groupKey
            )
        }
    }

    fun wipeAllLocalData() {
        scope.launch {
            db.memoryDao().clearAllMemories()
            db.taskDao().clearAllTasks()
            db.notificationDao().clearAllNotifications()
            db.expenseDao().clearAllExpenses()
            prefs.edit().clear().apply()
            _messages.value = emptyList()
            _memories.value = emptyList()
            _tasks.value = emptyList()
            _timers.value = emptyList()
            _activityLogs.value = emptyList()
            _visionScans.value = emptyList()
        }
        logActivity("Complete Data Wipe", "All preferences, Room DB, and logs purged.", ActivityType.SAFETY_ALERT, RiskLevel.RESTRICTED)
    }

    // ==========================================
    // EXPENSE & FINANCIAL BUDGET REPOSITORY
    // ==========================================
    val expensesFlow: kotlinx.coroutines.flow.Flow<List<com.example.jarvis.storage.db.ExpenseEntity>> =
        db.expenseDao().getAllExpenses()

    fun addExpense(title: String, amount: Double, category: String = "General", notes: String = "") {
        val entity = com.example.jarvis.storage.db.ExpenseEntity(
            title = title,
            amount = amount,
            category = category,
            notes = notes
        )
        scope.launch {
            db.expenseDao().insertExpense(entity)
        }
        logActivity("Expense Recorded", "₹$amount for $title ($category)", ActivityType.TASK_EVENT)
    }

    fun deleteExpense(expense: com.example.jarvis.storage.db.ExpenseEntity) {
        scope.launch {
            db.expenseDao().deleteExpense(expense)
        }
        logActivity("Expense Removed", "Deleted: ${expense.title}", ActivityType.TASK_EVENT)
    }

    // ==========================================
    // HABIT & STREAK REPOSITORY
    // ==========================================
    val habitsFlow: kotlinx.coroutines.flow.Flow<List<com.example.jarvis.storage.db.HabitEntity>> =
        db.habitDao().getAllHabits()

    fun addHabit(name: String, targetDays: Int = 7) {
        val entity = com.example.jarvis.storage.db.HabitEntity(
            name = name,
            streakDays = 1,
            targetDaysPerWeek = targetDays
        )
        scope.launch {
            db.habitDao().insertHabit(entity)
        }
        logActivity("Habit Created", name, ActivityType.TASK_EVENT)
    }

    fun completeHabitToday(habit: com.example.jarvis.storage.db.HabitEntity) {
        val updated = habit.copy(
            streakDays = habit.streakDays + 1,
            lastCompletedDateMillis = System.currentTimeMillis()
        )
        scope.launch {
            db.habitDao().updateHabit(updated)
        }
        logActivity("Habit Checked-In", "${habit.name} 🔥 Streak: ${updated.streakDays} Days", ActivityType.TASK_EVENT)
    }

    fun deleteHabit(id: String) {
        scope.launch {
            db.habitDao().deleteHabitById(id)
        }
        logActivity("Habit Removed", id, ActivityType.TASK_EVENT)
    }
}
