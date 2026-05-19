package com.traffic_guard.ai.ui.permissions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionsUiState(
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val showRationaleDialog: Boolean = false,
    val rationaleMessage: String? = null
)

class PermissionsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            locationPermissionGranted = granted,
            showRationaleDialog = !granted,
            rationaleMessage = if (!granted) "Location access is crucial for live navigation updates and safety notifications!" else null
        )
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            notificationPermissionGranted = granted
        )
    }

    fun dismissRationaleDialog() {
        _uiState.value = _uiState.value.copy(showRationaleDialog = false, rationaleMessage = null)
    }
}
