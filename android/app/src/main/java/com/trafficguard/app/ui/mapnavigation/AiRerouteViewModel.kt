package com.traffic_guard.ai.ui.mapnavigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.Incident
import com.traffic_guard.ai.data.IncidentType
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.data.RoutePath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiRerouteUiState(
    val hazardDetected: Incident? = null,
    val proposedRoute: RoutePath? = null,
    val timeSavedMinutes: Int = 0,
    val showModal: Boolean = false,
    val isCalculatingDetour: Boolean = false
)

class AiRerouteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiRerouteUiState())
    val uiState: StateFlow<AiRerouteUiState> = _uiState.asStateFlow()

    init {
        // Trigger simulated AI detour recommendations after a short driving interval
        triggerSimulatedHazardAhead()
    }

    fun triggerSimulatedHazardAhead() {
        viewModelScope.launch {
            delay(12000) // Simulates driving for 12 seconds before a hazard is spotted ahead
            _uiState.value = _uiState.value.copy(
                hazardDetected = Incident(
                    id = "hazard_f7",
                    title = "Flood Proximity Danger",
                    description = "Heavy flash flood registered 500m ahead. Srinagar Highway segment submerged.",
                    location = MapLatLng(33.6930, 73.0550),
                    severity = 5,
                    type = IncidentType.FLOOD
                ),
                isCalculatingDetour = true,
                showModal = true
            )

            delay(2000) // Simulate AI reasoning / route planning delay

            val detourPoints = listOf(
                MapLatLng(33.6844, 73.0479),
                MapLatLng(33.6910, 73.0495),
                MapLatLng(33.6980, 73.0510),
                MapLatLng(33.7040, 73.0535),
                MapLatLng(33.7120, 73.0570)
            )

            _uiState.value = _uiState.value.copy(
                isCalculatingDetour = false,
                proposedRoute = RoutePath(
                    points = detourPoints,
                    distanceMeters = 6800,
                    durationSeconds = 620,
                    isHazardSegment = false,
                    summary = "AI Recommended detour via G-9 Bypass"
                ),
                timeSavedMinutes = 8 // Saves 8 minutes compared to getting stuck in flood traffic
            )
        }
    }

    fun acceptDetour(onAccepted: (RoutePath) -> Unit) {
        _uiState.value.proposedRoute?.let {
            onAccepted(it)
        }
        dismissModal()
    }

    fun dismissModal() {
        _uiState.value = _uiState.value.copy(
            showModal = false
        )
    }
}
