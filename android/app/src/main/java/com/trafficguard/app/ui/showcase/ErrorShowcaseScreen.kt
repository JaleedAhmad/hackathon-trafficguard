package com.traffic_guard.ai.ui.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.EmptyStateView
import com.traffic_guard.ai.ui.components.ErrorMessage

@Composable
fun ErrorShowcaseScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTopBar(
            title = "Empty & Error States",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Audit of Application Non-Ideal Flows",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Showcase Item 1: Generic Warning Error Card
            ErrorMessage(
                title = "Network Sync Failed",
                message = "The TrafficGuard AI core was unable to sync local geo-intelligence reports with our cloud layers. Please verify your connection.",
                onRetry = {}
            )

            // Showcase Item 2: Empty State Card with primary action hook
            EmptyStateView(
                title = "No Traffic Reports Found",
                description = "It looks like your current regional zone is completely clear. Enjoy a secure, uninterrupted drive!",
                icon = Icons.Default.Info,
                actionText = "Report an Incident",
                onActionClick = {}
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
