package com.traffic_guard.ai.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.EmergencyRepository
import com.traffic_guard.ai.data.MapLatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.traffic_guard.ai.data.LocationRepository

data class SosUiState(
    val tapsRemaining: Int = 3,
    val isBroadcasting: Boolean = false,
    val contactsAlerted: Boolean = false,
    val emergencyActive: Boolean = false,
    val locationNotEnabled: Boolean = false,
    val error: String? = null
)

class SosViewModel(
    private val emergencyRepository: EmergencyRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    init {
        locationRepository.startLocationUpdates()
    }

    fun registerSosTap(problem: String) {
        if (_uiState.value.isBroadcasting) return

        if (!locationRepository.isLocationEnabled()) {
            _uiState.update { it.copy(locationNotEnabled = true) }
            return
        }

        val remaining = _uiState.value.tapsRemaining
        if (remaining <= 1) {
            _uiState.update { it.copy(tapsRemaining = 0) }
            triggerEmergency(problem)
        } else {
            _uiState.update { it.copy(tapsRemaining = remaining - 1) }
        }
    }

    fun abortSos() {
        _uiState.update { it.copy(tapsRemaining = 3, isBroadcasting = false, emergencyActive = false, locationNotEnabled = false) }
        viewModelScope.launch {
            emergencyRepository.cancelSos()
        }
    }

    fun resetSosState() {
        _uiState.update { it.copy(tapsRemaining = 3, isBroadcasting = false, contactsAlerted = false, emergencyActive = false, locationNotEnabled = false) }
    }

    private fun triggerEmergency(problem: String) {
        _uiState.update { it.copy(isBroadcasting = true) }

        viewModelScope.launch {
            val location = locationRepository.currentLocation ?: MapLatLng(33.7220, 73.0580)
            val result = emergencyRepository.triggerSos(location, problem, true)

            if (result is com.traffic_guard.ai.data.AppResult.Success) {
                _uiState.update {
                    it.copy(
                        contactsAlerted = true,
                        emergencyActive = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBroadcasting = false,
                        tapsRemaining = 3,
                        error = "Failed to broadcast SOS. Ensure SMS permissions are enabled for offline fallback."
                    )
                }
            }
        }
    }
}

