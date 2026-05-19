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
    val phoneInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,
    val authenticatedUser: UserProfile? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthFormState())
    val uiState: StateFlow<AuthFormState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            emailInput = email,
            emailError = if (email.contains("@") || email.isEmpty()) null else "Ghalat email format (Invalid Email)"
        )
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            passwordInput = password,
            passwordError = if (password.length >= 8 || password.isEmpty()) null else "Password kam az kam 8 characters hona chahiye (Min 8 chars)"
        )
    }

    fun onPhoneChanged(phone: String) {
        _uiState.value = _uiState.value.copy(
            phoneInput = phone,
            phoneError = if (phone.all { it.isDigit() } || phone.isEmpty()) null else "Sirf numbers darj karein (Numbers only)"
        )
    }

    fun loginWithEmail(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.emailInput.isEmpty() || state.passwordInput.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email aur password khali nahi ho sakte (Fields cannot be empty)")
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
                    // Premium Roman Urdu hints mapping standard exception codes
                    val msg = res.exception.message ?: "Authentication fail ho gayi"
                    val romanUrduMsg = when {
                        msg.contains("password", ignoreCase = true) -> "Aap ka darj kiya hua password ghalat hai (Incorrect Password)"
                        msg.contains("user", ignoreCase = true) -> "Is email ka koi account nahi mila (User not found)"
                        else -> "Sign in nakam raha. Dobara koshish karein: $msg"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = romanUrduMsg)
                }
                else -> {}
            }
        }
    }

    fun signupWithEmail(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.emailInput.isEmpty() || state.passwordInput.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Fields khali nahi chor sakte")
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
                    val msg = res.exception.message ?: "Signup fail ho gayi"
                    val romanUrduMsg = when {
                        msg.contains("collision", ignoreCase = true) || msg.contains("exists", ignoreCase = true) -> 
                            "Yeh email pehle se register hai (Email already exists)"
                        else -> "Registration nakam rahi: $msg"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = romanUrduMsg)
                }
                else -> {}
            }
        }
    }

    fun loginAnonymously(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.signInAnonymously()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticatedUser = res.data)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Guest access nakam raha. Network check karein."
                    )
                }
                else -> {}
            }
        }
    }

    fun sendPhoneOtp(onSuccess: (String) -> Unit) {
        val phone = _uiState.value.phoneInput
        if (phone.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Phone number darj karein")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.sendOtpCode(phone)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(res.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "OTP bhejna nakam raha: ${res.exception.message}"
                    )
                }
                else -> {}
            }
        }
    }
}
