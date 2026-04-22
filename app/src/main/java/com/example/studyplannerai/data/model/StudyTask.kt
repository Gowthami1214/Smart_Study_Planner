package com.example.studyplannerai.data.model

data class StudyTask(
    val id: String = "",
    val subjectId: String = "",
    val title: String = "",
    val description: String = "",
    val deadlineEpochDay: Long = 0L,
    val estimatedMinutes: Int = 0,
    val completedMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val remainingMinutes: Int
        get() = (estimatedMinutes - completedMinutes).coerceAtLeast(0)
}
