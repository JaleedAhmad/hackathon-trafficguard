package com.traffic_guard.ai.ui.mapnavigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.ActiveRouteCard
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.GoogleMapsView
import com.traffic_guard.ai.ui.components.RerouteRecommendationModal
import com.traffic_guard.ai.ui.components.RouteAlternativeItem

@Composable
fun MapNavigationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDrivingMode: () -> Unit,
    viewModel: NavigationViewModel,
    aiRerouteViewModel: AiRerouteViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val rerouteState by aiRerouteViewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        // Map Container showing active navigation route lines
        GoogleMapsView(
            userLocation = state.userLocation,
            incidents = emptyList(), // managed by parent or live subscription
            activeRoute = state.activeRoute,
            showHeatmap = false,
            showFloodPolygons = state.navState == NavigationUiState.PROXIMITY_HAZARD,
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation Bar
        AppTopBar(
            title = "Plan Route",
            onBackClick = onNavigateBack,
            actions = {
                if (state.navState == NavigationUiState.ACTIVE_ROUTING) {
                    IconButton(
                        onClick = onNavigateToDrivingMode,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveEta,
                            contentDescription = "Driving Mode",
                            tint = AccentBlue
                        )
                    }
                }
            }
        )

        // Floating Destination Planning Panel
        if (state.navState == NavigationUiState.IDLE) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkBgDeep else LightBgDeep
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
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
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kahan jana chahte hain? (Select Destination)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        text = "F-7 Markaz, Islamabad",
                        onClick = {
                            viewModel.requestRoutePlan(MapLatLng(33.7220, 73.0580))
                        },
                        variant = ButtonVariant.SOLID,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Active Routing HUD Indicators & Alternative Routes Drawer
        AnimatedVisibility(
            visible = state.navState == NavigationUiState.ACTIVE_ROUTING,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDark) DarkBgDeep else LightBgDeep,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(24.dp)
            ) {
                // Active Turn by Turn Navigation Guidance Card
                state.currentGuidance?.let { guidance ->
                    ActiveRouteCard(
                        etaMinutes = guidance.durationSeconds / 60,
                        distanceMeters = guidance.distanceMeters,
                        instruction = guidance.instruction,
                        maneuver = guidance.maneuver,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (state.alternateRoutes.isNotEmpty()) {
                    Text(
                        text = "Alternative Detour Choices",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(state.alternateRoutes) { route ->
                            RouteAlternativeItem(
                                route = route,
                                onClick = {
                                    viewModel.selectAlternateRoute(route)
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // AI Dynamic Reroute Dialog trigger listener
        if (rerouteState.showModal && rerouteState.hazardDetected != null) {
            // Trigger overlay polygon visual effects on map
            viewModel.triggerProximityWarning()

            RerouteRecommendationModal(
                hazardTitle = rerouteState.hazardDetected!!.title,
                hazardDescription = rerouteState.hazardDetected!!.description,
                detourSummary = rerouteState.proposedRoute?.summary ?: "Alternative detour bypass",
                timeSavedMinutes = rerouteState.timeSavedMinutes,
                onAccept = {
                    rerouteState.proposedRoute?.let { route ->
                        viewModel.selectAlternateRoute(route)
                    }
                    viewModel.clearHazardWarning()
                    aiRerouteViewModel.dismissModal()
                },
                onDismiss = {
                    viewModel.clearHazardWarning()
                    aiRerouteViewModel.dismissModal()
                }
            )
        }
    }
}
