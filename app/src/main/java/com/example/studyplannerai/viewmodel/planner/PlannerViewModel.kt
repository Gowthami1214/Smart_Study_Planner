package com.example.studyplannerai.viewmodel.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.data.model.Subject
import com.example.studyplannerai.data.model.StudyTask
import com.example.studyplannerai.data.repository.PlannerRepository
import com.example.studyplannerai.domain.ScheduleGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class PlannerViewModel(
    private val repository: PlannerRepository = PlannerRepository(),
    private val scheduleGenerator: ScheduleGenerator = ScheduleGenerator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlannerUiState(taskDeadlineInput = LocalDate.now().plusDays(1).toString())
    )
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        observePlannerData()
    }

    fun updateSubjectName(value: String) {
        _uiState.update { it.copy(subjectNameInput = value) }
    }

    fun startEditingSubject(subject: Subject) {
        _uiState.update {
            it.copy(
                editingSubjectId = subject.id,
                subjectNameInput = subject.name
            )
        }
    }

    fun cancelSubjectEditing() {
        _uiState.update {
            it.copy(
                editingSubjectId = null,
                subjectNameInput = ""
            )
        }
    }

    fun updateSelectedSubject(subjectId: String) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun updateTaskTitle(value: String) {
        _uiState.update { it.copy(taskTitleInput = value) }
    }

    fun updateTaskDescription(value: String) {
        _uiState.update { it.copy(taskDescriptionInput = value) }
    }

    fun updateTaskDeadline(value: String) {
        _uiState.update { it.copy(taskDeadlineInput = value) }
    }

    fun updateTaskEstimatedMinutes(value: String) {
        _uiState.update { it.copy(taskEstimatedMinutesInput = value.filter { character -> character.isDigit() }) }
    }

    fun startEditingTask(task: StudyTask) {
        _uiState.update {
            it.copy(
                editingTaskId = task.id,
                selectedSubjectId = task.subjectId,
                taskTitleInput = task.title,
                taskDescriptionInput = task.description,
                taskDeadlineInput = LocalDate.ofEpochDay(task.deadlineEpochDay).toString(),
                taskEstimatedMinutesInput = task.estimatedMinutes.toString()
            )
        }
    }

    fun cancelTaskEditing() {
        _uiState.update {
            it.copy(
                editingTaskId = null,
                taskTitleInput = "",
                taskDescriptionInput = "",
                taskDeadlineInput = LocalDate.now().plusDays(1).toString(),
                taskEstimatedMinutesInput = "60"
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    fun addSubject() {
        val name = uiState.value.subjectNameInput.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a subject name.") }
            return
        }

        launchAction(
            successMessage = if (uiState.value.editingSubjectId == null) "Subject added." else "Subject updated."
        ) {
            val editingSubjectId = uiState.value.editingSubjectId
            if (editingSubjectId == null) {
                repository.addSubject(name)
            } else {
                val existingSubject = uiState.value.subjects.firstOrNull { it.id == editingSubjectId }
                    ?: throw IllegalStateException("Subject not found.")
                repository.updateSubject(existingSubject.copy(name = name))
            }
            _uiState.update { state -> state.copy(subjectNameInput = "", editingSubjectId = null) }
        }
    }

    fun addTask() {
        val state = uiState.value
        val subjectId = state.selectedSubjectId
        if (subjectId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Add a subject before creating a task.") }
            return
        }

        val title = state.taskTitleInput.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a task title.") }
            return
        }

        val estimatedMinutes = state.taskEstimatedMinutesInput.toIntOrNull()
        if (estimatedMinutes == null || estimatedMinutes <= 0) {
            _uiState.update { it.copy(errorMessage = "Estimated minutes must be greater than 0.") }
            return
        }

        val deadline = try {
            LocalDate.parse(state.taskDeadlineInput.trim())
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Deadline must use YYYY-MM-DD format.") }
            return
        }

        launchAction(
            successMessage = if (state.editingTaskId == null) "Task added." else "Task updated."
        ) {
            val existingTask = state.tasks.firstOrNull { it.id == state.editingTaskId }
            if (existingTask == null) {
                repository.addTask(
                    StudyTask(
                        subjectId = subjectId,
                        title = title,
                        description = state.taskDescriptionInput.trim(),
                        deadlineEpochDay = deadline.toEpochDay(),
                        estimatedMinutes = estimatedMinutes,
                        completedMinutes = 0,
                        isCompleted = false
                    )
                )
            } else {
                val clampedCompleted = existingTask.completedMinutes.coerceAtMost(estimatedMinutes)
                repository.updateTask(
                    existingTask.copy(
                        subjectId = subjectId,
                        title = title,
                        description = state.taskDescriptionInput.trim(),
                        deadlineEpochDay = deadline.toEpochDay(),
                        estimatedMinutes = estimatedMinutes,
                        completedMinutes = clampedCompleted,
                        isCompleted = clampedCompleted >= estimatedMinutes
                    )
                )
            }

            _uiState.update {
                it.copy(
                    editingTaskId = null,
                    taskTitleInput = "",
                    taskDescriptionInput = "",
                    taskDeadlineInput = LocalDate.now().plusDays(1).toString(),
                    taskEstimatedMinutesInput = "60"
                )
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        launchAction(successMessage = "Subject removed.") {
            repository.deleteSubject(subjectId)
            if (_uiState.value.editingSubjectId == subjectId) {
                cancelSubjectEditing()
            }
        }
    }

    fun changeTaskProgress(task: StudyTask, deltaMinutes: Int) {
        launchAction {
            repository.updateTaskProgress(task, task.completedMinutes + deltaMinutes)
        }
    }

    fun toggleTaskCompletion(task: StudyTask, completed: Boolean) {
        launchAction {
            repository.setTaskCompletion(task, completed)
        }
    }

    fun deleteTask(taskId: String) {
        launchAction(successMessage = "Task removed.") {
            repository.deleteTask(taskId)
            if (_uiState.value.editingTaskId == taskId) {
                cancelTaskEditing()
            }
        }
    }

    fun applyReschedule() {
        val suggestedDeadlines = uiState.value.suggestedDeadlines
        if (suggestedDeadlines.isEmpty()) {
            _uiState.update { it.copy(message = "No overdue tasks need rescheduling.") }
            return
        }

        launchAction(successMessage = "Overdue tasks rescheduled.") {
            repository.applySuggestedDeadlines(suggestedDeadlines)
        }
    }

    private fun observePlannerData() {
        viewModelScope.launch {
            combine(
                repository.observeSubjects(),
                repository.observeTasks()
            ) { subjects, tasks ->
                val plan = scheduleGenerator.generate(tasks = tasks, subjects = subjects)
                val selectedSubjectId = _uiState.value.selectedSubjectId
                val validSelectedSubjectId = when {
                    subjects.isEmpty() -> null
                    selectedSubjectId != null && subjects.any { it.id == selectedSubjectId } -> selectedSubjectId
                    else -> subjects.first().id
                }

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        subjects = subjects,
                        tasks = tasks,
                        schedule = plan.blocks,
                        progressSummary = ProgressSummary.from(tasks),
                        suggestedDeadlines = plan.suggestedDeadlines,
                        selectedSubjectId = validSelectedSubjectId
                    )
                }
            }.collect {}
        }
    }

    private fun launchAction(
        successMessage: String? = null,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            runCatching { action() }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = successMessage) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "Something went wrong."
                        )
                    }
                }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlannerViewModel() as T
        }
    }
}
