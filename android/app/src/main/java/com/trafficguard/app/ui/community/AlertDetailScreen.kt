package com.traffic_guard.ai.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.TrustActionButtons

@Composable
fun AlertDetailScreen(
    incidentId: String,
    viewModel: AlertDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiscussion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    LaunchedEffect(incidentId) {
        viewModel.loadIncidentDetails(incidentId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "Incident Details",
            onBackClick = onNavigateBack
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (state.incident != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                val incident = state.incident!!

                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = incident.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Community Trust",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${state.totalVotes} drivers confirmed this is active.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentBlue
                )

                Spacer(modifier = Modifier.height(24.dp))

                TrustActionButtons(
                    currentVote = state.userVoteState,
                    onUpvote = { viewModel.submitVote(incidentId, true) },
                    onDownvote = { viewModel.submitVote(incidentId, false) }
                )

                Spacer(modifier = Modifier.weight(1f))

                AppButton(
                    text = "Join Discussion",
                    onClick = { onNavigateToDiscussion(incidentId) },
                    variant = ButtonVariant.SOLID,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Error loading details.", color = Color.Red)
            }
        }
    }
}
