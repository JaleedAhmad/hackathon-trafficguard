package com.traffic_guard.ai.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.Incident
import com.traffic_guard.ai.data.ReportRepository
import com.traffic_guard.ai.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DuplicateUiState(
    val duplicatesList: List<Incident> = emptyList(),
    val isChecking: Boolean = false,
    val duplicateConfirmed: Boolean = false,
    val skipCheck: Boolean = false
)

class DuplicateCheckViewModel(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicateUiState())
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    fun checkForDuplicates(lat: Double, lng: Double, category: String, onFinished: (Boolean) -> Unit) {
        if (_uiState.value.skipCheck) {
            onFinished(false)
            return
        }

        _uiState.value = _uiState.value.copy(isChecking = true)
        viewModelScope.launch {
            when (val res = reportRepository.checkNearbyDuplicates(lat, lng, category)) {
                is Result.Success -> {
                    val list = res.data
                    _uiState.value = _uiState.value.copy(
                        duplicatesList = list,
                        isChecking = false,
                        duplicateConfirmed = list.isNotEmpty()
                    )
                    onFinished(list.isNotEmpty())
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isChecking = false)
                    onFinished(false) // Direct submits if error occurs
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isChecking = false)
                    onFinished(false)
                }
            }
        }
    }

    fun setSkipCheck(skip: Boolean) {
        _uiState.value = _uiState.value.copy(skipCheck = skip)
    }

    fun resetState() {
        _uiState.value = DuplicateUiState()
    }
}
