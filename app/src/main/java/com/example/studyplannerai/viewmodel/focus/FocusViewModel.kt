package com.example.studyplannerai.viewmodel.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.local.TaskDao
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FocusState { Idle, Running, Paused, Break, Completed }

data class FocusSessionState(
    val isActive: Boolean = false,
    val taskId: String = "",
    val taskTitle: String = "",
    val taskTopic: String = "",
    val planId: String = "",
    val durationMinutes: Int = 25,
    val secondsLeft: Int = 25 * 60,
    val breakSecondsLeft: Int = 5 * 60,
    val state: FocusState = FocusState.Idle,
    val sessionCount: Int = 0,
    val totalFocusedMinutes: Int = 0,
    val showCompletionDialog: Boolean = false,
    val showDurationPicker: Boolean = false,
    val showDndPermissionDialog: Boolean = false,
    val selectedTask: StudyPlanItem? = null
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val taskDao: TaskDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sessionState = MutableStateFlow(FocusSessionState())
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null

    // ─── Duration Picker ─────────────────────────────────────────────

    fun showDurationPicker(task: StudyPlanItem) {
        _sessionState.update {
            it.copy(
                showDurationPicker = true,
                selectedTask = task,
                taskId = task.id,
                taskTitle = task.task,
                taskTopic = task.topic,
                planId = task.planId
            )
        }
    }

    fun dismissDurationPicker() {
        _sessionState.update { it.copy(showDurationPicker = false) }
    }

    // ─── Session Control ─────────────────────────────────────────────

    fun startSession(task: StudyPlanItem, durationMinutes: Int) {
        timerJob?.cancel()
        _sessionState.update {
            it.copy(
                isActive = true,
                taskId = task.id,
                taskTitle = task.task,
                taskTopic = task.topic,
                planId = task.planId,
                durationMinutes = durationMinutes,
                secondsLeft = durationMinutes * 60,
                breakSecondsLeft = 5 * 60,
                state = FocusState.Running,
                showDurationPicker = false,
                showCompletionDialog = false,
                selectedTask = task
            )
        }
        toggleDnd(true)
        startTimerJob()
    }

    fun pauseSession() {
        if (_sessionState.value.state == FocusState.Running) {
            _sessionState.update { it.copy(state = FocusState.Paused) }
            toggleDnd(false)
            timerJob?.cancel()
        }
    }

    fun resumeSession() {
        if (_sessionState.value.state == FocusState.Paused) {
            _sessionState.update { it.copy(state = FocusState.Running) }
            toggleDnd(true)
            startTimerJob()
        }
    }

    fun stopSession() {
        timerJob?.cancel()
        toggleDnd(false)

        // Record partial session if any time was spent
        val state = _sessionState.value
        val minutesSpent = state.durationMinutes - (state.secondsLeft / 60)
        if (minutesSpent > 0) {
            recordSession(minutesSpent)
        }

        _sessionState.update {
            FocusSessionState() // Reset to idle
        }
    }

    // ─── Session Completion ──────────────────────────────────────────

    private fun onSessionComplete() {
        toggleDnd(false)
        val state = _sessionState.value

        // Record the full session
        recordSession(state.durationMinutes)

        // Send notification
        sendCompletionNotification(state.taskTitle)

        _sessionState.update {
            it.copy(
                state = FocusState.Completed,
                showCompletionDialog = true,
                sessionCount = it.sessionCount + 1,
                totalFocusedMinutes = it.totalFocusedMinutes + it.durationMinutes
            )
        }
    }

    fun markTaskComplete() {
        val taskId = _sessionState.value.taskId
        viewModelScope.launch {
            studyRepository.updateTaskStatus(taskId, true)
        }
        _sessionState.update { FocusSessionState() }
    }

    fun startAnotherSession() {
        val state = _sessionState.value
        val task = state.selectedTask ?: return
        _sessionState.update { it.copy(showCompletionDialog = false) }
        startSession(task, state.durationMinutes)
    }

    fun takeBreak() {
        _sessionState.update {
            it.copy(
                state = FocusState.Break,
                breakSecondsLeft = 5 * 60,
                showCompletionDialog = false
            )
        }
        startBreakTimer()
    }

    fun dismissCompletionDialog() {
        _sessionState.update {
            it.copy(showCompletionDialog = false)
        }
    }

    fun dismissDndDialog() {
        _sessionState.update { it.copy(showDndPermissionDialog = false) }
    }

    // ─── Timer Engine ────────────────────────────────────────────────

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_sessionState.value.state == FocusState.Running) {
                delay(1000)
                val current = _sessionState.value
                if (current.state == FocusState.Running) {
                    if (current.secondsLeft > 0) {
                        _sessionState.update { it.copy(secondsLeft = it.secondsLeft - 1) }
                    } else {
                        onSessionComplete()
                        break
                    }
                }
            }
        }
    }

    private fun startBreakTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_sessionState.value.state == FocusState.Break) {
                delay(1000)
                val current = _sessionState.value
                if (current.state == FocusState.Break) {
                    if (current.breakSecondsLeft > 0) {
                        _sessionState.update { it.copy(breakSecondsLeft = it.breakSecondsLeft - 1) }
                    } else {
                        // Break is over, return to idle
                        _sessionState.update {
                            it.copy(
                                state = FocusState.Idle,
                                isActive = false
                            )
                        }
                        break
                    }
                }
            }
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────

    private fun recordSession(minutes: Int) {
        val taskId = _sessionState.value.taskId
        viewModelScope.launch {
            try {
                taskDao.recordFocusSession(taskId, minutes, System.currentTimeMillis())
            } catch (e: Exception) {
                android.util.Log.e("FocusVM", "Failed to record session", e)
            }
        }
    }

    // ─── DnD Toggle ──────────────────────────────────────────────────

    private fun toggleDnd(enabled: Boolean) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val filter = if (enabled)
                    NotificationManager.INTERRUPTION_FILTER_NONE
                else
                    NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(filter)
            } else if (enabled) {
                _sessionState.update { it.copy(showDndPermissionDialog = true) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── Notification ────────────────────────────────────────────────

    private fun sendCompletionNotification(taskTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "focus_session_complete"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Session",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when focus sessions complete"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Focus Session Complete! 🎉")
            .setContentText("Great work on: $taskTitle")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify("focus_complete".hashCode(), notification)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        toggleDnd(false)
    }
}
