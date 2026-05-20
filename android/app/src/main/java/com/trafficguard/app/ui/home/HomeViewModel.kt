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
    val isLoading: Boolean = true,
    val isLocationEnabled: Boolean = true,
    val hasLocationPermission: Boolean = true
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

        // Fetch and load initial incidents
        refreshIncidents(showLoading = true)
    }

    fun checkLocationRequirements(context: android.content.Context) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        val isEnabled = locationRepository.isLocationEnabled()
        
        _uiState.update { state ->
            state.copy(
                isLocationEnabled = isEnabled,
                hasLocationPermission = hasFine || hasCoarse
            )
        }
    }

    fun startLocationTracking() {
        viewModelScope.launch {
            locationRepository.startLocationUpdates()
        }
    }


    fun refreshIncidents(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }
            when (val result = communityRepository.getAlertsFeed(limit = 100, offset = 0, filter = "ALL")) {
                is com.traffic_guard.ai.data.CommunityResult.Success -> {
                    val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
                    val filtered = result.data.filter { it.timestamp >= cutoff }
                    _uiState.update { state ->
                        state.copy(
                            nearbyIncidents = filtered,
                            activeAlertCount = filtered.size,
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
