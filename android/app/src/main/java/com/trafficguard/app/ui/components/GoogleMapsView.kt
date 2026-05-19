package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.traffic_guard.ai.data.Incident
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.data.RoutePath
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.AccentRed

@Composable
fun GoogleMapsView(
    userLocation: MapLatLng?,
    incidents: List<Incident>,
    activeRoute: RoutePath?,
    showHeatmap: Boolean,
    showFloodPolygons: Boolean,
    modifier: Modifier = Modifier
) {
    val defaultPos = LatLng(33.6844, 73.0479) // Default Islamabad coordinate
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 14f)
    }

    // Centering camera when user moves
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 14.5f),
                1000
            )
        }
    }

    val mapUiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false,
            compassEnabled = true
        )
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = userLocation != null
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = mapUiSettings,
        properties = mapProperties
    ) {
        // Draw User Location marker
        userLocation?.let {
            Marker(
                state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                title = "Aap Ki Location (You)",
                snippet = "Live Position Telemetry"
            )
        }

        // Draw Incident markers
        incidents.forEach { inc ->
            Marker(
                state = MarkerState(position = LatLng(inc.location.latitude, inc.location.longitude)),
                title = inc.title,
                snippet = inc.description
            )

            // Draw Traffic Heatmap rings
            if (showHeatmap && inc.type == com.traffic_guard.ai.data.IncidentType.TRAFFIC) {
                Circle(
                    center = LatLng(inc.location.latitude, inc.location.longitude),
                    radius = 300.0, // 300 meters circle
                    fillColor = AccentRed.copy(alpha = 0.25f),
                    strokeColor = AccentRed,
                    strokeWidth = 2f
                )
            }
        }

        // Draw active route polyline path
        activeRoute?.let { route ->
            val routePoints = route.points.map { LatLng(it.latitude, it.longitude) }
            Polyline(
                points = routePoints,
                color = if (route.isHazardSegment) AccentRed else AccentBlue,
                width = 12f
            )
        }

        // Draw blue semi-transparent polygons illustrating active flooding alerts
        if (showFloodPolygons) {
            // Islamabad Sector F-7 Flood Warning Area Polygon
            val f7FloodBounds = listOf(
                LatLng(33.7250, 73.0550),
                LatLng(33.7280, 73.0650),
                LatLng(33.7200, 73.0680),
                LatLng(33.7170, 73.0570)
            )

            Polygon(
                points = f7FloodBounds,
                fillColor = Color(0x403B82F6), // Blue 25% opacity
                strokeColor = Color(0xFF3B82F6), // Neon blue border
                strokeWidth = 3f
            )
        }
    }
}
