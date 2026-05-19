package com.traffic_guard.ai.ui.showcase

import androidx.lifecycle.ViewModel
import com.traffic_guard.ai.data.ThemeMode
import com.traffic_guard.ai.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShowcaseUiState(
    val inputText: String = "",
    val inputError: String? = null,
    val isLoadingSimulated: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO
)

sealed interface ShowcaseEvent {
    data class InputTextChanged(val text: String) : ShowcaseEvent
    data class ThemeModeChanged(val mode: ThemeMode) : ShowcaseEvent
    data class SetLoadingState(val isLoading: Boolean) : ShowcaseEvent
}

class ShowcaseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShowcaseUiState())
    val uiState: StateFlow<ShowcaseUiState> = _uiState.asStateFlow()

    fun onEvent(event: ShowcaseEvent) {
        when (event) {
            is ShowcaseEvent.InputTextChanged -> {
                val error = if (event.text.length < 3 && event.text.isNotEmpty()) {
                    "Input must be at least 3 characters"
                } else null
                _uiState.value = _uiState.value.copy(
                    inputText = event.text,
                    inputError = error
                )
            }
            is ShowcaseEvent.ThemeModeChanged -> {
                _uiState.value = _uiState.value.copy(themeMode = event.mode)
            }
            is ShowcaseEvent.SetLoadingState -> {
                _uiState.value = _uiState.value.copy(isLoadingSimulated = event.isLoading)
            }
        }
    }
}
