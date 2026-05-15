package com.example.studyplannerai.viewmodel.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.data.model.StudyPlanItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.domain.repository.AiRepository
import com.example.studyplannerai.domain.repository.StudyRepository
import com.example.studyplannerai.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject



data class PlannerUiState(
    val isLoading: Boolean = false,
    val studyPlan: List<StudyPlanItem> = emptyList(),
    val temporaryPlan: List<StudyPlanItem> = emptyList(),
    val history: List<StudyPlanItem> = emptyList(),
    val topics: List<String> = emptyList(),
    val selectedTopics: List<String> = emptyList(),
    val errorMessage: String? = null,
    val progress: Float = 0f,
    val isPlanAccepted: Boolean = false,
    val showDndPermissionDialog: Boolean = false,
    // Date-grouped sections
    val todayTasks: List<StudyPlanItem> = emptyList(),
    val tomorrowTasks: List<StudyPlanItem> = emptyList(),
    val upcomingTasks: List<StudyPlanItem> = emptyList(),
    val missedTasks: List<StudyPlanItem> = emptyList(),
    val carryForwardCount: Int = 0,
    val dailyProgressMap: Map<String, Float> = emptyMap()
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val studyRepository: StudyRepository,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()


    init {
        loadSavedPlan()
        handleCarryForward()
    }

    private fun handleCarryForward() {
        viewModelScope.launch {
            // Mark old tasks as missed, then carry forward eligible ones
            studyRepository.markOverdueAsMissed()
            val result = studyRepository.carryForwardOverdueTasks()
            if (result is Resource.Success && (result.data ?: 0) > 0) {
                _uiState.update { it.copy(carryForwardCount = result.data ?: 0) }
            }
        }
    }

    fun loadSavedPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            studyRepository.getSavedPlanFlow().collect { tasks ->
                updatePlanAndProgress(tasks)
            }
        }
    }

    fun getTopics(subject: String) {
        viewModelScope.launch {
            // Clear previous topics before fetching new ones
            _uiState.update { it.copy(isLoading = true, errorMessage = null, topics = emptyList(), temporaryPlan = emptyList()) }
            when (val result = aiRepository.getTopicsForSubject(subject)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, topics = result.data ?: emptyList()) }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    /** Call this when the user opens the planning flow for a new subject. */
    fun resetPlanningFlow() {
        _uiState.update { it.copy(topics = emptyList(), temporaryPlan = emptyList(), selectedTopics = emptyList(), errorMessage = null) }
    }

    fun generateFinalSchedule(
        subjects: String,
        selectedTopics: List<String>,
        daysToComplete: Int = 7
    ) {
        val subjectList = subjects.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val planId = subjectList.joinToString(" & ")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isPlanAccepted = false, selectedTopics = selectedTopics) }
            when (val result = aiRepository.generateSchedule(selectedTopics, daysToComplete, "Flexible", 15)) {
                is Resource.Success -> {
                    val plan = (result.data ?: emptyList()).map { it.copy(planId = planId) }
                    _uiState.update { it.copy(isLoading = false, temporaryPlan = plan) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun acceptPlan() {
        viewModelScope.launch {
            val planToSave = _uiState.value.temporaryPlan
            if (planToSave.isEmpty()) return@launch
            
            _uiState.update { it.copy(isLoading = true) }
            when (val result = studyRepository.savePlan(planToSave)) {
                is Resource.Success -> {
                    updatePlanAndProgress(planToSave)
                    _uiState.update { it.copy(temporaryPlan = emptyList(), isPlanAccepted = true) }
                    
                    // Schedule reminders for each task
                    planToSave.forEach { item ->
                        reminderScheduler.schedule(item)
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearPlan() {
        viewModelScope.launch {
            _uiState.value.studyPlan.forEach { studyRepository.deleteTask(it.id) }
            _uiState.update { it.copy(studyPlan = emptyList(), temporaryPlan = emptyList(), topics = emptyList(), progress = 0f) }
        }
    }

    fun regeneratePlan(subjects: String, days: Int = 7) {
        generateFinalSchedule(subjects, _uiState.value.selectedTopics, days)
    }

    suspend fun chatWithAssistant(message: String): com.example.studyplannerai.core.util.Resource<String> {
        return aiRepository.chatWithAssistant(message, "")
    }

    fun toggleTaskCompletion(item: StudyPlanItem) {
        viewModelScope.launch {
            val isCompleted = !item.isCompleted
            when (val result = studyRepository.updateTaskStatus(item.id, isCompleted)) {
                is Resource.Success -> {
                    // DB updates automatically emit to getSavedPlanFlow in init
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteSingleTask(taskId: String) {
        viewModelScope.launch {
            studyRepository.deleteTask(taskId)
            // DB updates automatically emit to getSavedPlanFlow
        }
    }

    fun addCustomTask(day: String, title: String, topic: String, duration: Int) {
        viewModelScope.launch {
            val newTask = StudyPlanItem(
                id = "${System.currentTimeMillis()}",
                day = day,
                time_slot = "Custom",
                duration_minutes = duration,
                topic = topic,
                task = title,
                status = "pending"
            )
            val currentPlan = _uiState.value.studyPlan.toMutableList()
            currentPlan.add(newTask)
            studyRepository.savePlan(listOf(newTask))
            updatePlanAndProgress(currentPlan)
        }
    }

    fun rescheduleTask(taskId: String, newDay: String, newTime: String) {
        viewModelScope.launch {
            val task = _uiState.value.studyPlan.find { it.id == taskId } ?: return@launch
            val updated = task.copy(day = newDay, time_slot = newTime)
            studyRepository.updateTask(updated)
            // DB updates automatically emit to getSavedPlanFlow
        }
    }

    fun updateReminderOffset(taskId: String, offsetMins: Int) {
        viewModelScope.launch {
            val task = _uiState.value.studyPlan.find { it.id == taskId } ?: return@launch
            val updated = task.copy(reminder_offset_minutes = offsetMins)
            studyRepository.updateTask(updated)
            // DB updates automatically emit to getSavedPlanFlow
        }
    }

    fun regenerateSingleTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val task = _uiState.value.studyPlan.find { it.id == taskId }
            if (task == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            when (val result = aiRepository.generateScheduleForSubject(task.topic)) {
                is Resource.Success -> {
                    val newPlan = result.data ?: emptyList()
                    val replacement = newPlan.firstOrNull() ?: task
                    val updatedTask = task.copy(
                        task = replacement.task, 
                        duration_minutes = replacement.duration_minutes,
                        topic = replacement.topic
                    )
                    studyRepository.updateTask(updatedTask)
                    // DB updates automatically emit to getSavedPlanFlow
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun updatePlanAndProgress(plan: List<StudyPlanItem>) {
        val activeTasks  = plan.filter { !it.isCompleted && it.status != "completed" }
        val historyTasks = plan.filter {  it.isCompleted || it.status == "completed" }

        val today = java.time.LocalDate.now()
        val todayStr = today.toString()
        val tomorrowStr = today.plusDays(1).toString()

        // Date-grouped sections
        val todayTasks = activeTasks.filter { it.day == todayStr }.sortedWith(
            compareByDescending<StudyPlanItem> { it.priority }.thenBy { it.time_slot }
        )
        val tomorrowTasks = activeTasks.filter { it.day == tomorrowStr }.sortedBy { it.time_slot }
        val upcomingTasks = activeTasks.filter { it.day > tomorrowStr }.sortedWith(
            compareBy<StudyPlanItem> { it.day }.thenBy { it.time_slot }
        )
        val missedTasks = activeTasks.filter { it.day < todayStr && it.status == "missed" }

        // Calculate today's progress
        val todayActive   = todayTasks.size
        val todayCompleted = historyTasks.count { it.day == todayStr }
        val totalToday    = todayActive + todayCompleted
        val progress = if (totalToday > 0) todayCompleted.toFloat() / totalToday.toFloat() else 0f

        // Build daily progress map for the week
        val allDays = plan.map { it.day }.distinct().sorted()
        val dailyProgressMap = allDays.associateWith { day ->
            val dayTotal = plan.count { it.day == day }
            val dayCompleted = plan.count { it.day == day && (it.isCompleted || it.status == "completed") }
            if (dayTotal > 0) dayCompleted.toFloat() / dayTotal.toFloat() else 0f
        }

        _uiState.update {
            it.copy(
                isLoading  = false,
                studyPlan  = activeTasks.sortedWith(
                    compareBy<StudyPlanItem> { p -> p.day }.thenBy { p -> p.time_slot }
                ),
                history    = historyTasks.sortedByDescending { p -> p.completedAt ?: 0 },
                progress   = progress,
                todayTasks = todayTasks,
                tomorrowTasks = tomorrowTasks,
                upcomingTasks = upcomingTasks,
                missedTasks = missedTasks,
                dailyProgressMap = dailyProgressMap
            )
        }
    }


}
