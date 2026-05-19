package com.traffic_guard.ai.ui.mapnavigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.GuidanceStep
import com.traffic_guard.ai.data.LocationRepository
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.data.NavigationRepository
import com.traffic_guard.ai.data.Result
import com.traffic_guard.ai.data.RoutePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class NavigationUiState {
    IDLE,
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
    val error: String? = null
)

class NavigationViewModel(
    private val navigationRepository: NavigationRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapNavigationUiState())
    val uiState: StateFlow<MapNavigationUiState> = _uiState.asStateFlow()

    init {
        // Stream live location telemetry to trace active vehicle path
        viewModelScope.launch {
            locationRepository.startLocationUpdates()
            locationRepository.locationFlow.collectLatest { latLng ->
                _uiState.value = _uiState.value.copy(userLocation = latLng)
                updateGuidanceForLocation(latLng)
            }
        }
    }

    fun requestRoutePlan(destination: MapLatLng) {
        val start = _uiState.value.userLocation ?: MapLatLng(33.6844, 73.0479)
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
                                navState = NavigationUiState.ACTIVE_ROUTING,
                                currentGuidance = GuidanceStep(
                                    instruction = "Srinagar Highway par seedha chaliye (Go straight on Srinagar Highway)",
                                    distanceMeters = 800,
                                    durationSeconds = 60,
                                    maneuver = "straight"
                                ),
                                isLoading = false
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                activeRoute = activeRoute,
                                navState = NavigationUiState.ACTIVE_ROUTING,
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
            alternateRoutes = currentAlts,
            navState = NavigationUiState.ACTIVE_ROUTING,
            currentGuidance = GuidanceStep(
                instruction = "Naye raste par mudiye (Merge onto alternative detour route)",
                distanceMeters = 500,
                durationSeconds = 40,
                maneuver = "merge"
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
            val dist = active.distanceMeters / 2
            _uiState.value = _uiState.value.copy(
                currentGuidance = GuidanceStep(
                    instruction = "Agla turn 200m me baayein mudiye (Turn left in 200m onto Jinnah Ave)",
                    distanceMeters = dist,
                    durationSeconds = active.durationSeconds / 2,
                    maneuver = "turn-left"
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationRepository.stopLocationUpdates()
    }
}
