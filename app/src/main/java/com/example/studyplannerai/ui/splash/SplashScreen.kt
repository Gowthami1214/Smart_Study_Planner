package com.example.studyplannerai.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplannerai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // --- Animations ---
    val logoScale   = remember { Animatable(0.4f) }
    val logoAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val dotAlpha    = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Outer glow ring pulse
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ringAlpha"
    )

    // Loading dots bounce
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(500, 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(500, 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot3"
    )

    // Gradient sweep for shimmer bar
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    LaunchedEffect(true) {
        launch { logoAlpha.animateTo(1f, tween(700)) }
        launch { logoScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        delay(500)
        textAlpha.animateTo(1f, tween(500))
        delay(300)
        dotAlpha.animateTo(1f, tween(300))
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Surface900, Surface800, Surface900))
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background glow blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Violet400.copy(alpha = 0.07f))
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .clip(CircleShape)
                .background(Cyan400.copy(alpha = 0.06f))
                .blur(80.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo cluster
            Box(contentAlignment = Alignment.Center) {
                // Outer pulse ring
                Box(
                    modifier = Modifier
                        .size((140 * ringScale).dp)
                        .clip(CircleShape)
                        .background(Violet400.copy(alpha = ringAlpha * 0.2f))
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Violet400.copy(0.3f), Violet500.copy(0.1f))
                            )
                        )
                )
                // Core icon circle with gradient
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Violet500, Cyan400))),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.example.studyplannerai.R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // App name
            Text(
                "AI Smart Study Planner",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = OnSurface100,
                modifier = Modifier.alpha(textAlpha.value)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Plan Smart. Study Better.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = OnSurface300,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(56.dp))

            // Shimmer loading bar
            Box(
                modifier = Modifier
                    .alpha(dotAlpha.value)
                    .width(200.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Surface700)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .offset(x = shimmerOffset.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Violet300, Cyan300, Color.Transparent)
                            )
                        )
                )
            }

            Spacer(Modifier.height(20.dp))

            // Bouncing dots
            Row(
                modifier = Modifier.alpha(dotAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1, dot2, dot3).forEachIndexed { i, offset ->
                    Box(
                        modifier = Modifier
                            .offset(y = offset.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (i) {
                                    0 -> Violet400
                                    1 -> Cyan400
                                    else -> Emerald400
                                }
                            )
                    )
                }
            }
        }
    }
}
