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
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.EmergencyRouteCard

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory

@Composable
fun EmergencyRoutingScreen(
    viewModel: EmergencyRoutingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        val initialLoc = state.userLocation ?: MapLatLng(33.7220, 73.0580)
        position = CameraPosition.fromLatLngZoom(LatLng(initialLoc.latitude, initialLoc.longitude), 14f)
    }

    LaunchedEffect(Unit) {
        viewModel.loadNearbyCenters()
    }

    LaunchedEffect(state.userLocation) {
        state.userLocation?.let { userLoc ->
            if (state.selectedCenter == null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(userLoc.latitude, userLoc.longitude), 14.5f),
                    1000
                )
            }
        }
    }

    LaunchedEffect(state.selectedCenter) {
        state.selectedCenter?.let { selected ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(selected.location.latitude, selected.location.longitude), 15.5f),
                1000
            )
        }
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
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // User current location marker
                val userLatLng = state.userLocation ?: MapLatLng(33.7220, 73.0580)
                Marker(
                    state = MarkerState(position = LatLng(userLatLng.latitude, userLatLng.longitude)),
                    title = "Your Location"
                )

                // Safe zones markers
                state.closestCenters.forEach { center ->
                    Marker(
                        state = MarkerState(position = LatLng(center.location.latitude, center.location.longitude)),
                        title = center.name,
                        snippet = "${center.type}: ${center.address}"
                    )
                }
            }
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
                }
            }
        }
    }
}
