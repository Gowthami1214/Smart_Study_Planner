package com.example.studyplannerai.reminder

import android.content.Context
import androidx.work.*
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.data.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(task: StudyPlanItem) {
        val reminderOffset = task.reminder_offset_minutes ?: 10
        val delayMillis = calculateDelay(task.day, task.time_slot, reminderOffset)
        
        if (delayMillis < 0) return

        val inputData = Data.Builder()
            .putString("TASK_TITLE", task.topic)
            .putString("TASK_TOPIC", task.task)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(task.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            task.id,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun schedule(task: Task) {
        val reminderTime = task.reminderTime ?: return
        val delayMillis = reminderTime - System.currentTimeMillis()
        
        if (delayMillis <= 0) return

        val inputData = Data.Builder()
            .putString("TASK_TITLE", task.title)
            .putString("TASK_TOPIC", task.subject)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(task.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            task.id,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancel(taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(taskId)
    }

    private fun calculateDelay(dayStr: String, timeSlot: String, offsetMinutes: Int): Long {
        return try {
            val startTimeStr = timeSlot.split("-").firstOrNull()?.trim() ?: return -1L
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val timeDate = format.parse(startTimeStr) ?: return -1L
            
            val dayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = dayFormat.parse(dayStr) ?: return -1L
            
            val timeCalendar = java.util.Calendar.getInstance().apply { time = timeDate }
            val dayCalendar = java.util.Calendar.getInstance().apply { time = date }
            
            dayCalendar.set(java.util.Calendar.HOUR_OF_DAY, timeCalendar.get(java.util.Calendar.HOUR_OF_DAY))
            dayCalendar.set(java.util.Calendar.MINUTE, timeCalendar.get(java.util.Calendar.MINUTE))
            dayCalendar.set(java.util.Calendar.SECOND, 0)
            dayCalendar.set(java.util.Calendar.MILLISECOND, 0)
            
            val targetTime = dayCalendar.timeInMillis - (offsetMinutes * 60 * 1000)
            val now = System.currentTimeMillis()
            
            if (targetTime < now) -1L else targetTime - now
        } catch (e: Exception) {
            -1L
        }
    }
}
