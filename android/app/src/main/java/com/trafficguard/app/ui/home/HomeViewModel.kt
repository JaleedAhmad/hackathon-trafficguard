package com.traffic_guard.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.CommunityRepository
import com.traffic_guard.ai.data.Incident
import com.traffic_guard.ai.data.IncidentType
import com.traffic_guard.ai.data.LocationRepository
import com.traffic_guard.ai.data.MapLatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val userLocation: MapLatLng? = null,
    val nearbyIncidents: List<Incident> = emptyList(),
    val activeAlertCount: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val locationRepository: LocationRepository,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Stream real-time location telemetry
        viewModelScope.launch {
            locationRepository.startLocationUpdates()
            locationRepository.locationFlow.collectLatest { latLng ->
                _uiState.update { it.copy(userLocation = latLng) }
            }
        }

        // Fetch live alerts from the backend instead of static Islamabad mock incidents
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = communityRepository.getAlertsFeed(limit = 50, offset = 0, filter = "ALL")) {
                is com.traffic_guard.ai.data.CommunityResult.Success -> {
                    val realIncidents = result.data
                    _uiState.update { state ->
                        state.copy(
                            nearbyIncidents = realIncidents,
                            activeAlertCount = realIncidents.size,
                            isLoading = false
                        )
                    }
                }
                is com.traffic_guard.ai.data.CommunityResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationRepository.stopLocationUpdates()
    }
}
