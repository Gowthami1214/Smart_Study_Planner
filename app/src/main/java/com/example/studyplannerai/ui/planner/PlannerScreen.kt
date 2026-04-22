package com.example.studyplannerai.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.Task
import com.example.studyplannerai.ui.task.AddEditTaskDialog
import com.example.studyplannerai.ui.task.AddTaskScreen
import com.example.studyplannerai.ui.task.TaskListScreen
import com.example.studyplannerai.viewmodel.task.TaskViewModel

@Composable
fun PlannerScreen(
    taskViewModel: TaskViewModel,
    innerPadding: PaddingValues
) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AddTaskScreen(
            uiState = uiState,
            onAddTask = taskViewModel::addTask
        )

        TaskListScreen(
            uiState = uiState,
            onTaskCheckedChange = taskViewModel::markTaskComplete,
            onTaskEdit = { taskToEdit = it },
            onTaskDelete = { taskToDelete = it },
            modifier = Modifier.fillMaxWidth()
        )
    }

    taskToEdit?.let { task ->
        AddEditTaskDialog(
            task = task,
            uiState = uiState,
            onDismiss = { taskToEdit = null },
            onSave = { title, subject, deadline, reminder, isCompleted ->
                taskViewModel.updateTask(
                    taskId = task.id,
                    title = title,
                    subject = subject,
                    deadlineMillis = deadline,
                    reminderTimeMillis = reminder,
                    isCompleted = isCompleted
                )
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${task.title}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskViewModel.deleteTask(task)
                        taskToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
