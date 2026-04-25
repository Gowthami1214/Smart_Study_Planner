package com.example.studyplannerai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── DARK color scheme (indigo/violet) ────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Violet400,
    onPrimary            = Color.White,
    primaryContainer     = Surface700,
    onPrimaryContainer   = Violet300,

    secondary            = Cyan400,
    onSecondary          = Surface900,
    secondaryContainer   = Surface700,
    onSecondaryContainer = Cyan300,

    tertiary             = Emerald400,
    onTertiary           = Surface900,
    tertiaryContainer    = Surface700,
    onTertiaryContainer  = Emerald300,

    background           = Surface900,
    onBackground         = OnSurface100,

    surface              = Surface800,
    onSurface            = OnSurface100,
    surfaceVariant       = Surface700,
    onSurfaceVariant     = OnSurface200,

    outline              = Surface600,
    error                = Rose400,
    onError              = Color.White,
)

// ─── LIGHT color scheme (soft indigo/teal on white) ───────────
private val LightColorScheme = lightColorScheme(
    primary              = Violet500,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFEDE9FE),  // very light violet
    onPrimaryContainer   = Violet500,

    secondary            = Color(0xFF0E7490),  // deep teal
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0E7490),

    tertiary             = Color(0xFF059669),  // emerald
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFD1FAE5),
    onTertiaryContainer  = Color(0xFF059669),

    background           = Color(0xFFF8F7FF),
    onBackground         = Color(0xFF1A1033),

    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A1033),
    surfaceVariant       = Color(0xFFF0EDFF),
    onSurfaceVariant     = Color(0xFF4B4569),

    outline              = Color(0xFFCDC8E8),
    error                = Color(0xFFDC2626),
    onError              = Color.White,
)

@Composable
fun StudyPlannerAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) Surface900.toArgb() else Color(0xFFF8F7FF).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}