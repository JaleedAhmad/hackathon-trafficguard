package com.traffic_guard.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.ProfileRepository
import com.traffic_guard.ai.data.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val result = profileRepository.getSettings()
            if (result is com.traffic_guard.ai.data.AppResult.Success) {
                _uiState.update { result.data ?: SettingsState() }
            }
        }
    }

    fun toggleDataSaver(enabled: Boolean) {
        val newState = _uiState.value.copy(dataSaverEnabled = enabled)
        saveSettings(newState)
    }

    fun toggleAlerts(enabled: Boolean) {
        val newState = _uiState.value.copy(alertsEnabled = enabled)
        saveSettings(newState)
    }

    private fun saveSettings(newState: SettingsState) {
        _uiState.update { newState }
        viewModelScope.launch {
            profileRepository.updateSettings(newState)
        }
    }
}
