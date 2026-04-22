package com.example.studyplannerai.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyplannerai.ui.home.HomeScreen
import com.example.studyplannerai.ui.planner.PlannerScreen
import com.example.studyplannerai.ui.profile.ProfileScreen
import com.example.studyplannerai.ui.settings.SettingsScreen
import com.example.studyplannerai.viewmodel.auth.AuthViewModel
import com.example.studyplannerai.viewmodel.settings.SettingsViewModel
import com.example.studyplannerai.viewmodel.task.TaskViewModel

private enum class AppTab(val label: String) {
    Home("Home"),
    Planner("Planner"),
    Profile("Profile"),
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current.applicationContext
    val taskViewModel: TaskViewModel = viewModel(
        key = "tasks-${authViewModel.currentUserId.value ?: "guest"}",
        factory = TaskViewModel.Factory(context)
    )
    val settingsViewModel: SettingsViewModel = viewModel()
    val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }

    LaunchedEffect(taskState.message, taskState.errorMessage, settingsState.message) {
        taskState.message?.let {
            snackbarHostState.showSnackbar(it)
            taskViewModel.clearMessage()
        }
        taskState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            taskViewModel.clearMessage()
        }
        settingsState.message?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Smart Study Planner") },
                actions = {
                    IconButton(onClick = { authViewModel.logOut() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AppTab.Home -> Icons.Filled.CalendarToday
                                    AppTab.Planner -> Icons.Filled.ListAlt
                                    AppTab.Profile -> Icons.Filled.Person
                                    AppTab.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            AppTab.Home -> HomeScreen(
                taskViewModel = taskViewModel,
                innerPadding = paddingValues,
                showWeeklyPlan = settingsState.settings.weeklyViewEnabled
            )
            AppTab.Planner -> PlannerScreen(
                taskViewModel = taskViewModel,
                innerPadding = paddingValues
            )
            AppTab.Profile -> ProfileScreen(
                authViewModel = authViewModel,
                taskViewModel = taskViewModel,
                innerPadding = paddingValues
            )
            AppTab.Settings -> SettingsScreen(settingsViewModel = settingsViewModel, innerPadding = paddingValues)
        }
    }
}
