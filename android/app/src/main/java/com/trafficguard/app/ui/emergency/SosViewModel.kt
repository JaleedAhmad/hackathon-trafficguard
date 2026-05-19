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

data class SosUiState(
    val countdownValue: Int = 3,
    val isBroadcasting: Boolean = false,
    val contactsAlerted: Boolean = false,
    val emergencyActive: Boolean = false,
    val error: String? = null
)

class SosViewModel(
    private val emergencyRepository: EmergencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var countdownJob: kotlinx.coroutines.Job? = null

    fun startSosCountdown() {
        if (_uiState.value.isBroadcasting || countdownJob?.isActive == true) return

        _uiState.update { it.copy(countdownValue = 3, error = null) }

        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(countdownValue = i) }
                delay(1000)
            }
            triggerEmergency()
        }
    }

    fun abortSos() {
        countdownJob?.cancel()
        _uiState.update { it.copy(countdownValue = 3, isBroadcasting = false, emergencyActive = false) }
        viewModelScope.launch {
            emergencyRepository.cancelSos()
        }
    }

    private fun triggerEmergency() {
        _uiState.update { it.copy(isBroadcasting = true, countdownValue = 0) }

        viewModelScope.launch {
            val location = MapLatLng(33.0, 73.0) // Mock current location
            val result = emergencyRepository.triggerSos(location, true)

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
                        error = "Failed to broadcast SOS. Ensure SMS permissions are enabled for offline fallback."
                    )
                }
            }
        }
    }
}
