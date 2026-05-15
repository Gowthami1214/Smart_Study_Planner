package com.example.studyplannerai.ui.planner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.planner.PlannerUiState
import com.example.studyplannerai.viewmodel.planner.PlannerViewModel
import java.time.LocalDate

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    plannerViewModel: PlannerViewModel,
    innerPadding: PaddingValues
) {
    val uiState by plannerViewModel.uiState.collectAsStateWithLifecycle()
    var subject by remember { mutableStateOf("") }
    var daysToComplete by remember { mutableStateOf("7") }
    val selectedTopics = remember { mutableStateListOf<String>() }
    var showPlanningFlow by remember { mutableStateOf(false) }
    var showModifyDialog by remember { mutableStateOf(false) }

    var selectedTask by remember { mutableStateOf<StudyPlanItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showCustomTaskDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    fun onRequestNewPlan() {
        if (uiState.studyPlan.isNotEmpty()) showModifyDialog = true
        else {
            plannerViewModel.resetPlanningFlow()
            selectedTopics.clear()
            subject = ""
            daysToComplete = "7"
            showPlanningFlow = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.studyPlan.isEmpty() && !showPlanningFlow) {
                EmptyStateSection(onStart = {
                    plannerViewModel.resetPlanningFlow()
                    selectedTopics.clear()
                    subject = ""
                    daysToComplete = "7"
                    showPlanningFlow = true
                })
            } else if (showPlanningFlow) {
                PlanningFlowSection(
                    uiState = uiState,
                    subject = subject,
                    daysToComplete = daysToComplete,
                    onSubjectChange = { subject = it },
                    onDaysChange = { daysToComplete = it },
                    selectedTopics = selectedTopics,
                    onToggleTopic = { topic ->
                        if (selectedTopics.contains(topic)) selectedTopics.remove(topic)
                        else selectedTopics.add(topic)
                    },
                    onGetTopics = { plannerViewModel.getTopics(subject) },
                    onGenerate = {
                        plannerViewModel.generateFinalSchedule(
                            subjects = subject,
                            selectedTopics = selectedTopics.toList(),
                            daysToComplete = daysToComplete.toIntOrNull() ?: 7
                        )
                    },
                    onAccept = {
                        plannerViewModel.acceptPlan()
                        showPlanningFlow = false
                        selectedTopics.clear()
                        subject = ""
                        daysToComplete = "7"
                    },
                    onCancel = {
                        showPlanningFlow = false
                        selectedTopics.clear()
                        subject = ""
                        daysToComplete = "7"
                    }
                )
            } else {
                ActivePlanSection(
                    plan = uiState.studyPlan,
                    history = uiState.history,
                    progress = uiState.progress,
                    onTaskClick = {
                        selectedTask = it
                        showOptionsSheet = true
                    },
                    onAddMore = { showCustomTaskDialog = true },
                    onScheduleAgain = { onRequestNewPlan() },
                    todayTasks = uiState.todayTasks,
                    tomorrowTasks = uiState.tomorrowTasks,
                    upcomingTasks = uiState.upcomingTasks,
                    missedTasks = uiState.missedTasks,
                    carryForwardCount = uiState.carryForwardCount,
                    dailyProgressMap = uiState.dailyProgressMap
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        var showReminderDialog by remember { mutableStateOf(false) }

        // Modification dialog: shown when user taps "New Plan" but one already exists
        if (showModifyDialog) {
            AlertDialog(
                onDismissRequest = { showModifyDialog = false },
                containerColor = Surface800,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text("You already have a plan!", style = MaterialTheme.typography.titleLarge, color = OnSurface100)
                },
                text = {
                    Text(
                        "Would you like to add a new subject to the existing plan, or replace everything with a fresh schedule?",
                        style = MaterialTheme.typography.bodyMedium, color = OnSurface200
                    )
                },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Violet500, Cyan400)))
                            .clickable {
                                showModifyDialog = false
                                plannerViewModel.resetPlanningFlow()
                                selectedTopics.clear()
                                subject = ""
                                daysToComplete = "7"
                                showPlanningFlow = true
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Add Subject", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showModifyDialog = false
                        plannerViewModel.clearPlan()
                        plannerViewModel.resetPlanningFlow()
                        selectedTopics.clear()
                        subject = ""
                        daysToComplete = "7"
                        showPlanningFlow = true
                    }) {
                        Text("Start Fresh", color = Rose400)
                    }
                }
            )
        }

        if (showOptionsSheet && selectedTask != null) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                TaskOptionsContent(
                    task = selectedTask!!,
                    onComplete = {
                        plannerViewModel.toggleTaskCompletion(it)
                        showOptionsSheet = false
                    },
                    onReschedule = { 
                        plannerViewModel.rescheduleTask(it.id, LocalDate.now().plusDays(1).toString(), it.time_slot)
                        showOptionsSheet = false 
                    },
                    onRegenerateTask = { 
                        plannerViewModel.regenerateSingleTask(it.id)
                        showOptionsSheet = false 
                    },
                    onAddReminder = {
                        showReminderDialog = true
                        showOptionsSheet = false
                    },
                    onDelete = {
                        plannerViewModel.deleteSingleTask(it.id)
                        showOptionsSheet = false
                    }
                )
            }
        }
        
        if (showReminderDialog && selectedTask != null) {
            AlertDialog(
                onDismissRequest = { showReminderDialog = false },
                title = { Text("Set Reminder Offset") },
                text = { Text("Notify me before the task starts:") },
                confirmButton = {
                    Row {
                        TextButton(onClick = { plannerViewModel.updateReminderOffset(selectedTask!!.id, 5); showReminderDialog = false }) { Text("5 min") }
                        TextButton(onClick = { plannerViewModel.updateReminderOffset(selectedTask!!.id, 10); showReminderDialog = false }) { Text("10 min") }
                        TextButton(onClick = { plannerViewModel.updateReminderOffset(selectedTask!!.id, 15); showReminderDialog = false }) { Text("15 min") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") }
                }
            )
        }
        
        if (showCustomTaskDialog) {
            AddCustomTaskDialog(
                onDismiss = { showCustomTaskDialog = false },
                onAdd = { day, title, topic, duration ->
                    plannerViewModel.addCustomTask(day, title, topic, duration)
                    showCustomTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun EmptyStateSection(onStart: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emptyScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emptyGlow"
    )
    Column(
        modifier = Modifier.fillMaxSize().background(Surface900).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size((120 * scale).dp).clip(CircleShape).background(Violet400.copy(alpha = glowAlpha * 0.25f)))
            Box(modifier = Modifier.size(88.dp).clip(CircleShape).background(Surface700).border(BorderStroke(1.dp, Violet400.copy(0.4f)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(44.dp), tint = Violet300)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("No Study Plans Yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnSurface100)
        Spacer(Modifier.height(8.dp))
        Text("Let AI craft the perfect study schedule for you.", style = MaterialTheme.typography.bodyMedium, color = OnSurface300)
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(Violet500, Cyan400)))
                .clickable { onStart() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Text("Create New Plan", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanningFlowSection(
    uiState: PlannerUiState,
    subject: String,
    daysToComplete: String,
    onSubjectChange: (String) -> Unit,
    onDaysChange: (String) -> Unit,
    selectedTopics: List<String>,
    onToggleTopic: (String) -> Unit,
    onGetTopics: () -> Unit,
    onGenerate: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Surface900)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.ArrowBack, null, tint = OnSurface200)
            }
            Text("New Study Plan", style = MaterialTheme.typography.titleMedium, color = OnSurface100)
        }

        if (uiState.temporaryPlan.isNotEmpty()) {
            PlanPreviewSection(
                plan = uiState.temporaryPlan,
                onAccept = onAccept,
                onRegenerate = onGenerate,
                isLoading = uiState.isLoading
            )
        } else if (uiState.topics.isNotEmpty()) {
            // Topic selection step
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("Pick topics to study", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnSurface100)
                Text("Subjects: ${subject.split(",").filter { it.isNotBlank() }.joinToString(" • ") { it.trim() }}", style = MaterialTheme.typography.bodySmall, color = OnSurface300)
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.topics.forEach { topic ->
                        val selected = selectedTopics.contains(topic)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleTopic(topic) },
                            label = { Text(topic, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Violet400.copy(0.2f),
                                selectedLabelColor = Violet300,
                                containerColor = Surface700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Surface600,
                                selectedBorderColor = Violet400.copy(0.5f),
                                enabled = true,
                                selected = selected
                            )
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Days reminder badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Amber400.copy(0.1f),
                    border = BorderStroke(1.dp, Amber400.copy(0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarToday, null, tint = Amber400, modifier = Modifier.size(16.dp))
                        Text("Schedule spans ${daysToComplete.toIntOrNull() ?: 7} days", style = MaterialTheme.typography.bodySmall, color = Amber400)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selectedTopics.isNotEmpty()) Brush.horizontalGradient(listOf(Violet500, Cyan400)) else Brush.horizontalGradient(listOf(Surface600, Surface600)))
                        .clickable(enabled = selectedTopics.isNotEmpty()) { onGenerate() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Generate Schedule", style = MaterialTheme.typography.titleMedium, color = if (selectedTopics.isNotEmpty()) Color.White else OnSurface300, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        } else {
            SubjectInputSection(
                subject = subject,
                daysToComplete = daysToComplete,
                onSubjectChange = onSubjectChange,
                onDaysChange = onDaysChange,
                onGenerate = onGetTopics,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
fun ActivePlanSection(
    plan: List<StudyPlanItem>,
    history: List<StudyPlanItem>,
    progress: Float,
    onTaskClick: (StudyPlanItem) -> Unit,
    onAddMore: () -> Unit,
    onScheduleAgain: () -> Unit,
    todayTasks: List<StudyPlanItem> = emptyList(),
    tomorrowTasks: List<StudyPlanItem> = emptyList(),
    upcomingTasks: List<StudyPlanItem> = emptyList(),
    missedTasks: List<StudyPlanItem> = emptyList(),
    carryForwardCount: Int = 0,
    dailyProgressMap: Map<String, Float> = emptyMap()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProgressCard(progress)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Task")
                }
                OutlinedButton(
                    onClick = onScheduleAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(4.dp))
                    Text("New Plan")
                }
            }
        }

        // ─── Carry-Forward Banner ───
        if (carryForwardCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Amber400.copy(0.1f),
                    border = BorderStroke(1.dp, Amber400.copy(0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Update, null, tint = Amber400, modifier = Modifier.size(20.dp))
                        Text(
                            "📌 $carryForwardCount task${if (carryForwardCount > 1) "s" else ""} carried forward from previous days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber400,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ─── Missed Tasks Section ───
        if (missedTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Missed",
                    count = missedTasks.size,
                    icon = Icons.Default.Warning,
                    color = Rose400
                )
            }
            items(missedTasks) { task ->
                PremiumTaskCard(task, onClick = { onTaskClick(task) })
            }
        }

        // ─── Today Section ───
        item {
            SectionHeader(
                title = "Today",
                count = todayTasks.size,
                icon = Icons.Default.Today,
                color = Violet300,
                progress = dailyProgressMap[java.time.LocalDate.now().toString()]
            )
        }
        if (todayTasks.isEmpty()) {
            item {
                EmptyDayCard("No tasks scheduled for today")
            }
        }
        items(todayTasks) { task ->
            PremiumTaskCard(task, onClick = { onTaskClick(task) })
        }

        // ─── Tomorrow Section ───
        if (tomorrowTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Tomorrow",
                    count = tomorrowTasks.size,
                    icon = Icons.Default.EventNote,
                    color = Cyan300,
                    progress = dailyProgressMap[java.time.LocalDate.now().plusDays(1).toString()]
                )
            }
            items(tomorrowTasks) { task ->
                PremiumTaskCard(task, onClick = { onTaskClick(task) })
            }
        }

        // ─── Upcoming Section ───
        if (upcomingTasks.isNotEmpty()) {
            val upcomingGrouped = upcomingTasks.groupBy { it.day }
            item {
                SectionHeader(
                    title = "Upcoming",
                    count = upcomingTasks.size,
                    icon = Icons.Default.DateRange,
                    color = Emerald400
                )
            }
            upcomingGrouped.forEach { (day, tasks) ->
                item {
                    DayHeader(day)
                }
                items(tasks) { task ->
                    PremiumTaskCard(task, onClick = { onTaskClick(task) })
                }
            }
        }
        
        // ─── History Section ───
        if (history.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Completed",
                    count = history.size,
                    icon = Icons.Default.CheckCircle,
                    color = Emerald400
                )
            }
            items(history.take(10)) { task ->
                PremiumTaskCard(task, onClick = { })
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    progress: Float? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface100
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Text(
                    "$count",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        progress?.let { pct ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Surface700
            ) {
                Text(
                    "${(pct * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pct >= 1f) Emerald400 else OnSurface200
                )
            }
        }
    }
}

@Composable
fun EmptyDayCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface800)
            .border(BorderStroke(1.dp, Surface600), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.EventAvailable, null, tint = OnSurface300, modifier = Modifier.size(18.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurface300)
        }
    }
}

// Re-using helper components from previous version
@Composable
fun PremiumTaskCard(task: StudyPlanItem, onClick: () -> Unit) {
    val isCompleted = task.status == "completed"
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    val borderColor = if (isCompleted) Emerald400.copy(0.3f) else Violet400.copy(0.2f)
    val iconColor   = if (isCompleted) Emerald400 else Violet300
    val iconBg      = if (isCompleted) Emerald400.copy(0.12f) else Violet400.copy(0.12f)
    val cardBg      = if (isCompleted) Surface700.copy(alpha = 0.5f) else Surface700

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable { pressed = true; onClick(); pressed = false }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rounded-square icon (more modern and recognizable than a bare circle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                    contentDescription = if (isCompleted) "Completed" else "Pending",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimeBadge(task.time_slot)
                    DurationBadge("${task.duration_minutes}m")
                    if (task.isCarriedForward) {
                        Surface(color = Amber400.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Amber400.copy(0.25f))) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Update, null, modifier = Modifier.size(10.dp), tint = Amber400)
                                Text("Carried", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Amber400)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.topic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) OnSurface300 else OnSurface100
                )
                Text(
                    text = task.task,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface300,
                    maxLines = 2
                )
            }
            // Explicit status label instead of bare chevron
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCompleted) Emerald400.copy(0.12f) else Violet400.copy(0.08f),
                border = BorderStroke(1.dp, if (isCompleted) Emerald400.copy(0.3f) else Violet400.copy(0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (isCompleted) Emerald400 else Violet300,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isCompleted) "Done" else "Tap",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Emerald400 else Violet300
                    )
                }
            }
        }
    }
}

@Composable
fun DurationBadge(duration: String) {
    Surface(color = Cyan400.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Cyan400.copy(0.25f))) {
        Text(text = duration, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Cyan300)
    }
}

@Composable
fun TaskOptionsContent(task: StudyPlanItem, onComplete: (StudyPlanItem) -> Unit, onReschedule: (StudyPlanItem) -> Unit, onRegenerateTask: (StudyPlanItem) -> Unit, onAddReminder: () -> Unit, onDelete: (StudyPlanItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
        Text(task.topic, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(task.task, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        OptionItem(Icons.Default.CheckCircle, "Mark Complete", Color(0xFF4CAF50)) { onComplete(task) }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.EventRepeat, "Reschedule (Tomorrow)", MaterialTheme.colorScheme.primary) { onReschedule(task) }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.AutoAwesome, "Regenerate Task", MaterialTheme.colorScheme.secondary) { onRegenerateTask(task) }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.Notifications, "Add Reminder Offset", MaterialTheme.colorScheme.tertiary) { onAddReminder() }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.Delete, "Delete Task", MaterialTheme.colorScheme.error) { onDelete(task) }
    }
}

@Composable
fun AddCustomTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Custom Task") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") })
    OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") })
    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (mins)") }) } }, confirmButton = { Button(onClick = { onAdd(day, title, topic, duration.toIntOrNull() ?: 60) }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun DayHeader(day: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Violet400))
        Text(text = day, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Violet300, letterSpacing = 1.sp)
    }
}

@Composable
fun ProgressCard(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progressAnim"
    )
    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Surface700)
            .border(BorderStroke(1.dp, Brush.horizontalGradient(listOf(Violet400.copy(0.5f), Cyan400.copy(0.3f)))), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Text("Today's Progress", style = MaterialTheme.typography.labelLarge, color = OnSurface300)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = OnSurface100)
                Icon(Icons.Default.TrendingUp, null, tint = Violet300, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Surface600)) {
                Box(modifier = Modifier.fillMaxWidth(animatedProgress.coerceIn(0f,1f)).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Violet500, Cyan400))))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = OnSurface100)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = OnSurface300, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun TimeBadge(time: String) {
    Surface(color = Indigo500.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Indigo400.copy(0.25f))) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, null, modifier = Modifier.size(10.dp), tint = Indigo300)
            Spacer(Modifier.width(4.dp))
            Text(time, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Indigo200)
        }
    }
}

@Composable
fun SubjectInputSection(
    subject: String,
    daysToComplete: String,
    onSubjectChange: (String) -> Unit,
    onDaysChange: (String) -> Unit,
    onGenerate: () -> Unit,
    isLoading: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "subjectGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "subjectGlowAlpha"
    )

    val subjectList = subject.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val isReady = subject.isNotBlank() && daysToComplete.toIntOrNull()?.let { it > 0 } == true && !isLoading

    Column(
        modifier = Modifier.fillMaxSize().background(Surface900).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(120.dp).clip(CircleShape).background(Violet400.copy(alpha = glowAlpha * 0.2f)))
            Box(Modifier.size(80.dp).clip(CircleShape).background(Surface700).border(BorderStroke(1.dp, Violet400.copy(0.4f)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(40.dp), tint = Violet300)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Plan Your Study", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnSurface100)
        Spacer(Modifier.height(6.dp))
        Text("Enter subjects and your target completion window.", style = MaterialTheme.typography.bodyMedium, color = OnSurface300)
        Spacer(Modifier.height(32.dp))

        // Multi-subject field
        OutlinedTextField(
            value = subject,
            onValueChange = onSubjectChange,
            label = { Text("Subjects (e.g. Kotlin, DSA, DBMS)", color = OnSurface300) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = false,
            maxLines = 3,
            supportingText = {
                if (subjectList.size > 1) {
                    Text("${subjectList.size} subjects: ${subjectList.joinToString(", ")}", color = Cyan400, style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("Separate multiple subjects with commas", color = OnSurface300, style = MaterialTheme.typography.labelSmall)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet400,
                unfocusedBorderColor = Surface600,
                focusedTextColor = OnSurface100,
                unfocusedTextColor = OnSurface100,
                cursorColor = Violet300
            )
        )
        Spacer(Modifier.height(16.dp))

        // Days to complete field
        OutlinedTextField(
            value = daysToComplete,
            onValueChange = { if (it.length <= 3 && it.all(Char::isDigit) || it.isEmpty()) onDaysChange(it) },
            label = { Text("Days to complete", color = OnSurface300) },
            leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Amber400, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            supportingText = { Text("How many days until you finish all subjects?", color = OnSurface300, style = MaterialTheme.typography.labelSmall) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Amber400,
                unfocusedBorderColor = Surface600,
                focusedTextColor = OnSurface100,
                unfocusedTextColor = OnSurface100,
                cursorColor = Amber400
            )
        )
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isReady) Brush.horizontalGradient(listOf(Violet500, Cyan400)) else Brush.horizontalGradient(listOf(Surface600, Surface600)))
                .clickable(enabled = isReady) { onGenerate() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Get Topics →", style = MaterialTheme.typography.titleMedium, color = if (isReady) Color.White else OnSurface300, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlanPreviewSection(plan: List<StudyPlanItem>, onAccept: () -> Unit, onRegenerate: () -> Unit, isLoading: Boolean) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Review New Schedule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val grouped = plan.groupBy { it.day }
            grouped.forEach { (day, tasks) ->
                item { DayHeader(day) }
                items(tasks) { task -> PremiumTaskCard(task, onClick = {}) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRegenerate, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), enabled = !isLoading) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Regenerate")
            }
            Button(onClick = onAccept, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), enabled = !isLoading) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Accept & Save")
            }
        }
    }
}
