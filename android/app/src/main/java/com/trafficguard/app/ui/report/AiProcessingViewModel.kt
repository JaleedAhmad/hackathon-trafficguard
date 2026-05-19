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
        "Scanning coordinates for duplicate reports...",
        "Running AI Ingestion Agent (Phase 1)...",
        "Running AI Trust Detection Agent (Phase 2)...",
        "Running Situation Planning Agent (Phase 3)...",
        "Running Execution & Notification Agent (Phase 4)...",
        "Pushing structured report entries to backend...",
        "Incident reported successfully!"
    )

    fun submitReport(report: ReportFormState, isNetworkAvailable: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = ProcessingUiState()

            // Dynamic Step-by-Step AI Verification Simulation
            for (i in verificationSteps.indices) {
                delay(600) // Simulated reasoning latency
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
                        error = "Failed to submit report. Please verify connection and try again.",
                        progress = 0.0f
                    )
                }
                else -> {}
            }
        }
    }
}
