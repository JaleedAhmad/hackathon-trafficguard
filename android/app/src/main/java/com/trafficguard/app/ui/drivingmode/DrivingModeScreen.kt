package com.traffic_guard.ai.ui.drivingmode

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgDeep

@Composable
fun DrivingModeScreen(
    onExitDriving: () -> Unit,
    viewModel: VoiceGuidanceViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Pulsing audio animation synced with voice prompts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070F)) // Deep night mode background to reduce screen glare
            .padding(24.dp)
    ) {
        // Exit Driving Button in Top-Left
        IconButton(
            onClick = onExitDriving,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Driving Mode",
                tint = Color.White
            )
        }

        // Safe Oversized Guideline Displays
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp)
        ) {
            // Large Guidance Maneuver Arrow
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier
                    .size(96.dp)
                    .scale(if (state.isMuted) 1f else pulseScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // High Contrast Auditory Prompts Text Display
            Text(
                text = state.currentPromptText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Mute / Unmute Button HUD in Bottom-Right
        IconButton(
            onClick = { viewModel.toggleMute() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(64.dp)
                .background(
                    color = if (state.isMuted) AccentRed.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (state.isMuted) AccentRed else AccentBlue,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (state.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Mute Toggle",
                tint = if (state.isMuted) AccentRed else Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
