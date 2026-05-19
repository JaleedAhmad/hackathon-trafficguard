package com.traffic_guard.ai.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.EmergencyCenter
import com.traffic_guard.ai.data.EmergencyRepository
import com.traffic_guard.ai.data.MapLatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmergencyRouteUiState(
    val closestCenters: List<EmergencyCenter> = emptyList(),
    val selectedCenter: EmergencyCenter? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class EmergencyRoutingViewModel(
    private val emergencyRepository: EmergencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyRouteUiState())
    val uiState: StateFlow<EmergencyRouteUiState> = _uiState.asStateFlow()

    fun loadNearbyCenters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val location = MapLatLng(33.0, 73.0) // Mock location
            val result = emergencyRepository.getNearbyEmergencyCenters(location)
            
            if (result is com.traffic_guard.ai.data.AppResult.Success) {
                val centers = result.data ?: emptyList()
                _uiState.update {
                    it.copy(
                        closestCenters = centers,
                        selectedCenter = centers.firstOrNull(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load nearby emergency centers. Using offline cache..."
                    )
                }
            }
        }
    }

    fun selectCenter(center: EmergencyCenter) {
        _uiState.update { it.copy(selectedCenter = center) }
    }
}
