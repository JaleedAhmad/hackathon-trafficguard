package com.traffic_guard.ai.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.traffic_guard.ai.ui.components.ReputationGauge

@Composable
fun ContributorProfileScreen(
    viewModel: ReputationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "My Trust Profile",
            onBackClick = onNavigateBack
        )

        if (state.isLoading || state.currentUserRank == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            val user = state.currentUserRank!!
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                ReputationGauge(
                    score = user.reputationScore,
                    maxScore = 2000,
                    rankClass = user.reputationClass
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isDark) Color.White else Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Rank: #${user.rank} in your city",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "${user.contributionCount} Total Contributions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentBlue
                )
            }
        }
    }
}
