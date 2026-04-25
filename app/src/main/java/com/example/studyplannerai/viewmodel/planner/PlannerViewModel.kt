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

enum class PomodoroState { Idle, Running, Paused, Break }

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
    val showDndPermissionDialog: Boolean = false
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

    private val _pomodoroState = MutableStateFlow(PomodoroState.Idle)
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private val _pomodoroSecondsLeft = MutableStateFlow(25 * 60)
    val pomodoroSecondsLeft: StateFlow<Int> = _pomodoroSecondsLeft.asStateFlow()

    private val _pomodoroBreakSecondsLeft = MutableStateFlow(5 * 60)
    val pomodoroBreakSecondsLeft: StateFlow<Int> = _pomodoroBreakSecondsLeft.asStateFlow()

    private val _activePomodoroTaskId = MutableStateFlow<String?>(null)
    val activePomodoroTaskId: StateFlow<String?> = _activePomodoroTaskId.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        loadSavedPlan()
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

        val todayStr      = java.time.LocalDate.now().toString()
        val todayActive   = activeTasks.count  { it.day == todayStr }
        val todayCompleted = historyTasks.count { it.day == todayStr }
        val totalToday    = todayActive + todayCompleted
        // Only show progress when there are actual tasks for today
        val progress = if (totalToday > 0) todayCompleted.toFloat() / totalToday.toFloat() else 0f

        _uiState.update {
            it.copy(
                isLoading  = false,
                studyPlan  = activeTasks.sortedBy  { p -> p.time_slot },
                history    = historyTasks.sortedByDescending { p -> p.completedAt ?: 0 },
                progress   = progress
            )
        }
    }

    fun startPomodoro(taskId: String, durationMinutes: Int = 25) {
        if (_activePomodoroTaskId.value != taskId) {
            _activePomodoroTaskId.value = taskId
            _pomodoroSecondsLeft.value = durationMinutes * 60
            _pomodoroBreakSecondsLeft.value = 5 * 60
        }
        if (_pomodoroState.value != PomodoroState.Running && _pomodoroState.value != PomodoroState.Break) {
            _pomodoroState.value = PomodoroState.Running
            toggleDnd(true)
            startTimerJob(durationMinutes * 60)
        }
    }

    fun pausePomodoro() {
        if (_pomodoroState.value == PomodoroState.Running) {
            _pomodoroState.value = PomodoroState.Paused
            toggleDnd(false)
            timerJob?.cancel()
        }
    }

    fun resumePomodoro() {
        if (_pomodoroState.value == PomodoroState.Paused) {
            _pomodoroState.value = PomodoroState.Running
            toggleDnd(true)
            startTimerJob(25 * 60)
        }
    }

    fun resetPomodoro(durationMinutes: Int = 25) {
        timerJob?.cancel()
        _pomodoroState.value = PomodoroState.Idle
        toggleDnd(false)
        _pomodoroSecondsLeft.value = durationMinutes * 60
        _pomodoroBreakSecondsLeft.value = 5 * 60
    }

    fun dismissDndDialog() {
        _uiState.update { it.copy(showDndPermissionDialog = false) }
    }

    private fun toggleDnd(enabled: Boolean) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val filter = if (enabled) android.app.NotificationManager.INTERRUPTION_FILTER_NONE else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(filter)
            } else {
                if (enabled) {
                    _uiState.update { it.copy(showDndPermissionDialog = true) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startTimerJob(totalSeconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (_pomodoroState.value == PomodoroState.Running) {
                    if (_pomodoroSecondsLeft.value > 0) {
                        _pomodoroSecondsLeft.value--
                    } else {
                        _pomodoroState.value = PomodoroState.Break
                        toggleDnd(false)
                    }
                } else if (_pomodoroState.value == PomodoroState.Break) {
                    if (_pomodoroBreakSecondsLeft.value > 0) {
                        _pomodoroBreakSecondsLeft.value--
                    } else {
                        _pomodoroSecondsLeft.value = totalSeconds
                        _pomodoroBreakSecondsLeft.value = 5 * 60
                        _pomodoroState.value = PomodoroState.Idle
                        toggleDnd(false)
                        timerJob?.cancel()
                        break
                    }
                }
            }
        }
    }
}
