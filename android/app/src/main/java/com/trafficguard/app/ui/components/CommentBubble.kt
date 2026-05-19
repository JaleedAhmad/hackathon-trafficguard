package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.Comment
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.LightBgCard

@Composable
fun CommentBubble(
    comment: Comment,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = if (isDark) DarkBgCard else LightBgCard,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Rank tag
                Box(
                    modifier = Modifier
                        .background(
                            color = when (comment.userReputationClass) {
                                "GOLD" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                "SILVER" -> Color(0xFF94A3B8).copy(alpha = 0.2f)
                                else -> Color(0xFFB45309).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = comment.userReputationClass,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (comment.userReputationClass) {
                            "GOLD" -> Color(0xFFF59E0B)
                            "SILVER" -> Color(0xFF94A3B8)
                            else -> Color(0xFFB45309)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (comment.isPendingSync) {
                    Text(
                        text = "Sending...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
