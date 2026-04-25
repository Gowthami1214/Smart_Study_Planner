package com.example.studyplannerai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppDesign {
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Violet500, Color(0xFFA855F7))
    )

    val SecondaryGradient = Brush.linearGradient(
        colors = listOf(Indigo500, Cyan400)
    )

    val CardShape = RoundedCornerShape(20.dp)
    val ButtonShape = RoundedCornerShape(12.dp)
}
