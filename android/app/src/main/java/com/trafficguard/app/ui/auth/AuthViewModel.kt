package com.traffic_guard.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.data.Result
import com.traffic_guard.ai.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthFormState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val authenticatedUser: UserProfile? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthFormState())
    val uiState: StateFlow<AuthFormState> = _uiState.asStateFlow()

    // ── Field Validators ──────────────────────────────────────────────────────

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            emailInput = email,
            emailError = if (email.isEmpty() || email.contains("@")) null
                         else "Invalid email address format."
        )
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            passwordInput = password,
            passwordError = if (password.isEmpty() || password.length >= 6) null
                            else "Password must be at least 6 characters."
        )
    }

    // ── Auth Actions ──────────────────────────────────────────────────────────

    fun loginWithEmail(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email and password cannot be empty.")
            return
        }
        if (state.emailError != null || state.passwordError != null) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.signInWithEmail(state.emailInput, state.passwordInput)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticatedUser = res.data)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.exception.message
                    )
                }
                else -> {}
            }
        }
    }

    fun signupWithEmail(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email and password cannot be empty.")
            return
        }
        if (state.emailError != null || state.passwordError != null) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.signUpWithEmail(state.emailInput, state.passwordInput)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticatedUser = res.data)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.exception.message
                    )
                }
                else -> {}
            }
        }
    }

    /**
     * Called after the Credential Manager flow resolves a Google ID token.
     */
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticatedUser = res.data)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.exception.message
                    )
                }
                else -> {}
            }
        }
    }

    /**
     * Browse as Guest — no account required.
     * Profile and incident-posting features will be restricted.
     */
    fun continueAsGuest(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.signInAsGuest()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticatedUser = res.data)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to start guest session."
                    )
                }
                else -> {}
            }
        }
    }

    // Keep legacy name for backward compat in existing callers
    fun loginAnonymously(onSuccess: () -> Unit) = continueAsGuest(onSuccess)
}
