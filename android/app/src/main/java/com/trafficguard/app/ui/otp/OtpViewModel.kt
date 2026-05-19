package com.traffic_guard.ai.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.data.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OtpUiState(
    val codeBuffer: String = "",
    val countdownTimerSeconds: Int = 60,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val verificationId: String = ""
)

class OtpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun setVerificationId(verificationId: String) {
        _uiState.value = _uiState.value.copy(verificationId = verificationId)
        startCountdownTimer()
    }

    fun onCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(
            codeBuffer = code,
            errorMessage = if (code.length == 6 && code != "123456") "Ghalat verification code (Incorrect Code)" else null
        )
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.codeBuffer.length != 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ghalat code. 6 hindso ka code darj karein.")
            return
        }

        _uiState.value = _uiState.value.copy(isVerifying = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.verifyOtpCode(state.verificationId, state.codeBuffer)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isVerifying = false)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        errorMessage = "OTP verification fail ho gayi. Code dubara check karein."
                    )
                }
                else -> {}
            }
        }
    }

    fun resendOtp(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(countdownTimerSeconds = 60, errorMessage = null)
        startCountdownTimer()
        viewModelScope.launch {
            authRepository.sendOtpCode(phoneNumber)
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.countdownTimerSeconds > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    countdownTimerSeconds = _uiState.value.countdownTimerSeconds - 1
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
