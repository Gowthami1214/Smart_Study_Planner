package com.example.studyplannerai.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyplannerai.viewmodel.task.TaskUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskScreen(
    uiState: TaskUiState,
    onAddTask: (String, String, Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedReminderMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.taskAddedEvent) {
        if (uiState.taskAddedEvent > 0) {
            title = ""
            subject = ""
            selectedDateMillis = null
            selectedReminderMillis = null
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            TaskEditorFields(
                title = title,
                onTitleChange = { title = it },
                subject = subject,
                onSubjectChange = { subject = it },
                deadlineMillis = selectedDateMillis,
                onDeadlineChange = { selectedDateMillis = it },
                reminderMillis = selectedReminderMillis,
                onReminderChange = { selectedReminderMillis = it }
            )

            Button(
                onClick = { onAddTask(title, subject, selectedDateMillis, selectedReminderMillis) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Task")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorFields(
    title: String,
    onTitleChange: (String) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    deadlineMillis: Long?,
    onDeadlineChange: (Long?) -> Unit,
    reminderMillis: Long?,
    onReminderChange: (Long?) -> Unit
) {
    val context = LocalContext.current
    val zoneId = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

    fun showDeadlinePicker() {
        val initialDate = deadlineMillis?.toLocalDate(zoneId) ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                onDeadlineChange(selected)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    fun showReminderPicker() {
        val initialDateTime = reminderMillis?.toLocalDateTime(zoneId) ?: LocalDateTime.now().plusHours(1)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val selected = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                            .atZone(zoneId)
                            .toInstant()
                            .toEpochMilli()
                        onReminderChange(selected)
                    },
                    initialDateTime.hour,
                    initialDateTime.minute,
                    false
                ).show()
            },
            initialDateTime.year,
            initialDateTime.monthValue - 1,
            initialDateTime.dayOfMonth
        ).show()
    }

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Task title") },
        singleLine = true
    )

    OutlinedTextField(
        value = subject,
        onValueChange = onSubjectChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Subject") },
        singleLine = true
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = { showDeadlinePicker() }) {
            Text(deadlineMillis?.let { "Date: ${it.toLocalDate(zoneId).format(dateFormatter)}" } ?: "Pick Date")
        }
        OutlinedButton(onClick = { showReminderPicker() }) {
            Text(reminderMillis?.let { "Reminder: ${it.toLocalDateTime(zoneId).format(dateTimeFormatter)}" } ?: "Set Reminder")
        }
        if (deadlineMillis != null) {
            OutlinedButton(onClick = { onDeadlineChange(null) }) {
                Text("Clear Date")
            }
        }
        if (reminderMillis != null) {
            OutlinedButton(onClick = { onReminderChange(null) }) {
                Text("Clear Reminder")
            }
        }
    }
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}

private fun Long.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
}
