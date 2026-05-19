package com.traffic_guard.ai.ui.mapnavigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
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
import com.traffic_guard.ai.ui.components.AppTextField

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
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    var sourceText by remember { mutableStateOf("My Location") }
    var destText by remember { mutableStateOf("") }
    var activeSearchField by remember { mutableStateOf<String?>(null) } // "source" or "dest"

    var sourceLatLng by remember { mutableStateOf<MapLatLng?>(null) }
    var destLatLng by remember { mutableStateOf<MapLatLng?>(null) }

    val context = LocalContext.current

    val startAutocomplete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val latLng = place.latLng
            if (latLng != null) {
                if (activeSearchField == "source") {
                    sourceText = place.name ?: place.address ?: ""
                    sourceLatLng = MapLatLng(latLng.latitude, latLng.longitude)
                } else if (activeSearchField == "dest") {
                    destText = place.name ?: place.address ?: ""
                    destLatLng = MapLatLng(latLng.latitude, latLng.longitude)
                }
            }
        }
    }

    fun launchGooglePlacesSearch(field: String) {
        activeSearchField = field
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .setCountry("PK")
            .build(context)
        startAutocomplete.launch(intent)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) || 
            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            viewModel.retryLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(state.userLocation) {
        if (state.userLocation != null && sourceText == "My Location" && sourceLatLng == null) {
            sourceLatLng = state.userLocation
        }
    }

    LaunchedEffect(state.navState, state.activeRoute, state.userLocation) {
        val route = state.activeRoute
        if (state.navState == NavigationUiState.ACTIVE_ROUTING && route != null) {
            aiRerouteViewModel.startRouteMonitoring(route, state.userLocation)
        } else {
            aiRerouteViewModel.stopRouteMonitoring()
        }
    }

    LaunchedEffect(state.navState) {
        if (state.navState == NavigationUiState.ACTIVE_ROUTING) {
            val start = sourceLatLng ?: MapLatLng(33.6844, 73.0479)
            val dest = destLatLng ?: MapLatLng(33.6844, 73.0479)
            viewModel.connectNavigationSocket(
                uid = "anon_driver",
                start = start,
                destination = dest
            )
        } else {
            viewModel.disconnectNavigationSocket()
        }
    }

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
            alternateRoutes = state.alternateRoutes, // Draw the alternate routes on map
            showHeatmap = false,
            showFloodPolygons = state.navState == NavigationUiState.PROXIMITY_HAZARD,
            modifier = Modifier.fillMaxSize(),
            mapFocusLocation = state.mapFocusLocation,
            sosAlertLocation = state.sosAlertLocation,
            sosAlertMessage = state.sosAlertMessage
        )

        // Top Navigation Bar
        AppTopBar(
            title = if (state.navState == NavigationUiState.ACTIVE_ROUTING) "AI Active Navigation" else "Plan AI Route",
            onBackClick = {
                if (state.navState == NavigationUiState.ACTIVE_ROUTING) {
                    viewModel.resetToIdle()
                } else {
                    onNavigateBack()
                }
            },
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

        // SOS Alert Banner overlay
        state.sosAlertMessage?.let { sosMessage ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 90.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEF4444) // Vibrant, premium notification red
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.focusOnSosAlert()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CRITICAL SOS ALERT!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = sosMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { viewModel.clearSosAlert() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Route Reconsideration Banner overlay (only show if no active SOS alert is shown to avoid overlap)
        if (state.sosAlertMessage == null) {
            state.reconsiderAlertMessage?.let { alertMessage ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 90.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3B82F6) // Reconsider route blue alert
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val start = sourceLatLng ?: MapLatLng(33.6844, 73.0479)
                                val dest = destLatLng ?: MapLatLng(33.6844, 73.0479)
                                viewModel.reconsiderRoute(start, dest)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Traffic Alert!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = alertMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "REROUTE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

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
                        .padding(20.dp)
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
                            text = "AI Places Navigation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Source Input
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).clickable { launchGooglePlacesSearch("source") }) {
                            AppTextField(
                                value = sourceText,
                                onValueChange = {},
                                label = "Source (Start Point)",
                                leadingIcon = Icons.Default.Search,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(
                            onClick = {
                                sourceText = "My Location"
                                sourceLatLng = state.userLocation
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Use Current Location",
                                tint = AccentBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Destination Input
                    Box(modifier = Modifier.fillMaxWidth().clickable { launchGooglePlacesSearch("dest") }) {
                        AppTextField(
                            value = destText,
                            onValueChange = {},
                            label = "Destination",
                            leadingIcon = Icons.Default.Search,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        text = "Find Best AI Route",
                        onClick = {
                            val start = sourceLatLng ?: state.userLocation ?: MapLatLng(33.6844, 73.0479)
                            val dest = destLatLng ?: MapLatLng(33.7220, 73.0580)
                            viewModel.requestRoutePlan(start, dest)
                        },
                        variant = ButtonVariant.SOLID,
                        enabled = destText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Route Selection Drawer
        AnimatedVisibility(
            visible = state.navState == NavigationUiState.SELECTING_ROUTE,
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
                Text(
                    text = "Route Options",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Show currently selected/active route as an option too!
                state.activeRoute?.let { activeRoute ->
                    RouteAlternativeItem(
                        route = activeRoute,
                        onClick = { },
                        isRecommended = activeRoute.summary.contains("Primary", ignoreCase = true),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (state.alternateRoutes.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(state.alternateRoutes) { route ->
                            RouteAlternativeItem(
                                route = route,
                                onClick = { viewModel.selectAlternateRoute(route) },
                                isRecommended = route.summary.contains("Primary", ignoreCase = true),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AppButton(
                    text = "Start Navigation",
                    onClick = { viewModel.startNavigation() },
                    variant = ButtonVariant.SOLID,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Active Routing HUD Indicators
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
