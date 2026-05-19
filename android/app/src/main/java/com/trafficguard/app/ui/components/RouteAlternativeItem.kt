package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.RoutePath
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.DarkTextSecondary
import com.traffic_guard.ai.theme.LightBgCard
import com.traffic_guard.ai.theme.LightBorder
import com.traffic_guard.ai.theme.LightTextSecondary

@Composable
fun RouteAlternativeItem(
    route: RoutePath,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    val formattedDuration = "${route.durationSeconds / 60} min"
    val formattedDist = if (route.distanceMeters >= 1000) {
        String.format(java.util.Locale.US, "%.1f km", route.distanceMeters / 1000f)
    } else {
        "${route.distanceMeters} m"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkBgCard else LightBgCard
        ),
        border = BorderStroke(
            1.dp,
            if (route.isHazardSegment) AccentRed else (if (isDark) DarkBorder else LightBorder)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.summary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "$formattedDuration • $formattedDist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                )
            }
            if (route.isHazardSegment) {
                Text(
                    text = "HAZARD AHEAD",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentRed,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Text(
                    text = "FASTER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF10B981), // Emerald
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
