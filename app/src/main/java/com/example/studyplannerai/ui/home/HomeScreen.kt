package com.example.studyplannerai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.Task
import com.example.studyplannerai.viewmodel.task.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel,
    innerPadding: PaddingValues,
    showWeeklyPlan: Boolean
) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now()
    val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val sortedPendingTasks = uiState.tasks
        .filterNot { it.isCompleted }
        .sortedBy { it.deadline ?: it.reminderTime ?: Long.MAX_VALUE }

    val todaysTasks = sortedPendingTasks.filter { task ->
        val deadline = task.deadline
        val reminder = task.reminderTime
        (deadline != null && deadline in todayStart until tomorrowStart) ||
            (reminder != null && reminder in todayStart until tomorrowStart)
    }

    val upcomingTasks = sortedPendingTasks.take(if (showWeeklyPlan) 6 else 4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Total tasks: ${uiState.totalTasks}")
                    Text("Completed: ${uiState.completedTasks}")
                    Text("Pending: ${uiState.pendingTasks}")
                    Text("Subjects: ${uiState.subjectCount}")
                    LinearProgressIndicator(
                        progress = { uiState.completionPercent },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${(uiState.completionPercent * 100).toInt()}% completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            TaskSectionCard(
                title = "Today's Tasks",
                emptyText = "No tasks scheduled for today.",
                tasks = todaysTasks
            )
        }

        item {
            TaskSectionCard(
                title = "Upcoming Tasks",
                emptyText = "No upcoming tasks right now.",
                tasks = upcomingTasks
            )
        }
    }
}

@Composable
private fun TaskSectionCard(
    title: String,
    emptyText: String,
    tasks: List<Task>
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (tasks.isEmpty()) {
                Text(emptyText)
            } else {
                tasks.forEach { task ->
                    TaskSummaryRow(task)
                }
            }
        }
    }
}

@Composable
private fun TaskSummaryRow(task: Task) {
    val zoneId = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(task.title, fontWeight = FontWeight.Medium)
        Text("Subject: ${task.subject}", style = MaterialTheme.typography.bodySmall)
        task.deadline?.let {
            Text(
                "Due ${Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        task.reminderTime?.let {
            Text(
                "Reminder ${Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime().format(timeFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider()
    }
}
