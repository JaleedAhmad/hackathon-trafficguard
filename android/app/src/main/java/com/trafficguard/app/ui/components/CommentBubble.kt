package com.traffic_guard.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.Comment
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.LightBgCard

@Composable
fun CommentBubble(
    comment: Comment,
    modifier: Modifier = Modifier,
    animate: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    val currentUserId = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
    
    // Determine if message is sent by me
    val isMe = remember(comment.userId, currentUserId) {
        comment.userId.isNotEmpty() && comment.userId == currentUserId
    }

    val displayName = remember(comment.userName) {
        val raw = comment.userName
        if (raw.length > 15) {
            raw.take(12) + "..."
        } else {
            raw
        }
    }

    @Composable
    fun BubbleContent() {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Chat bubbles shouldn't take full width
                    .background(
                        color = if (isMe) {
                            AccentBlue
                        } else {
                            if (isDark) DarkBgCard else LightBgCard
                        },
                        shape = if (isMe) {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                        } else {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                        }
                    )
                    .padding(12.dp)
            ) {
                // Sender Info Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isMe) "You" else displayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isMe) Color.White else (if (isDark) Color.White else Color.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    // Rank Tag (Only if not Guest / Bronze)
                    if (comment.userReputationClass != "BRONZE" && comment.userReputationClass.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (comment.userReputationClass) {
                                        "GOLD" -> Color(0xFFF59E0B).copy(alpha = if (isMe) 0.3f else 0.2f)
                                        "SILVER" -> Color(0xFF94A3B8).copy(alpha = if (isMe) 0.3f else 0.2f)
                                        else -> Color(0xFFB45309).copy(alpha = if (isMe) 0.3f else 0.2f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = comment.userReputationClass,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (comment.userReputationClass) {
                                    "GOLD" -> Color(0xFFFBBF24)
                                    "SILVER" -> Color(0xFFCBD5E1)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val timeStr = remember(comment.timestamp) {
                        if (comment.timestamp == 0L) "" else {
                            java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(comment.timestamp))
                        }
                    }
                    if (timeStr.isNotEmpty()) {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    }

                    if (comment.isPendingSync) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sending...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                }
                
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) Color.White else (if (isDark) Color.LightGray else Color.DarkGray),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (animate) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { if (isMe) it else -it }),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            BubbleContent()
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            BubbleContent()
        }
    }
}
