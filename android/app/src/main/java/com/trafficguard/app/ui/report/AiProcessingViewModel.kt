package com.traffic_guard.ai.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.ReportFormState
import com.traffic_guard.ai.data.ReportRepository
import com.traffic_guard.ai.data.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProcessingUiState(
    val currentVerificationStep: String = "Initializing AI analysis pipeline...",
    val progress: Float = 0.0f,
    val isSuccess: Boolean = false,
    val isOfflineSuccess: Boolean = false,
    val error: String? = null
)

class AiProcessingViewModel(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private val verificationSteps = listOf(
        "Scanning coordinates for active duplicate reports...",
        "Analyzing media metadata and attachments...",
        "Verifying AI credibility weights and simulation values...",
        "Pushing structured report entries to Firestore /reports...",
        "Incident reported successfully!"
    )

    fun submitReport(report: ReportFormState, isNetworkAvailable: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = ProcessingUiState()

            // Dynamic Step-by-Step AI Verification Simulation
            for (i in verificationSteps.indices) {
                delay(800) // Simulated AI reasoning step latency
                val progressVal = (i + 1).toFloat() / verificationSteps.size
                _uiState.value = _uiState.value.copy(
                    currentVerificationStep = verificationSteps[i],
                    progress = progressVal
                )
            }

            // Real submission flow
            val submitRes = reportRepository.submitReport(report, forceOffline = !isNetworkAvailable)
            delay(500)

            when (submitRes) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSuccess = isNetworkAvailable,
                        isOfflineSuccess = !isNetworkAvailable,
                        progress = 1.0f
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Ghalti! Report upload karne me nakami hui."
                    )
                }
                else -> {}
            }
        }
    }
}
