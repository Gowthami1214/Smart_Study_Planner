package com.example.studyplannerai.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.PlannedStudyBlock
import com.example.studyplannerai.data.model.Task
import com.example.studyplannerai.ui.components.RationaleDialog
import com.example.studyplannerai.ui.components.StudyBlockCard
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.permission.PermissionViewModel
import com.example.studyplannerai.viewmodel.task.TaskViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sin

@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel,
    permissionViewModel: PermissionViewModel,
    innerPadding: PaddingValues,
    showWeeklyPlan: Boolean
) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionViewModel.onPermissionResult(isGranted, "Notification")
    }

    LaunchedEffect(permissionState.isPermissionRequested) {
        if (!permissionState.isPermissionRequested) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) permissionViewModel.showNotificationRationale()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmMgr.canScheduleExactAlarms()) permissionViewModel.showAlarmRationale()
            }
            permissionViewModel.setPermissionRequested()
        }
    }

    if (permissionState.showNotificationRationale) {
        RationaleDialog(
            onDismissRequest = { permissionViewModel.onNotificationRationaleDismissed() },
            onConfirm = {
                permissionViewModel.onNotificationRationaleDismissed()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            title = "Notifications Permission",
            text = "Enable notifications to receive timely reminders for your study tasks and deadlines."
        )
    }

    if (permissionState.showAlarmRationale) {
        RationaleDialog(
            onDismissRequest = { permissionViewModel.onAlarmRationaleDismissed() },
            onConfirm = {
                permissionViewModel.onAlarmRationaleDismissed()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            },
            title = "Exact Alarm Permission",
            text = "This app needs permission to schedule exact alarms so your study reminders arrive on time."
        )
    }

    AnimatedVisibility(
        visible = uiState.isLoading,
        enter = fadeIn(),
        exit  = fadeOut()
    ) {
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            PulsingLoader()
        }
    }

    if (!uiState.isLoading) {
        val completionPercent = uiState.completionPercent
        val today = LocalDate.now()

        val displayBlocks = if (uiState.studyBlocks.isEmpty()) {
            listOf(
                PlannedStudyBlock(
                    taskId = "1",
                    taskTitle = "Calculus Assignment",
                    subjectName = "Mathematics",
                    subjectColor = "#5C6BC0",
                    dateEpochDay = today.toEpochDay(),
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + 3600000,
                    plannedMinutes = 60,
                    suggestedDeadlineEpochDay = today.toEpochDay(),
                    isRescheduled = false,
                    status = "Pending"
                )
            )
        } else uiState.studyBlocks

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Surface900),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ─── Header Section ───
            item {
                HomeHeader(pendingTasks = uiState.pendingTasks)
            }

            // ─── Stats Row ───
            item {
                StatsRow(
                    completionPercent = completionPercent,
                    streakDays        = uiState.streakDays,
                    totalMinutes      = uiState.totalMinutesStudied
                )
            }

            // ─── Progress Card ───
            item {
                Spacer(Modifier.height(4.dp))
                AnimatedProgressCard(completionPercent = completionPercent)
                Spacer(Modifier.height(20.dp))
            }

            // ─── Today's Plan ───
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface100
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Violet400.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${displayBlocks.size} blocks",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Violet300
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            items(displayBlocks, key = { it.taskId }) { block ->
                AnimatedStudyBlockCard(block = block)
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─── Pulsing Loader ───────────────────────────────────────────
@Composable
private fun PulsingLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((64 * scale).dp)
                .clip(CircleShape)
                .background(Violet400.copy(alpha = alpha * 0.3f))
        )
        CircularProgressIndicator(
            color = Violet400,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
    }
}

// ─── Header ──────────────────────────────────────────────────
@Composable
private fun HomeHeader(pendingTasks: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Surface800, Surface900)
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        // Glow blob
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Violet400.copy(alpha = glowAlpha * 0.12f))
                .blur(80.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Emerald400)
                )
                Text(
                    "GOOD MORNING",
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald400,
                    letterSpacing = 2.sp
                )
            }
            Text(
                "Welcome back! 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface100
            )
            Text(
                if (pendingTasks == 0) "You're all caught up today!"
                else "You have $pendingTasks tasks to focus on today.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface200
            )
        }
    }
}

// ─── Stats Row ───────────────────────────────────────────────
@Composable
private fun StatsRow(
    completionPercent: Float,
    streakDays: Int,
    totalMinutes: Int
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatChip(
                icon    = Icons.Default.LocalFireDepartment,
                value   = "${streakDays}d",
                label   = "Streak",
                color   = Amber400
            )
        }
        item {
            StatChip(
                icon    = Icons.Default.Timer,
                value   = "${totalMinutes}m",
                label   = "Studied",
                color   = Cyan400
            )
        }
        item {
            StatChip(
                icon    = Icons.Default.CheckCircle,
                value   = "${(completionPercent * 100).toInt()}%",
                label   = "Done",
                color   = Emerald400
            )
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = Surface700,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleSmall, color = OnSurface100, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurface300)
            }
        }
    }
}

// ─── Animated Progress Card ───────────────────────────────────
@Composable
private fun AnimatedProgressCard(completionPercent: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue  = completionPercent,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label        = "progressAnim"
    )

    val gradientBrush = Brush.horizontalGradient(listOf(Violet500, Cyan400))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Surface700, Surface800),
                    start  = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end    = androidx.compose.ui.geometry.Offset(800f, 800f)
                )
            )
            .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(Violet400.copy(0.4f), Cyan400.copy(0.2f)))),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Progress", style = MaterialTheme.typography.labelLarge, color = OnSurface300)
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface100
                    )
                }
                GlowingIcon()
            }

            // Custom gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Surface600)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(gradientBrush)
                )
            }

            Text(
                text = when {
                    animatedProgress >= 1f -> "🎉 All done! Incredible focus today."
                    animatedProgress > 0.8f -> "Almost there! One last push."
                    animatedProgress > 0.5f -> "Great momentum! Keep it up."
                    animatedProgress > 0f   -> "Good start! Consistency is everything."
                    else                    -> "Your journey begins with the first task."
                },
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface300
            )
        }
    }
}

@Composable
private fun GlowingIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "glowIcon")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation   = tween(1500, easing = FastOutSlowInEasing),
            repeatMode  = RepeatMode.Reverse
        ),
        label = "glowIconAlpha"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Violet400.copy(alpha = glowAlpha * 0.3f))
        )
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Violet300,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ─── Animated Study Block Card ────────────────────────────────
@Composable
private fun AnimatedStudyBlockCard(block: PlannedStudyBlock) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(block.taskId) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(400)),
    ) {
        StudyBlockCard(block = block)
    }
}
