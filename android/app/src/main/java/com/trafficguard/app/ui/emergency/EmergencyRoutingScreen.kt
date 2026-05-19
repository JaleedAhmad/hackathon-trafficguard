package com.traffic_guard.ai.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.EmergencyRouteCard

@Composable
fun EmergencyRoutingScreen(
    viewModel: EmergencyRoutingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNearbyCenters()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgDeep) // Emergency screens are forced dark
    ) {
        AppTopBar(
            title = "Emergency Routing",
            onBackClick = onNavigateBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E293B)) // Placeholder for Map
        ) {
            Text(
                text = "Map view highlights safe shelter and hospital overlays here.",
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Map markers would go here
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBgDeep)
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentRed)
                }
            } else {
                Column {
                    Text(
                        text = "Nearest Safe Zones",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn {
                        items(state.closestCenters) { center ->
                            EmergencyRouteCard(
                                center = center,
                                onClick = { viewModel.selectCenter(center) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.selectedCenter != null) {
                        AppButton(
                            text = "Start Navigation to ${state.selectedCenter?.name}",
                            onClick = { /* Start real navigation */ },
                            variant = ButtonVariant.SOLID,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
