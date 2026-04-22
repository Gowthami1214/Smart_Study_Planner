package com.example.studyplannerai.data.repository

import com.example.studyplannerai.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TaskRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getTasks(): Flow<List<Task>> = callbackFlow {
        val listener = tasksCollection()
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val tasks = snapshot?.documents.orEmpty()
                    .map { document ->
                        Task(
                            id = document.id,
                            title = document.getString("title").orEmpty(),
                            subject = document.getString("subject").orEmpty(),
                            deadline = document.getLong("deadline"),
                            reminderTime = document.getLong("reminderTime"),
                            isCompleted = document.getBoolean("isCompleted") ?: false,
                            createdAt = document.getLong("createdAt") ?: 0L,
                            updatedAt = document.getLong("updatedAt") ?: 0L
                        )
                    }
                    .sortedWith(
                        compareBy<Task> { it.isCompleted }
                            .thenBy { it.deadline ?: Long.MAX_VALUE }
                            .thenBy { it.reminderTime ?: Long.MAX_VALUE }
                            .thenByDescending { it.updatedAt }
                    )

                trySend(tasks)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addTask(task: Task): Task {
        val taskId = task.id.ifBlank { UUID.randomUUID().toString() }
        val timestamp = System.currentTimeMillis()
        val savedTask = task.copy(
            id = taskId,
            createdAt = if (task.createdAt == 0L) timestamp else task.createdAt,
            updatedAt = timestamp
        )

        tasksCollection().document(taskId).set(savedTask.toMap()).await()
        return savedTask
    }

    suspend fun updateTask(task: Task): Task {
        require(task.id.isNotBlank()) { "Task id is required for updates." }

        val updatedTask = task.copy(updatedAt = System.currentTimeMillis())
        tasksCollection()
            .document(task.id)
            .set(updatedTask.toMap(), SetOptions.merge())
            .await()
        return updatedTask
    }

    suspend fun deleteTask(taskId: String) {
        tasksCollection().document(taskId).delete().await()
    }

    suspend fun markComplete(taskId: String, isCompleted: Boolean) {
        tasksCollection().document(taskId).update(
            mapOf(
                "isCompleted" to isCompleted,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    private fun Task.toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "subject" to subject,
        "deadline" to deadline,
        "reminderTime" to reminderTime,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun tasksCollection() = firestore.collection("users").document(requireUserId()).collection("tasks")

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User must be logged in to access tasks.")
    }
}
