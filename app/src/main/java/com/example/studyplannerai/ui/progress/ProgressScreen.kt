package com.example.studyplannerai.ui.progress

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.progress.ProgressViewModel

@Composable
fun ProgressScreen(viewModel: ProgressViewModel, innerPadding: PaddingValues) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Surface900).padding(innerPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ProgressHeader(uiState.streakCount) }
        item { GradientProgressCard(daily = uiState.dailyProgress, weekly = uiState.weeklyProgress) }
        item { Text("Your Stats", style = MaterialTheme.typography.titleMedium, color = OnSurface100, fontWeight = FontWeight.Bold) }
        item { StatsGrid(completed = uiState.completedTasks, total = uiState.totalTasks, hours = uiState.hoursStudied, topics = uiState.topicsCovered) }
        item { WeeklyBarChart(weeklyData = uiState.weeklyTasksPerDay) }
        item { MotivationCard(completedTasks = uiState.completedTasks, totalTasks = uiState.totalTasks) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ProgressHeader(streak: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your Growth", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnSurface100)
            Text("Keep the momentum going!", style = MaterialTheme.typography.bodySmall, color = OnSurface300)
        }
        Surface(shape = RoundedCornerShape(16.dp), color = Amber400.copy(0.12f), border = BorderStroke(1.dp, Amber400.copy(0.35f))) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = Amber400, modifier = Modifier.size(16.dp))
                Text("$streak day streak", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Amber400)
            }
        }
    }
}

@Composable
private fun GradientProgressCard(daily: Float, weekly: Float) {
    val animDaily by animateFloatAsState(targetValue = daily, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "daily")
    val animWeekly by animateFloatAsState(targetValue = weekly, animationSpec = tween(1400, easing = FastOutSlowInEasing), label = "weekly")
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Surface800)
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Violet400.copy(0.4f), Cyan400.copy(0.2f)))), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                GradientArc(progress = animDaily, size = 110.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(animDaily * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = OnSurface100)
                    Text("Today", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Weekly Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface100)
                Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Surface600)) {
                    Box(modifier = Modifier.fillMaxWidth(animWeekly.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Violet500, Cyan400))))
                }
                Text("${(animWeekly * 100).toInt()}% of weekly tasks done", style = MaterialTheme.typography.bodySmall, color = OnSurface300)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Surface600))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column { Text("${(animDaily * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Violet300); Text("Daily", style = MaterialTheme.typography.labelSmall, color = OnSurface300) }
                    Column { Text("${(animWeekly * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Cyan300); Text("Weekly", style = MaterialTheme.typography.labelSmall, color = OnSurface300) }
                }
            }
        }
    }
}

@Composable
private fun GradientArc(progress: Float, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        val rect = androidx.compose.ui.geometry.Rect(inset, inset, this.size.width - inset, this.size.height - inset)
        drawArc(color = Surface600, startAngle = -210f, sweepAngle = 240f, useCenter = false, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke, cap = StrokeCap.Round))
        if (progress > 0f) drawArc(brush = Brush.linearGradient(listOf(Violet400, Cyan400)), startAngle = -210f, sweepAngle = 240f * progress.coerceIn(0f, 1f), useCenter = false, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun StatsGrid(completed: Int, total: Int, hours: Float, topics: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumStatCard(Modifier.weight(1f), "Tasks Done", "$completed/$total", Icons.Default.CheckCircle, Emerald400)
            PremiumStatCard(Modifier.weight(1f), "Hours", "${hours.toInt()}h", Icons.Default.Timer, Cyan400)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumStatCard(Modifier.weight(1f), "Topics", "$topics", Icons.Default.MenuBook, Violet400)
            PremiumStatCard(Modifier.weight(1f), "Efficiency", "High", Icons.Default.TrendingUp, Amber400)
        }
    }
}

@Composable
private fun PremiumStatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Box(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Surface800).border(BorderStroke(1.dp, color.copy(0.2f)), RoundedCornerShape(18.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = OnSurface100)
            Text(title, style = MaterialTheme.typography.labelSmall, color = OnSurface300)
        }
    }
}

@Composable
private fun MotivationCard(completedTasks: Int, totalTasks: Int) {
    val percent = if (totalTasks == 0) 0 else (completedTasks * 100 / totalTasks)
    val (emoji, message) = when {
        percent >= 90 -> "🏆" to "Outstanding! You're crushing it!"
        percent >= 70 -> "🔥" to "Great momentum — keep it up!"
        percent >= 50 -> "💪" to "Past halfway — you've got this!"
        percent > 0   -> "🌱" to "Good start! Consistency is the key."
        else           -> "🚀" to "Ready to begin? Every task counts!"
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Violet500.copy(0.15f), Cyan400.copy(0.08f))))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Violet400.copy(0.3f), Cyan400.copy(0.2f)))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(emoji, fontSize = 36.sp)
            Column { Text("Motivation", style = MaterialTheme.typography.labelMedium, color = OnSurface300); Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface100) }
        }
    }
}

// ─── Weekly Bar Chart ─────────────────────────────────────────────
@Composable
fun WeeklyBarChart(weeklyData: List<Pair<String, Int>>) {
    if (weeklyData.isEmpty()) return
    val maxCount = weeklyData.maxOf { it.second }.coerceAtLeast(1)
    val violet = Violet400
    val cyan = Cyan400
    val track = Surface600

    // Animate bars growing from 0 to their target height
    val animatedFractions = weeklyData.map { (_, count) ->
        animateFloatAsState(
            targetValue = count.toFloat() / maxCount,
            animationSpec = tween(900, easing = FastOutSlowInEasing),
            label = "bar"
        ).value
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Surface800)
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Violet400.copy(0.3f), Cyan400.copy(0.15f)))), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("This Week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface100)
                Text("Tasks completed", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
            }
            // Chart
            Canvas(
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                val barCount = weeklyData.size
                val spacing = size.width / barCount
                val barWidth = spacing * 0.5f
                val maxBarHeight = size.height * 0.85f

                weeklyData.forEachIndexed { i, (_, count) ->
                    val fraction = animatedFractions[i]
                    val barHeight = maxBarHeight * fraction
                    val x = i * spacing + spacing / 2 - barWidth / 2
                    val y = size.height - barHeight

                    // Track bar (full height, dark)
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(x, size.height - maxBarHeight),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                    // Value bar (gradient)
                    if (count > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(cyan, violet),
                                startY = y, endY = size.height
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2)
                        )
                    }
                }
            }
            // Day labels row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weeklyData.forEach { (day, count) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count > 0) Cyan300 else OnSurface300,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(day, style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                    }
                }
            }
        }
    }
}
