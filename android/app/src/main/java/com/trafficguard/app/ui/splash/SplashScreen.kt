package com.traffic_guard.ai.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep

@Composable
fun SplashScreen(
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Scale spring animation for the shield logo
    val scale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = uiState) {
        when (uiState) {
            is SplashState.NavigateToLanguageSelection -> onNavigateToLanguageSelection()
            is SplashState.NavigateToOnboarding -> onNavigateToOnboarding()
            is SplashState.NavigateToWelcome -> onNavigateToWelcome()
            is SplashState.NavigateToMain -> onNavigateToMain()
            else -> {}
        }
    }

    LaunchedEffect(key1 = true) {
        // Logo Shield spring scale-in
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Soft fade-in for brand text
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Highly high-fidelity custom shield drawing
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value)
            ) {
                val width = size.width
                val height = size.height
                
                val path = Path().apply {
                    moveTo(width * 0.5f, 0f)
                    lineTo(width * 0.9f, height * 0.15f)
                    quadraticTo(width * 0.9f, height * 0.6f, width * 0.5f, height)
                    quadraticTo(width * 0.1f, height * 0.6f, width * 0.1f, height * 0.15f)
                    close()
                }

                // AccentBlue backdrop fill
                drawPath(
                    path = path,
                    color = AccentBlue,
                    style = Fill
                )

                // Sleek inner core design to reflect modern AI protection
                val innerPath = Path().apply {
                    moveTo(width * 0.5f, height * 0.15f)
                    lineTo(width * 0.78f, height * 0.25f)
                    quadraticTo(width * 0.78f, height * 0.58f, width * 0.5f, height * 0.88f)
                    quadraticTo(width * 0.22f, height * 0.58f, width * 0.22f, height * 0.25f)
                    close()
                }
                drawPath(
                    path = innerPath,
                    color = Color.White.copy(alpha = 0.2f),
                    style = Fill
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "TRAFFICGUARD AI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = textAlpha.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Secure AI Navigation & Safety",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = textAlpha.value * 0.6f)
            )
        }
    }
}
