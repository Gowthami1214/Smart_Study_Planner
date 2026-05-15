package com.example.studyplannerai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class StudyPlanItem(
    @PrimaryKey val id: String = "",
    val planId: String = "",
    val day: String = "",
    val time_slot: String = "",
    val duration_minutes: Int = 60,
    val topic: String = "",
    val task: String = "",
    val status: String = "pending", // "pending", "completed", "missed"
    val reminder_offset_minutes: Int = 10,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: Int = 0,              // 0=normal, 1=high (carried-over), 2=critical
    val isCarriedForward: Boolean = false,
    val originalDay: String = "",       // tracks original scheduled date before carry-forward
    
    // Focus Session Tracking
    val completedMinutes: Int = 0,      // total focused minutes on this task
    val sessionCount: Int = 0,          // how many focus sessions completed
    val lastFocusedAt: Long? = null     // timestamp of last focus session
)

data class UserStats(
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val lastActiveDate: String = ""
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val deadline: String = "",
    val targetTasks: Int = 0,
    val completedTasks: Int = 0,
    val isCompleted: Boolean = false
)

data class UserSettings(
    val studyStyle: String = "Balanced", // "Intense", "Relaxed", "Balanced"
    val sessionLength: Int = 45, // minutes
    val preferredTime: String = "Morning"
)
