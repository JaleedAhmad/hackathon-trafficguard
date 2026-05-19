package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun ActiveRouteCard(
    etaMinutes: Int,
    distanceMeters: Int,
    instruction: String,
    maneuver: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    val arrowIcon = when (maneuver) {
        "turn-left" -> Icons.Default.TurnLeft
        "turn-right" -> Icons.Default.TurnRight
        "straight" -> Icons.Default.Navigation
        else -> Icons.Default.TurnSlightLeft
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkBgCard else LightBgCard
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) DarkBorder else LightBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = maneuver,
                tint = AccentBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$etaMinutes Min",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val formattedDist = if (distanceMeters >= 1000) {
                        String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000f)
                    } else {
                        "$distanceMeters m"
                    }
                    Text(
                        text = formattedDist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                    )
                }
            }
        }
    }
}
