package com.example.studyplannerai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyplannerai.data.model.PlannedStudyBlock
import com.example.studyplannerai.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StudyBlockCard(
    block: PlannedStudyBlock,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
    val startTimeStr = timeFormatter.format(Instant.ofEpochMilli(block.startTime))
    val endTimeStr   = timeFormatter.format(Instant.ofEpochMilli(block.endTime))

    val isCompleted  = block.status == "Completed"
    val isInProgress = block.status == "InProgress"

    // Parse subject color safely
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(block.subjectColor))
    } catch (e: Exception) { Violet400 }

    val statusColor = when {
        isCompleted  -> Emerald400
        isInProgress -> Amber400
        else          -> OnSurface300
    }
    val statusBg = when {
        isCompleted  -> Emerald400.copy(0.12f)
        isInProgress -> Amber400.copy(0.12f)
        else          -> Surface600
    }

    // Press scale animation
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface800)
            .border(BorderStroke(1.dp, subjectColor.copy(alpha = 0.25f)), RoundedCornerShape(20.dp))
            .clickable { pressed = !pressed }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
            // Left color accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(subjectColor, subjectColor.copy(0.4f)))
                    )
            )

            Column(
                modifier = Modifier.padding(16.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subject label row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(subjectColor)
                        )
                        Text(
                            text = block.subjectName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor
                        )
                    }
                    // Status pill
                    Surface(shape = RoundedCornerShape(8.dp), color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(0.3f))) {
                        Text(
                            text = block.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Task title
                Text(
                    text = block.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) OnSurface300 else OnSurface100
                )

                // Time row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = OnSurface300)
                    Text("$startTimeStr – $endTimeStr", style = MaterialTheme.typography.bodySmall, color = OnSurface300)
                    Spacer(Modifier.weight(1f))
                    // Duration chip
                    val mins = ((block.endTime - block.startTime) / 60000).toInt()
                    Surface(shape = RoundedCornerShape(8.dp), color = Indigo500.copy(0.1f), border = BorderStroke(1.dp, Indigo400.copy(0.25f))) {
                        Text("${mins}m", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = Indigo200, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
