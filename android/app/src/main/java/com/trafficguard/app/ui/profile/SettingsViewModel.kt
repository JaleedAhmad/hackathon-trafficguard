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

import com.traffic_guard.ai.data.PreferencesRepository
import com.traffic_guard.ai.data.ThemeMode
import kotlinx.coroutines.flow.first

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val result = profileRepository.getSettings()
            val prefTheme = preferencesRepository.themeMode.first()
            val themeType = when(prefTheme) {
                ThemeMode.ALWAYS_DARK -> com.traffic_guard.ai.data.ThemeType.DARK
                ThemeMode.ALWAYS_LIGHT -> com.traffic_guard.ai.data.ThemeType.LIGHT
                else -> com.traffic_guard.ai.data.ThemeType.SYSTEM
            }
            if (result is com.traffic_guard.ai.data.AppResult.Success) {
                _uiState.update { (result.data ?: SettingsState()).copy(theme = themeType) }
            } else {
                _uiState.update { it.copy(theme = themeType) }
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

    fun toggleLocation(enabled: Boolean) {
        val newState = _uiState.value.copy(locationEnabled = enabled)
        saveSettings(newState)
    }

    fun toggleTheme(theme: com.traffic_guard.ai.data.ThemeType) {
        val newState = _uiState.value.copy(theme = theme)
        saveSettings(newState)
        
        viewModelScope.launch {
            val prefMode = when(theme) {
                com.traffic_guard.ai.data.ThemeType.DARK -> ThemeMode.ALWAYS_DARK
                com.traffic_guard.ai.data.ThemeType.LIGHT -> ThemeMode.ALWAYS_LIGHT
                else -> ThemeMode.AUTO
            }
            preferencesRepository.setThemeMode(prefMode)
        }
    }

    private fun saveSettings(newState: SettingsState) {
        _uiState.update { newState }
        viewModelScope.launch {
            profileRepository.updateSettings(newState)
        }
    }
}
