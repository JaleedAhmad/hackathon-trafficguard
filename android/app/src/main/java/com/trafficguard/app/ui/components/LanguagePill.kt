package com.traffic_guard.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.DarkTextSecondary
import com.traffic_guard.ai.theme.LightBgCard
import com.traffic_guard.ai.theme.LightBorder
import com.traffic_guard.ai.theme.LightTextSecondary

@Composable
fun LanguagePill(
    title: String,
    nativeName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    // Smooth color state animations
    val borderAnim by animateColorAsState(
        targetValue = if (isSelected) AccentBlue else if (isDark) DarkBorder else LightBorder,
        animationSpec = tween(durationMillis = 250),
        label = "BorderColor"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "ScaleFactor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AccentBlue.copy(alpha = 0.15f)
            } else {
                if (isDark) DarkBgCard else LightBgCard
            }
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            borderAnim
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) AccentBlue else if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nativeName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) AccentBlue.copy(alpha = 0.8f) else if (isDark) DarkTextSecondary else LightTextSecondary
            )
        }
    }
}
