package com.example.studyplannerai.data.model

data class Task(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
