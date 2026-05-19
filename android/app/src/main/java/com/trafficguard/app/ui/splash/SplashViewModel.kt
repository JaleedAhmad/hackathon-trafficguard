package com.traffic_guard.ai.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashState {
    object Initializing : SplashState
    object NavigateToLanguageSelection : SplashState
    object NavigateToOnboarding : SplashState
    object NavigateToWelcome : SplashState
    object NavigateToMain : SplashState
}

class SplashViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashState>(SplashState.Initializing)
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Emulate bootstrap checks (2 seconds)
            delay(2000)
            
            val language = preferencesRepository.preferredLanguage.first()
            val onboardingCompleted = preferencesRepository.isOnboardingCompleted.first()
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

            if (language == null) {
                _uiState.value = SplashState.NavigateToLanguageSelection
            } else if (!onboardingCompleted) {
                _uiState.value = SplashState.NavigateToOnboarding
            } else if (firebaseUser != null) {
                _uiState.value = SplashState.NavigateToMain
            } else {
                _uiState.value = SplashState.NavigateToWelcome
            }
        }
    }
}
