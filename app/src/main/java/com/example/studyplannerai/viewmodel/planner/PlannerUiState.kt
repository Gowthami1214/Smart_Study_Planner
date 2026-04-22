package com.example.studyplannerai.viewmodel.planner

import com.example.studyplannerai.data.model.PlannedStudyBlock
import com.example.studyplannerai.data.model.Subject
import com.example.studyplannerai.data.model.StudyTask

data class ProgressSummary(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val completionPercent: Int = 0,
    val totalEstimatedMinutes: Int = 0,
    val totalCompletedMinutes: Int = 0
) {
    companion object {
        fun from(tasks: List<StudyTask>): ProgressSummary {
            val totalTasks = tasks.size
            val completedTasks = tasks.count { it.isCompleted }
            val totalEstimated = tasks.sumOf { it.estimatedMinutes }
            val totalCompleted = tasks.sumOf { it.completedMinutes.coerceAtMost(it.estimatedMinutes) }
            val percent = if (totalEstimated == 0) 0 else ((totalCompleted * 100f) / totalEstimated).toInt()
            return ProgressSummary(
                totalTasks = totalTasks,
                completedTasks = completedTasks,
                pendingTasks = totalTasks - completedTasks,
                completionPercent = percent.coerceIn(0, 100),
                totalEstimatedMinutes = totalEstimated,
                totalCompletedMinutes = totalCompleted
            )
        }
    }
}

data class PlannerUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val subjects: List<Subject> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val schedule: List<PlannedStudyBlock> = emptyList(),
    val progressSummary: ProgressSummary = ProgressSummary(),
    val suggestedDeadlines: Map<String, Long> = emptyMap(),
    val subjectNameInput: String = "",
    val editingSubjectId: String? = null,
    val selectedSubjectId: String? = null,
    val taskTitleInput: String = "",
    val taskDescriptionInput: String = "",
    val taskDeadlineInput: String = "",
    val taskEstimatedMinutesInput: String = "60",
    val editingTaskId: String? = null,
    val message: String? = null,
    val errorMessage: String? = null
)
