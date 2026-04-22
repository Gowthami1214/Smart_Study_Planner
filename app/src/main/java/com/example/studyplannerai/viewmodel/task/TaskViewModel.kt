package com.example.studyplannerai.viewmodel.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.data.model.Task
import com.example.studyplannerai.data.repository.TaskRepository
import com.example.studyplannerai.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository = TaskRepository(),
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        observeTasks()
    }

    fun addTask(
        title: String,
        subject: String,
        deadlineMillis: Long?,
        reminderTimeMillis: Long?
    ) {
        val trimmedTitle = title.trim()
        val trimmedSubject = subject.trim()

        when (val error = validateTaskInput(trimmedTitle, trimmedSubject, reminderTimeMillis)) {
            null -> Unit
            else -> {
                _uiState.update { it.copy(errorMessage = error) }
                return
            }
        }

        launchAction(successMessage = "Task saved.") {
            val savedTask = repository.addTask(
                Task(
                    title = trimmedTitle,
                    subject = trimmedSubject,
                    deadline = deadlineMillis,
                    reminderTime = reminderTimeMillis,
                    isCompleted = false
                )
            )

            rescheduleReminder(savedTask)
            _uiState.update { it.copy(taskAddedEvent = it.taskAddedEvent + 1) }
        }
    }

    fun updateTask(
        taskId: String,
        title: String,
        subject: String,
        deadlineMillis: Long?,
        reminderTimeMillis: Long?,
        isCompleted: Boolean
    ) {
        val trimmedTitle = title.trim()
        val trimmedSubject = subject.trim()

        when (val error = validateTaskInput(trimmedTitle, trimmedSubject, reminderTimeMillis)) {
            null -> Unit
            else -> {
                _uiState.update { it.copy(errorMessage = error) }
                return
            }
        }

        val existingTask = uiState.value.tasks.firstOrNull { it.id == taskId }
        if (existingTask == null) {
            _uiState.update { it.copy(errorMessage = "Task not found.") }
            return
        }

        launchAction(successMessage = "Task updated.") {
            val updatedTask = repository.updateTask(
                existingTask.copy(
                    title = trimmedTitle,
                    subject = trimmedSubject,
                    deadline = deadlineMillis,
                    reminderTime = reminderTimeMillis,
                    isCompleted = isCompleted
                )
            )

            if (existingTask.reminderTime != updatedTask.reminderTime || existingTask.title != updatedTask.title || existingTask.subject != updatedTask.subject) {
                cancelReminder(existingTask.id)
                rescheduleReminder(updatedTask)
            }

            _uiState.update { it.copy(taskUpdatedEvent = it.taskUpdatedEvent + 1) }
        }
    }

    fun deleteTask(task: Task) {
        launchAction(successMessage = "Task deleted.") {
            repository.deleteTask(task.id)
            cancelReminder(task.id)
            _uiState.update { it.copy(taskDeletedEvent = it.taskDeletedEvent + 1) }
        }
    }

    fun markTaskComplete(task: Task, isCompleted: Boolean) {
        launchAction {
            repository.markComplete(task.id, isCompleted)
            if (isCompleted) {
                cancelReminder(task.id)
            } else {
                rescheduleReminder(task.copy(isCompleted = false))
            }
        }
    }

    fun rescheduleReminder(task: Task) {
        if (task.isCompleted || task.reminderTime == null) return
        reminderScheduler.schedule(task)
    }

    fun cancelReminder(taskId: String) {
        reminderScheduler.cancel(taskId)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            repository.getTasks().collect { tasks ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tasks = tasks
                    )
                }
            }
        }
    }

    private fun validateTaskInput(
        title: String,
        subject: String,
        reminderTimeMillis: Long?
    ): String? {
        return when {
            title.isBlank() -> "Enter a task title."
            subject.isBlank() -> "Enter a subject."
            reminderTimeMillis != null && reminderTimeMillis <= System.currentTimeMillis() -> {
                "Reminder time must be in the future."
            }
            else -> null
        }
    }

    private fun launchAction(
        successMessage: String? = null,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, errorMessage = null) }
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

    class Factory(
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(
                reminderScheduler = ReminderScheduler(appContext)
            ) as T
        }
    }
}
