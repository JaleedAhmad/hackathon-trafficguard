package com.traffic_guard.ai.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingSlideData(
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class OnboardingUiState(
    val slides: List<OnboardingSlideData> = listOf(
        OnboardingSlideData(
            title = "Real-time Traffic Updates",
            description = "Get instant notifications about traffic bottlenecks, accidents, and route delays across your regular routes.",
            icon = Icons.Default.Info
        ),
        OnboardingSlideData(
            title = "Smart Routing",
            description = "Let our AI-driven routing find the safest, fastest routes bypassing any potential hazard zones.",
            icon = Icons.Default.Map
        ),
        OnboardingSlideData(
            title = "Dynamic Safety Alerts",
            description = "Receive immediate alerts for dangerous road conditions, severe weather, and high-incident areas.",
            icon = Icons.Default.Security
        )
    ),
    val currentSlideIndex: Int = 0
)

class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateSlideIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentSlideIndex = index)
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            onComplete()
        }
    }
}
