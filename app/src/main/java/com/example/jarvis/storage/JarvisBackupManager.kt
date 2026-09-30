package com.example.jarvis.storage

import android.content.Context
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.MemoryItem
import com.example.jarvis.model.MessageSender
import com.example.jarvis.storage.db.ExpenseEntity
import com.example.jarvis.storage.db.HabitEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupStats(
    val memoriesCount: Int,
    val tasksCount: Int,
    val expensesCount: Int,
    val habitsCount: Int,
    val chatMessagesCount: Int,
    val jsonString: String
)

data class ImportResult(
    val success: Boolean,
    val importedMemories: Int = 0,
    val importedTasks: Int = 0,
    val importedExpenses: Int = 0,
    val importedHabits: Int = 0,
    val errorMessage: String? = null
)

class JarvisBackupManager(
    private val context: Context,
    private val repository: JarvisRepository
) {

    suspend fun createFullBackupJson(): BackupStats {
        val memoriesList = repository.memories.value
        val tasksList = repository.tasks.value
        val expensesList = repository.expensesFlow.first()
        val habitsList = repository.habitsFlow.first()
        val messagesList = repository.messages.value

        val rootObj = JSONObject()
        rootObj.put("version", 1)
        rootObj.put("timestamp", System.currentTimeMillis())
        rootObj.put("timestampIso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        rootObj.put("protocol", "JARVIS_SYSTEM_BACKUP")

        // Memories
        val memArray = JSONArray()
        memoriesList.forEach { mem ->
            val obj = JSONObject().apply {
                put("id", mem.id)
                put("title", mem.title)
                put("content", mem.content)
                put("category", mem.category)
                put("timestamp", mem.timestamp)
            }
            memArray.put(obj)
        }
        rootObj.put("memories", memArray)

        // Tasks
        val tasksArray = JSONArray()
        tasksList.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("notes", t.notes)
                put("isCompleted", t.isCompleted)
                put("priority", t.priority)
            }
            tasksArray.put(obj)
        }
        rootObj.put("tasks", tasksArray)

        // Expenses
        val expArray = JSONArray()
        expensesList.forEach { exp ->
            val obj = JSONObject().apply {
                put("id", exp.id)
                put("title", exp.title)
                put("amount", exp.amount)
                put("category", exp.category)
                put("notes", exp.notes)
                put("timestamp", exp.timestamp)
            }
            expArray.put(obj)
        }
        rootObj.put("expenses", expArray)

        // Habits
        val habitsArray = JSONArray()
        habitsList.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("streakDays", h.streakDays)
                put("targetDaysPerWeek", h.targetDaysPerWeek)
                put("lastCompletedDateMillis", h.lastCompletedDateMillis)
            }
            habitsArray.put(obj)
        }
        rootObj.put("habits", habitsArray)

        // Chat History (Last 50)
        val chatArray = JSONArray()
        messagesList.takeLast(50).forEach { msg ->
            val obj = JSONObject().apply {
                put("sender", msg.sender.name)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            }
            chatArray.put(obj)
        }
        rootObj.put("chatHistory", chatArray)

        val jsonStr = rootObj.toString(2)
        return BackupStats(
            memoriesCount = memoriesList.size,
            tasksCount = tasksList.size,
            expensesCount = expensesList.size,
            habitsCount = habitsList.size,
            chatMessagesCount = messagesList.size,
            jsonString = jsonStr
        )
    }

    suspend fun restoreFromJson(jsonString: String): ImportResult {
        return try {
            val root = JSONObject(jsonString)

            var importedMemories = 0
            var importedTasks = 0
            var importedExpenses = 0
            var importedHabits = 0

            // Restore Memories
            if (root.has("memories")) {
                val arr = root.getJSONArray("memories")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "Memory Note")
                    val content = obj.optString("content", "")
                    val category = obj.optString("category", "General")
                    repository.addMemory(title = title, content = content, category = category)
                    importedMemories++
                }
            }

            // Restore Tasks
            if (root.has("tasks")) {
                val arr = root.getJSONArray("tasks")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "Task")
                    val notes = obj.optString("notes", "")
                    val priority = obj.optString("priority", "Normal")
                    repository.addTask(title = title, notes = notes, priority = priority)
                    importedTasks++
                }
            }

            // Restore Expenses
            if (root.has("expenses")) {
                val arr = root.getJSONArray("expenses")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "Expense")
                    val amount = obj.optDouble("amount", 0.0)
                    val category = obj.optString("category", "General")
                    val notes = obj.optString("notes", "")
                    repository.addExpense(title = title, amount = amount, category = category, notes = notes)
                    importedExpenses++
                }
            }

            // Restore Habits
            if (root.has("habits")) {
                val arr = root.getJSONArray("habits")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "Habit")
                    val target = obj.optInt("targetDaysPerWeek", 7)
                    repository.addHabit(name = name, targetDays = target)
                    importedHabits++
                }
            }

            ImportResult(
                success = true,
                importedMemories = importedMemories,
                importedTasks = importedTasks,
                importedExpenses = importedExpenses,
                importedHabits = importedHabits
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Invalid JSON payload"
            )
        }
    }
}
