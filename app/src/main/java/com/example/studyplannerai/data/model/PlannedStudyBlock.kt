package com.example.studyplannerai.data.model

data class PlannedStudyBlock(
    val taskId: String,
    val taskTitle: String,
    val subjectName: String,
    val dateEpochDay: Long,
    val plannedMinutes: Int,
    val suggestedDeadlineEpochDay: Long,
    val isRescheduled: Boolean
)
