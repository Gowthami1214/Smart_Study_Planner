package com.example.studyplannerai.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.studyplannerai.data.model.Task
import com.example.studyplannerai.viewmodel.task.TaskUiState

@Composable
fun AddEditTaskDialog(
    task: Task,
    uiState: TaskUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?, Long?, Boolean) -> Unit
) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var subject by remember(task.id) { mutableStateOf(task.subject) }
    var deadlineMillis by remember(task.id) { mutableStateOf(task.deadline) }
    var reminderMillis by remember(task.id) { mutableStateOf(task.reminderTime) }
    val initialUpdateEvent = remember(task.id) { uiState.taskUpdatedEvent }

    LaunchedEffect(uiState.taskUpdatedEvent) {
        if (uiState.taskUpdatedEvent > initialUpdateEvent) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TaskEditorFields(
                    title = title,
                    onTitleChange = { title = it },
                    subject = subject,
                    onSubjectChange = { subject = it },
                    deadlineMillis = deadlineMillis,
                    onDeadlineChange = { deadlineMillis = it },
                    reminderMillis = reminderMillis,
                    onReminderChange = { reminderMillis = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uiState.isSaving,
                onClick = { onSave(title, subject, deadlineMillis, reminderMillis, task.isCompleted) }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
