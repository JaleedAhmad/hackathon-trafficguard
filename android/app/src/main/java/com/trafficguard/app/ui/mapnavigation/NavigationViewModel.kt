package com.traffic_guard.ai.ui.mapnavigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.GuidanceStep
import com.traffic_guard.ai.data.LocationRepository
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.data.NavigationRepository
import com.traffic_guard.ai.data.Result
import com.traffic_guard.ai.data.RoutePath
import com.traffic_guard.ai.data.PlaceSuggestion
import com.traffic_guard.ai.data.TrafficGuardApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationUiState {
    IDLE,
    SELECTING_ROUTE,
    ACTIVE_ROUTING,
    PROXIMITY_HAZARD,
    REROUTING
}

data class MapNavigationUiState(
    val userLocation: MapLatLng? = null,
    val activeRoute: RoutePath? = null,
    val alternateRoutes: List<RoutePath> = emptyList(),
    val currentGuidance: GuidanceStep? = null,
    val navState: NavigationUiState = NavigationUiState.IDLE,
    val isLoading: Boolean = false,
    val reconsiderAlertMessage: String? = null,
    val sosAlertMessage: String? = null,
    val sosAlertLocation: MapLatLng? = null,
    val mapFocusLocation: MapLatLng? = null,
    val error: String? = null
)

class NavigationViewModel(
    private val navigationRepository: NavigationRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapNavigationUiState())
    val uiState: StateFlow<MapNavigationUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val searchResults: StateFlow<List<PlaceSuggestion>> = _searchResults.asStateFlow()

    private val api = TrafficGuardApiClient.service
    private var navigationWebSocket: okhttp3.WebSocket? = null

    init {
        // Stream live location telemetry to trace active vehicle path
        viewModelScope.launch {
            locationRepository.startLocationUpdates()
            locationRepository.locationFlow.collectLatest { latLng ->
                _uiState.update { it.copy(userLocation = latLng) }
                updateGuidanceForLocation(latLng)

                // Send live coordinate telemetry to server over active WebSocket
                navigationWebSocket?.let { ws ->
                    try {
                        val json = org.json.JSONObject().apply {
                            put("lat", latLng.latitude)
                            put("lng", latLng.longitude)
                        }
                        ws.send(json.toString())
                    } catch (e: Exception) {
                        android.util.Log.w("NavigationViewModel", "Failed to send WS location: ${e.message}")
                    }
                }
            }
        }
    }

    fun focusOnSosAlert() {
        val loc = _uiState.value.sosAlertLocation
        if (loc != null) {
            _uiState.update { it.copy(mapFocusLocation = loc) }
        }
    }

    fun clearSosAlert() {
        _uiState.update { it.copy(sosAlertMessage = null, sosAlertLocation = null, mapFocusLocation = null) }
    }

    fun connectNavigationSocket(uid: String, start: MapLatLng, destination: MapLatLng) {
        disconnectNavigationSocket()

        val host = "10.0.2.2:8000" // Standard emulator loopback
        val wsUrl = "ws://$host/ws/navigation?uid=$uid&start_lat=${start.latitude}&start_lng=${start.longitude}&dest_lat=${destination.latitude}&dest_lng=${destination.longitude}"
        
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(wsUrl)
            .build()

        val listener = object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val json = org.json.JSONObject(text)
                    if (json.optString("type") == "RECONSIDER_ROUTE") {
                        val message = json.optString("message")
                        _uiState.update { it.copy(reconsiderAlertMessage = message) }
                    } else if (json.optString("type") == "SOS_ALERT") {
                        val message = json.optString("message")
                        val lat = json.optDouble("lat", 0.0)
                        val lng = json.optDouble("lng", 0.0)
                        _uiState.update {
                            it.copy(
                                sosAlertMessage = message,
                                sosAlertLocation = MapLatLng(lat, lng)
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("NavigationViewModel", "WS parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                android.util.Log.w("NavigationViewModel", "Navigation WS failure: ${t.message}")
            }
        }

        navigationWebSocket = client.newWebSocket(request, listener)
    }

    fun disconnectNavigationSocket() {
        navigationWebSocket?.close(1000, "Disconnected")
        navigationWebSocket = null
        _uiState.update { it.copy(reconsiderAlertMessage = null, sosAlertMessage = null, sosAlertLocation = null, mapFocusLocation = null) }
    }

    fun retryLocationUpdates() {
        locationRepository.startLocationUpdates()
    }

    fun searchPlaces(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val response = api.getPlacesAutocomplete(query)
                _searchResults.value = response.predictions
            } catch (e: Exception) {
                android.util.Log.w("NavigationViewModel", "searchPlaces failed: ${e.message}")
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun resetToIdle() {
        disconnectNavigationSocket()
        _uiState.value = _uiState.value.copy(
            activeRoute = null,
            alternateRoutes = emptyList(),
            currentGuidance = null,
            navState = NavigationUiState.IDLE,
            error = null
        )
    }

    fun requestRoutePlan(start: MapLatLng, destination: MapLatLng) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val routeRes = navigationRepository.getRoute(start, destination)) {
                is Result.Success -> {
                    val activeRoute = routeRes.data
                    when (val altRes = navigationRepository.getAlternatives(start, destination)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                activeRoute = activeRoute,
                                alternateRoutes = altRes.data,
                                navState = NavigationUiState.SELECTING_ROUTE,
                                currentGuidance = null,
                                isLoading = false
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                activeRoute = activeRoute,
                                navState = NavigationUiState.SELECTING_ROUTE,
                                currentGuidance = null,
                                isLoading = false
                            )
                        }
                        else -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Route calculate karne me ghalti hui. Network check karein."
                    )
                }
                else -> {}
            }
        }
    }

    fun selectAlternateRoute(route: RoutePath) {
        val currentActive = _uiState.value.activeRoute ?: return
        val currentAlts = _uiState.value.alternateRoutes.toMutableList()

        currentAlts.remove(route)
        currentAlts.add(currentActive)

        _uiState.value = _uiState.value.copy(
            activeRoute = route,
            alternateRoutes = currentAlts
        )
    }

    fun reconsiderRoute(start: MapLatLng, destination: MapLatLng) {
        disconnectNavigationSocket()
        _uiState.update { it.copy(isLoading = true, reconsiderAlertMessage = null) }
        viewModelScope.launch {
            when (val routeRes = navigationRepository.getRoute(start, destination)) {
                is Result.Success -> {
                    val activeRoute = routeRes.data
                    when (val altRes = navigationRepository.getAlternatives(start, destination)) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    activeRoute = activeRoute,
                                    alternateRoutes = altRes.data,
                                    navState = NavigationUiState.SELECTING_ROUTE,
                                    currentGuidance = null,
                                    isLoading = false
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    activeRoute = activeRoute,
                                    navState = NavigationUiState.SELECTING_ROUTE,
                                    currentGuidance = null,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Route reconsider update failed. Please try again."
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun startNavigation() {
        val active = _uiState.value.activeRoute
        val startPoint = active?.points?.firstOrNull()
        _uiState.value = _uiState.value.copy(
            navState = NavigationUiState.ACTIVE_ROUTING,
            userLocation = startPoint ?: _uiState.value.userLocation,
            currentGuidance = GuidanceStep(
                instruction = "Continue following the highlighted route",
                distanceMeters = active?.distanceMeters ?: 800,
                durationSeconds = active?.durationSeconds ?: 60,
                maneuver = "straight"
            )
        )
    }

    fun triggerProximityWarning() {
        _uiState.value = _uiState.value.copy(
            navState = NavigationUiState.PROXIMITY_HAZARD
        )
    }

    fun clearHazardWarning() {
        _uiState.value = _uiState.value.copy(
            navState = NavigationUiState.ACTIVE_ROUTING
        )
    }

    private fun updateGuidanceForLocation(location: MapLatLng) {
        // Dynamic step updating based on simulated coordinates
        val active = _uiState.value.activeRoute ?: return
        val points = active.points
        if (points.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                currentGuidance = GuidanceStep(
                    instruction = "Continue following the highlighted route",
                    distanceMeters = active.distanceMeters,
                    durationSeconds = active.durationSeconds,
                    maneuver = "straight"
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectNavigationSocket()
        locationRepository.stopLocationUpdates()
    }
}
