package com.example.studyplannerai.ui.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.focus.FocusSessionState
import com.example.studyplannerai.viewmodel.focus.FocusState
import com.example.studyplannerai.viewmodel.focus.FocusViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val motivationalQuotes = listOf(
    "Focus on the step in front of you, not the whole staircase.",
    "The secret of your future is hidden in your daily routine.",
    "Strive for progress, not perfection.",
    "Don't stop when you're tired. Stop when you're done.",
    "Small disciplines repeated with consistency lead to great achievements."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    focusViewModel: FocusViewModel,
    onNavigateBack: () -> Unit
) {
    val state by focusViewModel.sessionState.collectAsStateWithLifecycle()
    
    // Rotating quote
    var currentQuote by remember { mutableStateOf(motivationalQuotes.random()) }
    LaunchedEffect(state.state) {
        if (state.state == FocusState.Break) {
            currentQuote = "Take a deep breath. You earned this break."
        } else if (state.state == FocusState.Running) {
            while (true) {
                delay(60000) // Rotate quote every minute
                currentQuote = motivationalQuotes.random()
            }
        }
    }

    if (state.showDurationPicker) {
        ModalBottomSheet(
            onDismissRequest = { focusViewModel.dismissDurationPicker() },
            containerColor = Surface900
        ) {
            DurationPickerSheet(
                taskTitle = state.taskTitle,
                onStart = { duration ->
                    state.selectedTask?.let { task ->
                        focusViewModel.startSession(task, duration)
                    }
                }
            )
        }
    }

    if (state.showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { focusViewModel.dismissCompletionDialog() },
            containerColor = Surface800,
            titleContentColor = OnSurface100,
            textContentColor = OnSurface200,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Celebration, null, tint = Emerald400)
                    Text("Session Complete!", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Great job focusing for ${state.durationMinutes} minutes. What would you like to do next?") },
            confirmButton = {
                Button(
                    onClick = { focusViewModel.markTaskComplete() },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald400)
                ) {
                    Text("Mark Task Done")
                }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { focusViewModel.startAnotherSession() }
                    ) {
                        Text("Another Session")
                    }
                    TextButton(onClick = { focusViewModel.takeBreak() }) {
                        Text("Take a 5-min Break", color = Violet300)
                    }
                }
            }
        )
    }

    // Only render the full screen if active
    if (!state.isActive && state.state != FocusState.Completed) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    focusViewModel.stopSession()
                    onNavigateBack()
                },
                modifier = Modifier.background(Surface800, CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close Session", tint = OnSurface200)
            }
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface800,
                border = BorderStroke(1.dp, Surface700)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Amber400, modifier = Modifier.size(16.dp))
                    Text("Session ${state.sessionCount + 1}", style = MaterialTheme.typography.labelMedium, color = OnSurface200)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Task Info
            Text(
                text = state.taskTopic,
                style = MaterialTheme.typography.labelLarge,
                color = Violet300,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = state.taskTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface100,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Timer Ring
            TimerRing(state = state)

            Spacer(modifier = Modifier.height(64.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state.state) {
                    FocusState.Running -> {
                        FloatingActionButton(
                            onClick = { focusViewModel.pauseSession() },
                            containerColor = Amber400,
                            contentColor = Surface900,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(Icons.Default.Pause, "Pause", modifier = Modifier.size(36.dp))
                        }
                    }
                    FocusState.Paused -> {
                        FloatingActionButton(
                            onClick = { focusViewModel.resumeSession() },
                            containerColor = Emerald400,
                            contentColor = Surface900,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Resume", modifier = Modifier.size(36.dp))
                        }
                    }
                    FocusState.Break -> {
                        // Read-only during break, or can skip
                        OutlinedButton(onClick = { focusViewModel.stopSession() }) {
                            Text("End Break Early")
                        }
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Quote
            Text(
                text = "\"$currentQuote\"",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface300,
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun TimerRing(state: FocusSessionState) {
    val isBreak = state.state == FocusState.Break
    val totalSeconds = if (isBreak) 5 * 60 else state.durationMinutes * 60
    val currentSeconds = if (isBreak) state.breakSecondsLeft else state.secondsLeft
    
    val progress = if (totalSeconds > 0) 1f - (currentSeconds.toFloat() / totalSeconds.toFloat()) else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress"
    )

    val primaryColor = if (isBreak) Emerald400 else Violet400
    val trackColor = Surface700

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(280.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            
            // Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dot indicator
            val angleInDegrees = -90f + (360f * animatedProgress)
            val angleInRadians = angleInDegrees * PI / 180f
            val radius = (size.width - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            val dotX = center.x + radius * cos(angleInRadians).toFloat()
            val dotY = center.y + radius * sin(angleInRadians).toFloat()
            
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }

        // Time Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val minutes = currentSeconds / 60
            val seconds = currentSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
                color = OnSurface100,
                fontSize = 72.sp
            )
            Text(
                text = if (isBreak) "BREAK" else "FOCUS",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DurationPickerSheet(
    taskTitle: String,
    onStart: (Int) -> Unit
) {
    var customDuration by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Start Focus Session",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface100
        )
        Text(
            taskTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Violet300,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        val presetDurations = listOf(25, 45, 60, 90)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetDurations) { mins ->
                Surface(
                    onClick = { onStart(mins) },
                    shape = RoundedCornerShape(16.dp),
                    color = Surface800,
                    border = BorderStroke(1.dp, Surface600)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "$mins",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface100
                        )
                        Text(
                            "minutes",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurface300
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(Modifier.weight(1f), color = Surface700)
            Text(" OR CUSTOM ", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
            Divider(Modifier.weight(1f), color = Surface700)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customDuration,
                onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) customDuration = it },
                placeholder = { Text("Mins", color = OnSurface300) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet400,
                    unfocusedBorderColor = Surface600,
                    focusedTextColor = OnSurface100,
                    unfocusedTextColor = OnSurface100
                ),
                singleLine = true
            )
            Button(
                onClick = {
                    val mins = customDuration.toIntOrNull() ?: 0
                    if (mins in 1..999) {
                        onStart(mins)
                    }
                },
                enabled = customDuration.isNotEmpty() && (customDuration.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)
            ) {
                Text("Start")
            }
        }
    }
}
