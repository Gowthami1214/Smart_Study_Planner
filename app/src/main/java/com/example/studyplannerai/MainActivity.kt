package com.example.studyplannerai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyplannerai.reminder.NotificationHelper
import com.example.studyplannerai.ui.auth.AuthScreen
import com.example.studyplannerai.ui.navigation.AppShell
import com.example.studyplannerai.ui.theme.StudyPlannerAiTheme
import com.example.studyplannerai.viewmodel.auth.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            StudyPlannerAiTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    RequestNotificationPermission()
    val authViewModel: AuthViewModel = viewModel()

    if (authViewModel.isLoggedIn.value) {
        AppShell(authViewModel = authViewModel)
    } else {
        AuthScreen(
            viewModel = authViewModel,
            onNavigateHome = {}
        )
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (!context.hasNotificationPermission()) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun Context.hasNotificationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
