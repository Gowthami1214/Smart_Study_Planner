package com.example.studyplannerai.ui.profile

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.auth.AuthViewModel
import com.example.studyplannerai.viewmodel.task.TaskViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    taskViewModel: TaskViewModel,
    innerPadding: PaddingValues
) {
    val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by authViewModel.profileState.collectAsStateWithLifecycle()
    val avatarInitial = profileState.userName.firstOrNull()?.uppercase()
        ?: profileState.userEmail.firstOrNull()?.uppercase() ?: "U"

    // Refresh profile every time this screen is shown
    LaunchedEffect(Unit) { authViewModel.refreshProfile() }

    var showNotifDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "profileGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "profileGlowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ─── Hero header ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Surface800, Surface900)))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow behind avatar
            Box(
                modifier = Modifier
                    .size((120 * glowAlpha).dp)
                    .clip(CircleShape)
                    .background(Violet400.copy(alpha = glowAlpha * 0.2f))
                    .align(Alignment.Center)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Violet500, Cyan400)))
                        .border(BorderStroke(3.dp, Brush.linearGradient(listOf(Violet300, Cyan300))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarInitial,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    profileState.userName.ifBlank { "Study Planner User" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface100
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profileState.userEmail.ifBlank { "Not available" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface300
                )
                Spacer(Modifier.height(12.dp))
                // Premium badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Violet400.copy(0.15f),
                    border = BorderStroke(1.dp, Violet400.copy(0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Violet300, modifier = Modifier.size(14.dp))
                        Text("AI Study Planner Pro", style = MaterialTheme.typography.labelMedium, color = Violet300, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── Stats Grid ─────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Study Summary", style = MaterialTheme.typography.titleMedium, color = OnSurface100, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Subjects", value = "${taskState.subjectCount}", icon = Icons.Default.MenuBook, color = Violet400)
                StatCard(modifier = Modifier.weight(1f), label = "Total Tasks", value = "${taskState.totalTasks}", icon = Icons.Default.ListAlt, color = Cyan400)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Completed", value = "${taskState.completedTasks}", icon = Icons.Default.CheckCircle, color = Emerald400)
                StatCard(modifier = Modifier.weight(1f), label = "Pending", value = "${taskState.pendingTasks}", icon = Icons.Default.PendingActions, color = Amber400)
            }
            Spacer(Modifier.height(12.dp))
            // Completion bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface700)
                    .border(BorderStroke(1.dp, Surface600), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Completion Rate", style = MaterialTheme.typography.labelLarge, color = OnSurface200)
                        Text(
                            "${(taskState.completionPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Emerald400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val animPct by animateFloatAsState(
                        targetValue = taskState.completionPercent,
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        label = "completionAnim"
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Surface600)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animPct.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(Violet500, Emerald400)))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── XP / Level ─────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val xp = taskState.completedTasks * 15
            val (level, levelLabel, nextXp) = when {
                xp >= 700 -> Triple(4, "🏆 Master", 1000)
                xp >= 300 -> Triple(3, "🎓 Expert", 700)
                xp >= 100 -> Triple(2, "📚 Scholar", 300)
                else       -> Triple(1, "🌱 Beginner", 100)
            }
            val xpInLevel = xp - when(level) { 4 -> 700; 3 -> 300; 2 -> 100; else -> 0 }
            val xpRange  = nextXp - when(level) { 4 -> 700; 3 -> 300; 2 -> 100; else -> 0 }
            val xpPct    = (xpInLevel.toFloat() / xpRange).coerceIn(0f, 1f)
            val animXp by animateFloatAsState(xpPct, tween(1000, easing = FastOutSlowInEasing), label = "xp")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your Level", style = MaterialTheme.typography.titleMedium, color = OnSurface100, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(20.dp), color = Amber400.copy(0.12f), border = BorderStroke(1.dp, Amber400.copy(0.35f))) {
                    Text(levelLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = Amber400, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface700).border(BorderStroke(1.dp, Surface600), RoundedCornerShape(16.dp)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$xp XP total", style = MaterialTheme.typography.labelSmall, color = OnSurface200)
                        Text("$nextXp XP for next level", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                    }
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Surface600)) {
                        Box(Modifier.fillMaxWidth(animXp).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Amber400, Rose400))))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── Achievements ────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Achievements", style = MaterialTheme.typography.titleMedium, color = OnSurface100, fontWeight = FontWeight.Bold)
            val achievements = listOf(
                Triple("🚀", "First Plan Created", taskState.totalTasks > 0),
                Triple("🔥", "7-Day Streak", taskState.streakDays >= 7),
                Triple("✅", "10 Tasks Done", taskState.completedTasks >= 10),
                Triple("⚡", "Speed Learner", taskState.completedTasks >= 5),
                Triple("🌙", "Night Owl", taskState.totalTasks > 0),
                Triple("🏅", "Plan Master", taskState.subjectCount >= 3)
            )
            val rows = achievements.chunked(3)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (emoji, label, unlocked) ->
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (unlocked) Violet400.copy(0.12f) else Surface700)
                                .border(BorderStroke(1.dp, if (unlocked) Violet400.copy(0.35f) else Surface600), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (unlocked) emoji else "🔒", fontSize = 24.sp)
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (unlocked) OnSurface100 else OnSurface300, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ─── Menu items ─────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium, color = OnSurface100, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
        ProfileMenuItem(icon = Icons.Default.Notifications, label = "Notification Settings", color = Cyan400, onClick = { showNotifDialog = true })
            ProfileMenuItem(icon = Icons.Default.Palette, label = "App Appearance", color = Violet400, onClick = { showThemeDialog = true })
            ProfileMenuItem(icon = Icons.Default.Info, label = "About the App", color = Emerald400, onClick = { showAboutDialog = true })
        }

        Spacer(Modifier.height(24.dp))

        // ─── Logout button ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Rose400.copy(0.08f))
                .border(BorderStroke(1.dp, Rose400.copy(0.3f)), RoundedCornerShape(18.dp))
                .clickable { authViewModel.logOut() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Logout, null, tint = Rose400, modifier = Modifier.size(20.dp))
                Text("Sign Out", style = MaterialTheme.typography.titleMedium, color = Rose400, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // ─── Notification Settings Dialog ─────────────────────────────
    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            containerColor = Surface800,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Notification Settings", style = MaterialTheme.typography.titleLarge, color = OnSurface100) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Study reminders are managed per-task. Long-press any task in your planner and choose 'Set Reminder' to configure its notification time.", style = MaterialTheme.typography.bodyMedium, color = OnSurface200)
                    Surface(shape = RoundedCornerShape(12.dp), color = Cyan400.copy(0.08f), border = BorderStroke(1.dp, Cyan400.copy(0.25f)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                            Text("Make sure notification permission is granted in device settings.", style = MaterialTheme.typography.labelSmall, color = Cyan400)
                        }
                    }
                }
            },
            confirmButton = {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Cyan400, Violet400))).clickable { showNotifDialog = false }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── App Appearance Dialog ─────────────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = Surface800,
            shape = RoundedCornerShape(24.dp),
            title = { Text("App Appearance", style = MaterialTheme.typography.titleLarge, color = OnSurface100) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("The app follows your system theme setting.", style = MaterialTheme.typography.bodyMedium, color = OnSurface200)
                    listOf(
                        Triple(Icons.Default.DarkMode, "Dark Mode", "Rich deep indigo palette — easy on the eyes."),
                        Triple(Icons.Default.LightMode, "Light Mode", "Soft violet & teal on a clean white background.")
                    ).forEach { (icon, title, desc) ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Surface700, border = BorderStroke(1.dp, Surface600), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(icon, null, tint = Violet300, modifier = Modifier.size(20.dp))
                                Column { Text(title, style = MaterialTheme.typography.labelLarge, color = OnSurface100, fontWeight = FontWeight.Bold); Text(desc, style = MaterialTheme.typography.labelSmall, color = OnSurface300) }
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Amber400.copy(0.08f), border = BorderStroke(1.dp, Amber400.copy(0.25f)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = Amber400, modifier = Modifier.size(14.dp))
                            Text("Change your system theme in device Settings > Display.", style = MaterialTheme.typography.labelSmall, color = Amber400)
                        }
                    }
                }
            },
            confirmButton = {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Violet500, Cyan400))).clickable { showThemeDialog = false }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── About Dialog ──────────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Surface800,
            shape = RoundedCornerShape(24.dp),
            title = { Text("About the App", style = MaterialTheme.typography.titleLarge, color = OnSurface100) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet500, Cyan400))), contentAlignment = Alignment.Center) {
                            Text("✦", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        Column { Text("AI Smart Study Planner", style = MaterialTheme.typography.titleMedium, color = OnSurface100, fontWeight = FontWeight.Bold); Text("Version 1.0.0", style = MaterialTheme.typography.labelSmall, color = OnSurface300) }
                    }
                    Text("An AI-powered study planning app that generates personalized schedules, tracks your progress, and keeps you on track with smart reminders.", style = MaterialTheme.typography.bodySmall, color = OnSurface200)
                    Text("Built with Kotlin · Jetpack Compose · Firebase · Groq AI", style = MaterialTheme.typography.labelSmall, color = OnSurface300)
                }
            },
            confirmButton = {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Violet500, Cyan400))).clickable { showAboutDialog = false }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface700)
            .border(BorderStroke(1.dp, color.copy(0.2f)), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = OnSurface100)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurface300)
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface700)
            .border(BorderStroke(1.dp, Surface600), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurface100, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = OnSurface300, modifier = Modifier.size(18.dp))
        }
    }
}
