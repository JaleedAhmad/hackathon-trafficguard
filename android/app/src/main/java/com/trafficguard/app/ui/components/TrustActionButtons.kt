package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.VoteType
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.AccentRed

@Composable
fun TrustActionButtons(
    currentVote: VoteType,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        AppButton(
            text = if (currentVote == VoteType.UPVOTE) "Confirmed" else "Confirm Active",
            onClick = onUpvote,
            variant = if (currentVote == VoteType.UPVOTE) ButtonVariant.SOLID else ButtonVariant.OUTLINED,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        AppButton(
            text = "Report Cleared",
            onClick = onDownvote,
            variant = if (currentVote == VoteType.DOWNVOTE) ButtonVariant.SOLID else ButtonVariant.OUTLINED,
            modifier = Modifier.weight(1f)
        )
    }
}
