package com.example.studyplannerai.data.repository

import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.domain.repository.AuthRepository
import com.example.studyplannerai.domain.repository.StudyRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.example.studyplannerai.data.local.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StudyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val taskDao: TaskDao
) : StudyRepository {

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
}
