package com.example.studyplannerai.data.repository

import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.domain.repository.AuthRepository
import com.example.studyplannerai.domain.repository.StudyRepository
import com.example.studyplannerai.domain.scheduler.ScheduleDistributor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.example.studyplannerai.data.local.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

class StudyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val taskDao: TaskDao
) : StudyRepository {

    companion object {
        private const val MAX_DAILY_MINUTES = 240 // 4 hours
        private const val MAX_CARRY_FORWARD_DAYS = 3 // After 3 days, mark as missed permanently
    }

    override suspend fun savePlan(plan: List<StudyPlanItem>): Resource<Unit> {
        val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
        
        // Save to Room (Offline-first)
        taskDao.insertTasks(plan)
        
        // Sync to Firestore (Background)
        val batch = firestore.batch()
        plan.forEach { item ->
            val planId = item.planId.ifBlank { "default_plan" }
            val docRef = firestore.collection("users").document(userId)
                .collection("plans").document(planId)
                .collection("tasks").document(item.id)
            batch.set(docRef, item)
        }
        batch.commit().addOnFailureListener { e -> 
            android.util.Log.e("StudyRepo", "Firestore sync failed", e) 
        }
        
        return Resource.Success(Unit)
    }

    override fun getSavedPlanFlow(): Flow<List<StudyPlanItem>> {
        return taskDao.getAllTasks()
    }

    override fun getAllTasksFlow(): Flow<List<StudyPlanItem>> {
        return taskDao.getAllTasks()
    }

    override fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<StudyPlanItem>> {
        return taskDao.getTasksForDateRange(startDate, endDate)
    }

    override suspend fun updateTaskStatus(itemId: String, isCompleted: Boolean): Resource<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
            val task = taskDao.getAllTasks().first().find { it.id == itemId }
            task?.let {
                val updatedTask = it.copy(
                    isCompleted = isCompleted,
                    status = if (isCompleted) "completed" else "pending",
                    completedAt = if (isCompleted) System.currentTimeMillis() else null
                )
                taskDao.updateTask(updatedTask)
                
                // Sync to Firestore
                val planId = updatedTask.planId.ifBlank { "default_plan" }
                firestore.collection("users").document(userId)
                    .collection("plans").document(planId)
                    .collection("tasks").document(itemId)
                    .set(updatedTask)
                    .addOnFailureListener { e -> android.util.Log.e("StudyRepo", "Update sync failed", e) }
                
                if (isCompleted) {
                    moveToHistory(updatedTask)
                }
            }
            return Resource.Success(Unit)
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "Failed to update task")
        }
    }

    override suspend fun getHistory(): Resource<List<StudyPlanItem>> {
        return try {
            val history = taskDao.getAllTasks().first().filter { it.isCompleted || it.status == "completed" }
            Resource.Success(history)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch history")
        }
    }

    override suspend fun moveToHistory(item: StudyPlanItem): Resource<Unit> {
        val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
        firestore.collection("users").document(userId)
            .collection("history").document(item.id)
            .set(mapOf(
                "task" to item.task,
                "completed_at" to (item.completedAt ?: System.currentTimeMillis()),
                "topic" to item.topic,
                "id" to item.id
            ))
            .addOnFailureListener { e -> android.util.Log.e("StudyRepo", "History sync failed", e) }
        return Resource.Success(Unit)
    }

    override suspend fun updateTask(item: StudyPlanItem): Resource<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
            taskDao.updateTask(item)
            val planId = item.planId.ifBlank { "default_plan" }
            firestore.collection("users").document(userId)
                .collection("plans").document(planId)
                .collection("tasks").document(item.id)
                .set(item)
                .addOnFailureListener { e -> android.util.Log.e("StudyRepo", "Update task sync failed", e) }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update task")
        }
    }

    override suspend fun deleteTask(itemId: String): Resource<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return Resource.Error("User not logged in")
            val tasks = taskDao.getAllTasks().first()
            val task = tasks.find { it.id == itemId }
            task?.let {
                taskDao.deleteTask(it)
                val planId = it.planId.ifBlank { "default_plan" }
                firestore.collection("users").document(userId)
                    .collection("plans").document(planId)
                    .collection("tasks").document(itemId)
                    .delete()
                    .addOnFailureListener { e -> android.util.Log.e("StudyRepo", "Delete task sync failed", e) }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete task")
        }
    }

    override suspend fun markOverdueAsMissed(): Resource<Unit> {
        return try {
            val today = LocalDate.now().toString()
            taskDao.markOverdueTasksAsMissed(today)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark overdue tasks")
        }
    }

    override suspend fun carryForwardOverdueTasks(): Resource<Int> {
        return try {
            val today = LocalDate.now()
            val todayStr = today.toString()

            // 1. Find all tasks from past days that are still pending
            val overdueTasks = taskDao.getOverdueTasks(todayStr).first()
            if (overdueTasks.isEmpty()) return Resource.Success(0)

            // 2. Filter out tasks that have been overdue for too long (> MAX_CARRY_FORWARD_DAYS)
            val (carryable, expired) = overdueTasks.partition { task ->
                val taskDate = runCatching { LocalDate.parse(task.originalDay.ifBlank { task.day }) }.getOrNull()
                taskDate != null && java.time.temporal.ChronoUnit.DAYS.between(taskDate, today) <= MAX_CARRY_FORWARD_DAYS
            }

            // Mark expired tasks as missed permanently
            expired.forEach { task ->
                taskDao.updateTask(task.copy(status = "missed"))
            }

            if (carryable.isEmpty()) return Resource.Success(0)

            // 3. Get today's existing tasks to calculate remaining capacity
            val todayTasks = taskDao.getTasksForDate(todayStr).first()
            val todayMinutes = todayTasks.sumOf { it.duration_minutes }
            var remainingMinutes = MAX_DAILY_MINUTES - todayMinutes

            // 4. Sort overdue tasks by priority (high first), then by original date (oldest first)
            val sorted = carryable.sortedWith(
                compareByDescending<StudyPlanItem> { it.priority }
                    .thenBy { it.day }
            )

            // 5. Move tasks: fill today first, overflow to tomorrow, etc.
            var carriedCount = 0
            var dayOffset = 0L
            var usedMinutesInOverflowDay = 0

            for (task in sorted) {
                if (dayOffset == 0L && remainingMinutes <= 0) {
                    // Today is full, start pushing to tomorrow
                    dayOffset = 1L
                    usedMinutesInOverflowDay = 0
                }
                if (dayOffset > 0L && usedMinutesInOverflowDay + task.duration_minutes > MAX_DAILY_MINUTES) {
                    // Overflow day is also full, go to next day
                    dayOffset++
                    usedMinutesInOverflowDay = 0
                }

                val targetDay = today.plusDays(dayOffset).toString()
                taskDao.carryForwardTask(task.id, targetDay)
                carriedCount++

                if (dayOffset == 0L) {
                    remainingMinutes -= task.duration_minutes
                } else {
                    usedMinutesInOverflowDay += task.duration_minutes
                }
            }

            // 6. Rebalance today's time slots after carry-forward
            val updatedTodayTasks = taskDao.getTasksForDate(todayStr).first()
                .filter { !it.isCompleted && it.status != "completed" }
            val rebalanced = ScheduleDistributor.rebalanceDayTimeslots(updatedTodayTasks)
            rebalanced.forEach { taskDao.updateTask(it) }

            Resource.Success(carriedCount)
        } catch (e: Exception) {
            android.util.Log.e("StudyRepo", "Carry forward failed", e)
            Resource.Error(e.message ?: "Failed to carry forward tasks")
        }
    }
}
