package com.example.studyplannerai.viewmodel.task

import com.example.studyplannerai.data.model.Task

data class TaskUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val taskAddedEvent: Int = 0,
    val taskUpdatedEvent: Int = 0,
    val taskDeletedEvent: Int = 0,
    val message: String? = null,
    val errorMessage: String? = null,
    val studyBlocks: List<com.example.studyplannerai.data.model.PlannedStudyBlock> = emptyList()
) {
    val totalTasks: Int
        get() = tasks.size

    val completedTasks: Int
        get() = tasks.count { it.isCompleted }

    val pendingTasks: Int
        get() = tasks.count { !it.isCompleted }

    val subjectCount: Int
        get() = tasks.map { it.subject.trim() }.filter { it.isNotBlank() }.distinct().size

    val completionPercent: Float
        get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()

    // Stub: count distinct completed days as streak
    val streakDays: Int
        get() = tasks.filter { it.isCompleted }.mapNotNull { it.deadline }.distinct().size

    // Stub: 60 minutes per completed task
    val totalMinutesStudied: Int
        get() = completedTasks * 60
}
