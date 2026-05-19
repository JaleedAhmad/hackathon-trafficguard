package com.traffic_guard.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
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
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AlertBannerCard
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.GoogleMapsView
import com.traffic_guard.ai.ui.components.SkeletonShimmer

@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToReport: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        if (state.isLoading) {
            SkeletonShimmer(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Live styled Google Map rendering incidents
            GoogleMapsView(
                userLocation = state.userLocation,
                incidents = state.nearbyIncidents,
                activeRoute = null,
                showHeatmap = true,
                showFloodPolygons = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Header Alert Banner HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            if (state.nearbyIncidents.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.nearbyIncidents) { incident ->
                        AlertBannerCard(
                            title = incident.title,
                            description = incident.description,
                            severity = incident.severity,
                            modifier = Modifier
                                .fillParentMaxWidth(0.9f)
                                .padding(end = 8.dp)
                        )
                    }
                }
            }
        }

        // Bottom Dashboard Controller HUD
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkBgDeep else LightBgDeep
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.activeAlertCount} Nearby Active Hazards",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppButton(
                    text = "Start AI Navigation",
                    onClick = onNavigateToMap,
                    variant = ButtonVariant.SOLID,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Floating Action Buttons for quick report reporting
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = onNavigateToReport,
                shape = CircleShape,
                containerColor = AccentRed,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Report Incident",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
