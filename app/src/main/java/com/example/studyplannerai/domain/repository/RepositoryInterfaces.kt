package com.example.studyplannerai.domain.repository

import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.data.model.Goal
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun getTopicsForSubject(subject: String): Resource<List<String>>
    suspend fun generateSchedule(
        selectedTopics: List<String>,
        availableHours: Int,
        preferredTime: String,
        breakDuration: Int
    ): Resource<List<StudyPlanItem>>
    suspend fun generateScheduleForSubject(subject: String): Resource<List<StudyPlanItem>>
    suspend fun chatWithAssistant(userMessage: String, historyContext: String): Resource<String>
}

interface StudyRepository {
    suspend fun savePlan(plan: List<StudyPlanItem>): Resource<Unit>
    fun getSavedPlanFlow(): Flow<List<StudyPlanItem>>
    fun getAllTasksFlow(): Flow<List<StudyPlanItem>>
    suspend fun updateTaskStatus(itemId: String, isCompleted: Boolean): Resource<Unit>
    suspend fun getHistory(): Resource<List<StudyPlanItem>>
    suspend fun moveToHistory(item: StudyPlanItem): Resource<Unit>
    suspend fun updateTask(item: StudyPlanItem): Resource<Unit>
    suspend fun deleteTask(itemId: String): Resource<Unit>
}

interface GoalRepository {
    fun getGoals(): Flow<List<Goal>>
    suspend fun addGoal(goal: Goal): Resource<Unit>
    suspend fun updateGoal(goal: Goal): Resource<Unit>
    suspend fun deleteGoal(goal: Goal): Resource<Unit>
}

interface AuthRepository {
    fun getCurrentUserId(): String?
    fun isUserLoggedIn(): Boolean
    fun getCurrentUser(): com.google.firebase.auth.FirebaseUser?
    suspend fun signUp(email: String, password: String): Resource<Unit>
    suspend fun logIn(email: String, password: String): Resource<Unit>
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit>
    fun logOut()
}
