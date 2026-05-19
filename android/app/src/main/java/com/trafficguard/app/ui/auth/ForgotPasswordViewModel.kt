package com.traffic_guard.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForgotPassState(
    val emailInput: String = "",
    val isSent: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPassState())
    val uiState: StateFlow<ForgotPassState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            emailInput = email,
            isError = false,
            errorMsg = null
        )
    }

    fun sendPasswordReset() {
        val email = _uiState.value.emailInput
        if (email.isEmpty() || !email.contains("@")) {
            _uiState.value = _uiState.value.copy(
                isError = true,
                errorMsg = "Ghalat email format (Invalid Email)"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, isError = false, errorMsg = null)
        viewModelScope.launch {
            when (val res = authRepository.sendPasswordResetEmail(email)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSent = true)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isError = true,
                        errorMsg = "Password reset email bhejne me ghalti hui."
                    )
                }
                else -> {}
            }
        }
    }
}
