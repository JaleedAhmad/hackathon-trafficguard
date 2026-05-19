package com.traffic_guard.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentRed

@Composable
fun SosCountdownButton(
    countdownValue: Int,
    isBroadcasting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            pulseScale.animateTo(
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp)
    ) {
        if (isBroadcasting) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = AccentRed.copy(alpha = 0.3f),
                    radius = size.minDimension / 2 * pulseScale.value
                )
                drawCircle(
                    color = AccentRed.copy(alpha = 0.5f),
                    radius = size.minDimension / 2 * (1f + (pulseScale.value - 1f) / 2)
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(AccentRed)
                .clickable { onClick() }
        ) {
            Text(
                text = if (isBroadcasting) "SOS\nACTIVE" else if (countdownValue > 0) "$countdownValue" else "SOS",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
