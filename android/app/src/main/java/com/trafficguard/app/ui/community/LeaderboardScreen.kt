package com.traffic_guard.ai.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.LeaderboardRowItem

@Composable
fun LeaderboardScreen(
    viewModel: ReputationViewModel,
    onNavigateBack: () -> Unit,
    isNested: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        if (!isNested) {
            AppTopBar(
                title = "Leaderboard",
                onBackClick = onNavigateBack
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Text(
                        text = "Top Contributors",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                items(state.leaderboardUsers) { user ->
                    LeaderboardRowItem(
                        userRank = user,
                        isCurrentUser = user.id == state.currentUserRank?.id
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val currentUser = state.currentUserRank
                if (currentUser != null && state.leaderboardUsers.none { it.id == currentUser.id }) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your Rank",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LeaderboardRowItem(
                            userRank = currentUser,
                            isCurrentUser = true
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
