package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.UserRank
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.LightBgCard

@Composable
fun LeaderboardRowItem(
    userRank: UserRank,
    isCurrentUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isCurrentUser) (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        // Rank Number
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = when (userRank.rank) {
                        1 -> Color(0xFFF59E0B)
                        2 -> Color(0xFF94A3B8)
                        3 -> Color(0xFFB45309)
                        else -> if (isDark) DarkBgCard else LightBgCard
                    },
                    shape = CircleShape
                )
        ) {
            Text(
                text = "${userRank.rank}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (userRank.rank <= 3) Color.White else (if (isDark) Color.LightGray else Color.DarkGray)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name and Badge
        Text(
            text = userRank.name + if (isCurrentUser) " (You)" else "",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal),
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.weight(1f)
        )

        // Score
        Text(
            text = "${userRank.reputationScore} pts",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = when (userRank.reputationClass) {
                "GOLD" -> Color(0xFFF59E0B)
                "SILVER" -> Color(0xFF94A3B8)
                else -> Color(0xFFB45309)
            }
        )
    }
}
