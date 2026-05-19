package com.traffic_guard.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.Incident
import com.traffic_guard.ai.data.IncidentType
import com.traffic_guard.ai.data.LocationRepository
import com.traffic_guard.ai.data.MapLatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val userLocation: MapLatLng? = null,
    val nearbyIncidents: List<Incident> = emptyList(),
    val activeAlertCount: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Pre-load interactive mock incidents around Islamabad
        val mockIncidents = listOf(
            Incident(
                id = "inc_1",
                title = "Severe Flooding",
                description = "Street 4, Sector F-7 under 2 feet of water. Avoid route.",
                location = MapLatLng(33.7220, 73.0580),
                severity = 5,
                type = IncidentType.FLOOD
            ),
            Incident(
                id = "inc_2",
                title = "Car Collision",
                description = "Accident on Srinagar Highway near G-9. Heavy traffic backlog.",
                location = MapLatLng(33.6870, 73.0450),
                severity = 4,
                type = IncidentType.ACCIDENT
            ),
            Incident(
                id = "inc_3",
                title = "Road Blockage",
                description = "Construction work on Jinnah Avenue. Speed drop to 10km/h.",
                location = MapLatLng(33.7180, 73.0620),
                severity = 3,
                type = IncidentType.TRAFFIC
            )
        )

        _uiState.value = _uiState.value.copy(
            nearbyIncidents = mockIncidents,
            activeAlertCount = mockIncidents.size,
            isLoading = false
        )

        // Stream real-time location telemetry
        viewModelScope.launch {
            locationRepository.startLocationUpdates()
            locationRepository.locationFlow.collectLatest { latLng ->
                _uiState.value = _uiState.value.copy(userLocation = latLng)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationRepository.stopLocationUpdates()
    }
}
