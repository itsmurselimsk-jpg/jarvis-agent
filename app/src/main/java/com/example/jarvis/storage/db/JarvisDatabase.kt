package com.example.jarvis.storage.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_approved")
    val isUserApproved: Boolean = true,
    val tags: String = ""
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMemoriesSync(query: String): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemoriesSync(limit: Int = 10): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memories")
    fun getMemoryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Normal", // Low, Normal, High
    val isRecurring: Boolean = false,
    val dueDate: String? = null,
    val dueTimestamp: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, timestamp DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueTimestamp IS NOT NULL AND isCompleted = 0")
    suspend fun getScheduledTasksSync(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTaskCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}

@Entity(tableName = "notifications")
data class NotificationLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "OTHER",
    val priority: String = "NORMAL",
    val isOngoing: Boolean = false,
    val groupKey: String = ""
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 100")
    fun getRecentNotifications(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotificationsSync(limit: Int = 50): List<NotificationLogEntity>

    @Query("SELECT * FROM notifications WHERE priority = 'HIGH' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getHighPriorityNotificationsSync(limit: Int = 20): List<NotificationLogEntity>

    @Query("SELECT * FROM notifications WHERE category = :category ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getNotificationsByCategorySync(category: String, limit: Int = 30): List<NotificationLogEntity>

    @Query("SELECT * FROM notifications WHERE appTitle LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getNotificationsByAppSync(query: String, limit: Int = 30): List<NotificationLogEntity>

    @Query("SELECT * FROM notifications WHERE title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' OR appTitle LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun searchNotificationsSync(query: String, limit: Int = 50): List<NotificationLogEntity>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getNotificationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationLogEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String,
    val type: String,
    val riskLevel: String = "SAFE",
    val status: String = "SUCCESS",
    val userId: String = "local_operator",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 200")
    fun getLogsForUser(userId: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs WHERE userId = :userId")
    suspend fun clearLogsForUser(userId: String)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY timestamp DESC")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalSpentFlow(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalSpentSync(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val streakDays: Int = 1,
    val lastCompletedDateMillis: Long = System.currentTimeMillis(),
    val targetDaysPerWeek: Int = 7
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY streakDays DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)
}

@Database(
    entities = [
        MemoryEntity::class,
        TaskEntity::class,
        NotificationLogEntity::class,
        ActivityLogEntity::class,
        ExpenseEntity::class,
        HabitEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_operating_system.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
