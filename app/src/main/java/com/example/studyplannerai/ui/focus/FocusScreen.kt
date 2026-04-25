package com.example.studyplannerai.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.planner.PlannerViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FocusScreen(
    plannerViewModel: PlannerViewModel,
    innerPadding: PaddingValues
) {
    val uiState by plannerViewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now().toString()
    val todayTasks = remember(uiState.studyPlan, uiState.history) {
        (uiState.studyPlan + uiState.history).filter { it.day == today }
    }
    
    val subjects = remember(todayTasks) {
        listOf("All") + todayTasks.map { it.planId.ifBlank { "General" } }.flatMap { it.split(" & ") }.map { it.trim() }.distinct()
    }
    var selectedSubject by remember { mutableStateOf("All") }

    val filteredTasks = remember(todayTasks, selectedSubject) {
        if (selectedSubject == "All") todayTasks
        else todayTasks.filter { 
            val taskSubjects = it.planId.ifBlank { "General" }.split(" & ").map { s -> s.trim() }
            taskSubjects.contains(selectedSubject)
        }
    }

    val allDoneToday = filteredTasks.isNotEmpty() && filteredTasks.all { it.isCompleted }

    val pomodoroState by plannerViewModel.pomodoroState.collectAsStateWithLifecycle()
    val pomodoroSecondsLeft by plannerViewModel.pomodoroSecondsLeft.collectAsStateWithLifecycle()
    val pomodoroBreakSecondsLeft by plannerViewModel.pomodoroBreakSecondsLeft.collectAsStateWithLifecycle()
    val activePomodoroTaskId by plannerViewModel.activePomodoroTaskId.collectAsStateWithLifecycle()

    var showConfetti by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<StudyPlanItem?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(allDoneToday) {
        if (allDoneToday) showConfetti = true
    }

    val context = LocalContext.current
    if (uiState.showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { plannerViewModel.dismissDndDialog() },
            title = { Text("Enable Do Not Disturb", color = OnSurface100) },
            text = { Text("To block notifications while you focus, the app needs permission to modify Do Not Disturb settings.", color = OnSurface300) },
            confirmButton = {
                TextButton(onClick = {
                    plannerViewModel.dismissDndDialog()
                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }) {
                    Text("Go to Settings", color = Violet400)
                }
            },
            dismissButton = {
                TextButton(onClick = { plannerViewModel.dismissDndDialog() }) {
                    Text("Cancel", color = OnSurface300)
                }
            },
            containerColor = Surface800
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Surface900).padding(innerPadding)) {
        if (todayTasks.isEmpty()) {
            FocusEmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Text("Today's Focus", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = OnSurface100)
                        Text("${filteredTasks.count { it.isCompleted }}/${filteredTasks.size} tasks done · ${LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = OnSurface300)
                    }
                }
                
                // Subject Filter Chips
                if (subjects.size > 2) { // Show filters if there is at least one specific subject (All + 1 subject = 2, so > 2 means multiple subjects)
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(subjects) { subj ->
                                val isSelected = subj == selectedSubject
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Violet400.copy(0.2f),
                                        selectedLabelColor = Violet300,
                                        containerColor = Surface700,
                                        labelColor = OnSurface300
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Surface600,
                                        selectedBorderColor = Violet400.copy(0.5f),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Progress bar
                item {
                    val pct = if (filteredTasks.isEmpty()) 0f else filteredTasks.count { it.isCompleted }.toFloat() / filteredTasks.size
                    val animPct by animateFloatAsState(pct, tween(600), label = "todayPct")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Surface600)) {
                            Box(Modifier.fillMaxWidth(animPct).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Violet500, Emerald400))))
                        }
                        Text("${(animPct * 100).toInt()}% complete", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                    }
                }
                items(filteredTasks) { task ->
                    FocusTaskCard(
                        task = task,
                        isSelected = selectedTask?.id == task.id,
                        onSelect = { selectedTask = if (selectedTask?.id == task.id) null else task },
                        onComplete = { plannerViewModel.toggleTaskCompletion(task) },
                        onReschedule = { 
                            plannerViewModel.rescheduleTask(task.id, LocalDate.now().plusDays(1).toString(), task.time_slot)
                            selectedTask = null
                        },
                        isActivePomodoro = activePomodoroTaskId == task.id,
                        pomodoroState = pomodoroState,
                        pomodoroSecondsLeft = pomodoroSecondsLeft,
                        pomodoroBreakSecondsLeft = pomodoroBreakSecondsLeft,
                        onStartPomodoro = { plannerViewModel.startPomodoro(task.id, 25) },
                        onPausePomodoro = { plannerViewModel.pausePomodoro() },
                        onResumePomodoro = { plannerViewModel.resumePomodoro() },
                        onResetPomodoro = { plannerViewModel.resetPomodoro(25) },
                        onRegenerateTask = {
                            plannerViewModel.regenerateSingleTask(task.id)
                            selectedTask = null
                        },
                        onAddReminder = {
                            selectedTask = task
                            showReminderDialog = true
                        },
                        onDelete = {
                            plannerViewModel.deleteSingleTask(task.id)
                            selectedTask = null
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
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

        // Confetti overlay
        AnimatedVisibility(visible = showConfetti, enter = fadeIn(), exit = fadeOut()) {
            ConfettiOverlay(onFinish = { showConfetti = false })
        }
    }
}

// ─── Focus Task Card with Pomodoro ─────────────────────────────
@Composable
fun FocusTaskCard(
    task: StudyPlanItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onComplete: () -> Unit,
    onReschedule: () -> Unit,
    isActivePomodoro: Boolean,
    pomodoroState: com.example.studyplannerai.viewmodel.planner.PomodoroState,
    pomodoroSecondsLeft: Int,
    pomodoroBreakSecondsLeft: Int,
    onStartPomodoro: () -> Unit,
    onPausePomodoro: () -> Unit,
    onResumePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onRegenerateTask: () -> Unit,
    onAddReminder: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (isSelected) 1.02f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "fCardScale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Surface700 else Surface800)
            .border(BorderStroke(1.dp, if (isSelected) Violet400.copy(0.5f) else Surface600), RoundedCornerShape(20.dp))
            .clickable { onSelect() }
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.topic,
                        style = MaterialTheme.typography.labelMedium,
                        color = Violet300,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = task.task,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) OnSurface300 else OnSurface100,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AccessTime, null, tint = OnSurface300, modifier = Modifier.size(12.dp))
                        Text("${task.duration_minutes} min · ${task.time_slot}", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                    }
                }
                // Complete check & Options
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (task.isCompleted) Emerald400.copy(0.2f) else Surface600)
                            .border(BorderStroke(1.5.dp, if (task.isCompleted) Emerald400 else Surface600.copy(2f)), CircleShape)
                            .clickable { onComplete() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) Icon(Icons.Default.Check, null, tint = Emerald400, modifier = Modifier.size(18.dp))
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Options", tint = OnSurface300)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Surface800)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark as Complete", color = OnSurface100) },
                                onClick = { showMenu = false; onComplete() },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Emerald400) }
                            )
                            DropdownMenuItem(
                                text = { Text("Reschedule (Tomorrow)", color = OnSurface100) },
                                onClick = { showMenu = false; onReschedule() },
                                leadingIcon = { Icon(Icons.Default.EventRepeat, null, tint = Violet300) }
                            )
                            DropdownMenuItem(
                                text = { Text("Regenerate Task", color = OnSurface100) },
                                onClick = { showMenu = false; onRegenerateTask() },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = Cyan400) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Reminder", color = OnSurface100) },
                                onClick = { showMenu = false; onAddReminder() },
                                leadingIcon = { Icon(Icons.Default.Notifications, null, tint = Amber400) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Task", color = Rose400) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Rose400) }
                            )
                        }
                    }
                }
            }

            // Pomodoro timer (expanded when selected)
            AnimatedVisibility(visible = isSelected && !task.isCompleted) {
                PomodoroTimer(
                    durationMinutes = 25, 
                    taskTitle = task.task,
                    isActive = isActivePomodoro,
                    state = pomodoroState,
                    secondsLeft = pomodoroSecondsLeft,
                    breakSecondsLeft = pomodoroBreakSecondsLeft,
                    onStart = onStartPomodoro,
                    onPause = onPausePomodoro,
                    onResume = onResumePomodoro,
                    onReset = onResetPomodoro
                )
            }
        }
    }
}

@Composable
fun PomodoroTimer(
    durationMinutes: Int = 25, 
    taskTitle: String,
    isActive: Boolean,
    state: com.example.studyplannerai.viewmodel.planner.PomodoroState,
    secondsLeft: Int,
    breakSecondsLeft: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    val totalSeconds = durationMinutes * 60
    
    // If this task is NOT the active one, we display it as Idle
    val displayState = if (isActive) state else com.example.studyplannerai.viewmodel.planner.PomodoroState.Idle
    val displaySecondsLeft = if (isActive) secondsLeft else totalSeconds
    val displayBreakSecondsLeft = if (isActive) breakSecondsLeft else 5 * 60

    val isBreak = displayState == com.example.studyplannerai.viewmodel.planner.PomodoroState.Break
    val displaySeconds = if (isBreak) displayBreakSecondsLeft else displaySecondsLeft
    val total = if (isBreak) 5 * 60 else totalSeconds
    val fraction = displaySeconds.toFloat() / total
    val animFraction by animateFloatAsState(fraction, tween(800), label = "pomodoro")

    val violet = Violet400; val cyan = Cyan400; val emerald = Emerald400

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Surface900).border(BorderStroke(1.dp, if (isBreak) Emerald400.copy(0.3f) else Violet400.copy(0.25f)), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isBreak) "☕ Break Time" else "🍅 Pomodoro Timer",
                style = MaterialTheme.typography.labelLarge,
                color = if (isBreak) Emerald400 else Violet300,
                fontWeight = FontWeight.Bold
            )

            // Arc timer display
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2
                    val rect = androidx.compose.ui.geometry.Rect(inset, inset, size.width - inset, size.height - inset)
                    drawArc(color = Surface600, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(
                        brush = if (isBreak) Brush.linearGradient(listOf(emerald, cyan)) else Brush.linearGradient(listOf(violet, cyan)),
                        startAngle = -90f, sweepAngle = 360f * animFraction, useCenter = false,
                        topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val m = displaySeconds / 60; val s = displaySeconds % 60
                    Text("%02d:%02d".format(m, s), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = OnSurface100, fontSize = 18.sp)
                    Text(if (isBreak) "break" else "focus", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Reset
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, null, tint = OnSurface300, modifier = Modifier.size(20.dp))
                }
                // Start/Pause
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(if (isBreak) listOf(Emerald400, Cyan400) else listOf(Violet500, Cyan400)))
                        .clickable {
                            when (displayState) {
                                com.example.studyplannerai.viewmodel.planner.PomodoroState.Idle -> onStart()
                                com.example.studyplannerai.viewmodel.planner.PomodoroState.Paused -> onResume()
                                com.example.studyplannerai.viewmodel.planner.PomodoroState.Running -> onPause()
                                com.example.studyplannerai.viewmodel.planner.PomodoroState.Break -> {}
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (displayState) {
                            com.example.studyplannerai.viewmodel.planner.PomodoroState.Idle -> "Start"
                            com.example.studyplannerai.viewmodel.planner.PomodoroState.Running -> "Pause"
                            com.example.studyplannerai.viewmodel.planner.PomodoroState.Paused -> "Resume"
                            com.example.studyplannerai.viewmodel.planner.PomodoroState.Break -> "In Break"
                        },
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ─── Confetti Overlay ────────────────────────────────────────────
private data class Particle(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, val size: Float, val rotation: Float)

@Composable
fun ConfettiOverlay(onFinish: () -> Unit) {
    val colors = listOf(Violet400, Cyan400, Emerald400, Amber400, Rose400, Violet300, Cyan300)
    val particles = remember {
        List(80) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 0.008f,
                vy = Random.nextFloat() * 0.004f + 0.003f,
                color = colors.random(),
                size = Random.nextFloat() * 14f + 6f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (progress < 1f) {
            delay(16)
            progress = ((System.currentTimeMillis() - start) / 2500f).coerceAtMost(1f)
        }
        onFinish()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val cx = (p.x + p.vx * progress * 200) * size.width
            val cy = (p.y + p.vy * progress * 200) * size.height
            val alpha = if (progress > 0.6f) (1f - (progress - 0.6f) / 0.4f).coerceIn(0f, 1f) else 1f
            drawCircle(color = p.color.copy(alpha = alpha), radius = p.size, center = Offset(cx, cy))
        }
    }

    // All done banner
    if (progress < 0.5f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Violet500.copy(0.95f), Cyan400.copy(0.9f))))
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎉", fontSize = 48.sp)
                    Text("All Done Today!", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
                    Text("Amazing work! Keep the streak going.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                }
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────
@Composable
private fun FocusEmptyState() {
    val inf = rememberInfiniteTransition(label = "focusEmpty")
    val pulse by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size((100 * pulse).dp).clip(CircleShape).background(Cyan400.copy(0.1f)))
            Box(Modifier.size(72.dp).clip(CircleShape).background(Surface700).border(BorderStroke(1.dp, Cyan400.copy(0.4f)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Today, null, modifier = Modifier.size(36.dp), tint = Cyan300)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("No Tasks for Today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnSurface100)
        Spacer(Modifier.height(8.dp))
        Text("Create a plan in the Planner tab to get started!", style = MaterialTheme.typography.bodyMedium, color = OnSurface300)
    }
}
