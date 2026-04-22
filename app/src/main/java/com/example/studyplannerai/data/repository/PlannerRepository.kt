package com.example.studyplannerai.data.repository

import com.example.studyplannerai.data.model.Subject
import com.example.studyplannerai.data.model.StudyTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PlannerRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeSubjects(): Flow<List<Subject>> = callbackFlow {
        val listener = subjectsCollection()
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val subjects = snapshot?.documents.orEmpty().map { document ->
                    Subject(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }
                trySend(subjects)
            }

        awaitClose { listener.remove() }
    }

    fun observeTasks(): Flow<List<StudyTask>> = callbackFlow {
        val listener = tasksCollection()
            .orderBy("deadlineEpochDay", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val tasks = snapshot?.documents.orEmpty().map { document ->
                    StudyTask(
                        id = document.id,
                        subjectId = document.getString("subjectId").orEmpty(),
                        title = document.getString("title").orEmpty(),
                        description = document.getString("description").orEmpty(),
                        deadlineEpochDay = document.getLong("deadlineEpochDay") ?: 0L,
                        estimatedMinutes = (document.getLong("estimatedMinutes") ?: 0L).toInt(),
                        completedMinutes = (document.getLong("completedMinutes") ?: 0L).toInt(),
                        isCompleted = document.getBoolean("isCompleted") ?: false,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        updatedAt = document.getLong("updatedAt") ?: 0L
                    )
                }
                trySend(tasks)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addSubject(name: String) {
        val subjectId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        subjectsCollection().document(subjectId).set(
            mapOf(
                "name" to name,
                "createdAt" to timestamp
            )
        ).await()
    }

    suspend fun updateSubject(subject: Subject) {
        subjectsCollection().document(subject.id).update(
            mapOf(
                "name" to subject.name,
                "createdAt" to subject.createdAt
            )
        ).await()
    }

    suspend fun deleteSubject(subjectId: String) {
        val batch = firestore.batch()
        batch.delete(subjectsCollection().document(subjectId))

        tasksCollection()
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
            .documents
            .forEach { document ->
                batch.delete(document.reference)
            }

        batch.commit().await()
    }

    suspend fun addTask(task: StudyTask) {
        val taskId = task.id.ifBlank { UUID.randomUUID().toString() }
        val timestamp = System.currentTimeMillis()
        tasksCollection().document(taskId).set(
            mapOf(
                "subjectId" to task.subjectId,
                "title" to task.title,
                "description" to task.description,
                "deadlineEpochDay" to task.deadlineEpochDay,
                "estimatedMinutes" to task.estimatedMinutes,
                "completedMinutes" to task.completedMinutes,
                "isCompleted" to task.isCompleted,
                "createdAt" to if (task.createdAt == 0L) timestamp else task.createdAt,
                "updatedAt" to timestamp
            )
        ).await()
    }

    suspend fun updateTask(task: StudyTask) {
        tasksCollection().document(task.id).update(
            mapOf(
                "subjectId" to task.subjectId,
                "title" to task.title,
                "description" to task.description,
                "deadlineEpochDay" to task.deadlineEpochDay,
                "estimatedMinutes" to task.estimatedMinutes,
                "completedMinutes" to task.completedMinutes.coerceAtMost(task.estimatedMinutes),
                "isCompleted" to task.isCompleted,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateTaskProgress(task: StudyTask, completedMinutes: Int) {
        val safeCompletedMinutes = completedMinutes.coerceIn(0, task.estimatedMinutes)
        tasksCollection().document(task.id).update(
            mapOf(
                "completedMinutes" to safeCompletedMinutes,
                "isCompleted" to (safeCompletedMinutes >= task.estimatedMinutes),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun setTaskCompletion(task: StudyTask, completed: Boolean) {
        val completedMinutes = if (completed) {
            task.estimatedMinutes
        } else {
            task.completedMinutes
                .takeIf { it < task.estimatedMinutes }
                ?: (task.estimatedMinutes - 15).coerceAtLeast(0)
        }

        tasksCollection().document(task.id).update(
            mapOf(
                "isCompleted" to completed,
                "completedMinutes" to completedMinutes,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun deleteTask(taskId: String) {
        tasksCollection().document(taskId).delete().await()
    }

    suspend fun applySuggestedDeadlines(suggestedDeadlines: Map<String, Long>) {
        if (suggestedDeadlines.isEmpty()) return

        val batch = firestore.batch()
        suggestedDeadlines.forEach { (taskId, deadlineEpochDay) ->
            val reference = tasksCollection().document(taskId)
            batch.update(
                reference,
                mapOf(
                    "deadlineEpochDay" to deadlineEpochDay,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }
        batch.commit().await()
    }

    private fun subjectsCollection() = userDocument().collection("subjects")

    private fun tasksCollection() = userDocument().collection("tasks")

    private fun userDocument() = firestore.collection("users").document(requireUserId())

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User must be logged in to access planner data.")
    }
}
