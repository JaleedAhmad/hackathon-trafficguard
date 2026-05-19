package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue

@Composable
fun ReputationGauge(
    score: Int,
    maxScore: Int = 2000,
    rankClass: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val progress = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)

    val gaugeColor = when (rankClass) {
        "GOLD" -> Color(0xFFF59E0B)
        "SILVER" -> Color(0xFF94A3B8)
        else -> Color(0xFFB45309)
    }
    
    val trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(160.dp)
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            // Track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = rankClass,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = gaugeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = "Trust Points",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
