package com.example.studyplannerai.data.local

import androidx.room.*
import com.example.studyplannerai.data.model.Goal
import com.example.studyplannerai.data.model.StudyPlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<StudyPlanItem>>

    @Query("SELECT * FROM tasks WHERE day = :date")
    fun getTasksForDate(date: String): Flow<List<StudyPlanItem>>

    @Query("SELECT * FROM tasks WHERE day = :date AND status != 'completed' AND isCompleted = 0")
    fun getPendingTasksForDate(date: String): Flow<List<StudyPlanItem>>

    @Query("SELECT * FROM tasks WHERE day < :today AND status = 'pending' AND isCompleted = 0")
    fun getOverdueTasks(today: String): Flow<List<StudyPlanItem>>

    @Query("SELECT * FROM tasks WHERE day >= :startDate AND day <= :endDate")
    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<StudyPlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyPlanItem>)

    @Update
    suspend fun updateTask(task: StudyPlanItem)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Delete
    suspend fun deleteTask(task: StudyPlanItem)

    @Query("UPDATE tasks SET day = :newDay, isCarriedForward = 1, priority = MIN(priority + 1, 2), originalDay = CASE WHEN originalDay = '' THEN day ELSE originalDay END WHERE id = :taskId")
    suspend fun carryForwardTask(taskId: String, newDay: String)

    @Query("UPDATE tasks SET status = 'missed' WHERE day < :today AND isCompleted = 0 AND status = 'pending'")
    suspend fun markOverdueTasksAsMissed(today: String)

    @Query("UPDATE tasks SET completedMinutes = completedMinutes + :minutes, sessionCount = sessionCount + 1, lastFocusedAt = :timestamp WHERE id = :taskId")
    suspend fun recordFocusSession(taskId: String, minutes: Int, timestamp: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}

@Database(entities = [StudyPlanItem::class, Goal::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
}
