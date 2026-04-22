package com.example.studyplannerai.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.studyplannerai.data.model.Task

class ReminderScheduler(
    private val context: Context
) {

    fun schedule(task: Task) {
        val reminderTime = task.reminderTime ?: return
        if (reminderTime <= System.currentTimeMillis()) {
            Log.w("ReminderScheduler", "Cannot schedule alarm for the past: ${task.title}")
            return
        }

        NotificationHelper.createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(task)

        Log.d("ReminderScheduler", "Scheduling alarm for task: ${task.title} at $reminderTime")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancel(taskId: String) {
        Log.d("ReminderScheduler", "Cancelling alarm for taskId: $taskId")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(taskId = taskId, title = "", subject = "")
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(task: Task): PendingIntent {
        return buildPendingIntent(
            taskId = task.id,
            title = task.title,
            subject = task.subject
        )
    }

    private fun buildPendingIntent(
        taskId: String,
        title: String,
        subject: String
    ): PendingIntent {
        val message = "Reminder for $title"
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TASK_SUBJECT, subject)
            putExtra(ReminderReceiver.EXTRA_TASK_MESSAGE, message)
        }

        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
