package com.example.studyplannerai.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.ui.chat.AiChatFab
import com.example.studyplannerai.ui.focus.FocusScreen
import com.example.studyplannerai.ui.planner.PlannerScreen
import com.example.studyplannerai.ui.profile.ProfileScreen
import com.example.studyplannerai.ui.progress.ProgressScreen
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.ui.focus.FocusSessionScreen
import com.example.studyplannerai.viewmodel.auth.AuthViewModel
import com.example.studyplannerai.viewmodel.focus.FocusViewModel
import com.example.studyplannerai.viewmodel.permission.PermissionViewModel
import com.example.studyplannerai.viewmodel.planner.PlannerViewModel
import com.example.studyplannerai.viewmodel.progress.ProgressViewModel
import com.example.studyplannerai.viewmodel.settings.SettingsViewModel
import com.example.studyplannerai.viewmodel.task.TaskViewModel
import kotlinx.coroutines.launch

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home("Home", Icons.Filled.CalendarToday, Icons.Filled.CalendarToday),
    Focus("Focus", Icons.Filled.CenterFocusStrong, Icons.Filled.CenterFocusStrong),
    Progress("Progress", Icons.Filled.TrendingUp, Icons.Filled.TrendingUp),
    Profile("Profile", Icons.Filled.Person, Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val plannerViewModel: PlannerViewModel = hiltViewModel()
    val focusViewModel: FocusViewModel = hiltViewModel()
    val progressViewModel: ProgressViewModel = hiltViewModel()
    val taskViewModel: TaskViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val permissionViewModel: PermissionViewModel = hiltViewModel()

    val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by permissionViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(taskState.message, taskState.errorMessage, settingsState.message) {
        taskState.message?.let { snackbarHostState.showSnackbar(it); taskViewModel.clearMessage() }
        taskState.errorMessage?.let { snackbarHostState.showSnackbar(it); taskViewModel.clearMessage() }
        settingsState.message?.let { snackbarHostState.showSnackbar(it); settingsViewModel.clearMessage() }
        permissionState.snackbarMessage?.let { snackbarHostState.showSnackbar(it); permissionViewModel.clearSnackbarMessage() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Surface900,
        topBar = {
            // Premium gradient top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Surface800, Surface900))
                    )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                selectedTab.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface100
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { authViewModel.logOut() }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Rose400.copy(0.1f))
                                    .border(BorderStroke(1.dp, Rose400.copy(0.3f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Rose400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Surface700,
                    contentColor = OnSurface100,
                    actionColor = Violet300,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(BorderStroke(1.dp, Brush.horizontalGradient(listOf(Violet400.copy(0.4f), Cyan400.copy(0.3f)))), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        bottomBar = {
            PremiumNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        floatingActionButton = {
            AiChatFab(
                modifier = Modifier.padding(bottom = 4.dp),
                onSendMessage = { msg, _ ->
                    var reply = ""
                    val job = coroutineScope.launch {
                        reply = when (val r = plannerViewModel.chatWithAssistant(msg)) {
                            is com.example.studyplannerai.core.util.Resource.Success -> r.data ?: "No response"
                            is com.example.studyplannerai.core.util.Resource.Error -> r.message ?: "Error"
                            else -> "Please try again."
                        }
                    }
                    job.join()
                    reply
                }
            )
        }
    ) { paddingValues ->
        val tabIndex = AppTab.entries.indexOf(selectedTab)
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val forward = AppTab.entries.indexOf(targetState) > AppTab.entries.indexOf(initialState)
                (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(300)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
            },
            label = "tabTransition"
        ) { tab ->
            when (tab) {
                AppTab.Home -> PlannerScreen(plannerViewModel = plannerViewModel, innerPadding = paddingValues)
                AppTab.Focus -> FocusScreen(plannerViewModel = plannerViewModel, innerPadding = paddingValues)
                AppTab.Progress -> ProgressScreen(viewModel = progressViewModel, innerPadding = paddingValues)
                AppTab.Profile -> ProfileScreen(authViewModel = authViewModel, taskViewModel = taskViewModel, innerPadding = paddingValues)
            }
        }
    }

    // Focus Session Overlay
    val focusState by focusViewModel.sessionState.collectAsStateWithLifecycle()
    if (focusState.isActive || focusState.showCompletionDialog || focusState.showDurationPicker) {
        FocusSessionScreen(focusViewModel = focusViewModel, onNavigateBack = {})
    }
}

@Composable
private fun PremiumNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Surface900, Surface800))
            )
            .border(BorderStroke(1.dp, Brush.horizontalGradient(listOf(Violet400.copy(0.1f), Cyan400.copy(0.1f), Violet400.copy(0.1f)))), RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { tab ->
                NavBarItem(
                    tab = tab,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(tab: AppTab, selected: Boolean, onClick: () -> Unit) {
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 40.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicatorWidth"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Violet300 else OnSurface300,
        label = "iconColor"
    )
    val bgAlpha by animateColorAsState(
        targetValue = if (selected) Violet400.copy(0.12f) else Color.Transparent,
        label = "bgAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgAlpha)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Active dot indicator
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Brush.horizontalGradient(listOf(Violet400, Cyan400))
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                )
        )
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}
