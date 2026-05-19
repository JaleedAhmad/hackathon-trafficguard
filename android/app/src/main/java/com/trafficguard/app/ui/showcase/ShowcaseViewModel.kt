package com.traffic_guard.ai.ui.showcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.AgentPipelineRepository
import com.traffic_guard.ai.data.AgentTraceResponse
import com.traffic_guard.ai.data.AgentTraceStep
import com.traffic_guard.ai.data.BaselineCompareResponse
import com.traffic_guard.ai.data.CurrentCrisisResponse
import com.traffic_guard.ai.data.Result
import com.traffic_guard.ai.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

data class ShowcaseUiState(
    val inputText: String = "",
    val inputError: String? = null,
    val isLoadingSimulated: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    // Live agent data
    val isLoadingTrace: Boolean = false,
    val agentTrace: AgentTraceResponse? = null,
    val traceError: String? = null,
    val isLoadingBaseline: Boolean = false,
    val baselineComparison: BaselineCompareResponse? = null,
    val baselineError: String? = null,
    val isLoadingCrisis: Boolean = false,
    val currentCrisis: CurrentCrisisResponse? = null,
    val crisisError: String? = null
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface ShowcaseEvent {
    data class InputTextChanged(val text: String) : ShowcaseEvent
    data class ThemeModeChanged(val mode: ThemeMode) : ShowcaseEvent
    data class SetLoadingState(val isLoading: Boolean) : ShowcaseEvent
    data object LoadAgentTrace : ShowcaseEvent
    data object LoadBaselineComparison : ShowcaseEvent
    data object LoadCurrentCrisis : ShowcaseEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ShowcaseViewModel : ViewModel() {

    private val repo = AgentPipelineRepository()

    private val _uiState = MutableStateFlow(ShowcaseUiState())
    val uiState: StateFlow<ShowcaseUiState> = _uiState.asStateFlow()

    fun onEvent(event: ShowcaseEvent) {
        when (event) {
            is ShowcaseEvent.InputTextChanged -> {
                val error = if (event.text.length < 3 && event.text.isNotEmpty())
                    "Input must be at least 3 characters" else null
                _uiState.value = _uiState.value.copy(inputText = event.text, inputError = error)
            }

            is ShowcaseEvent.ThemeModeChanged ->
                _uiState.value = _uiState.value.copy(themeMode = event.mode)

            is ShowcaseEvent.SetLoadingState ->
                _uiState.value = _uiState.value.copy(isLoadingSimulated = event.isLoading)

            is ShowcaseEvent.LoadAgentTrace -> loadAgentTrace()
            is ShowcaseEvent.LoadBaselineComparison -> loadBaselineComparison()
            is ShowcaseEvent.LoadCurrentCrisis -> loadCurrentCrisis()
        }
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private fun loadAgentTrace() {
        _uiState.value = _uiState.value.copy(isLoadingTrace = true, traceError = null)
        viewModelScope.launch {
            when (val result = repo.getAgentTrace()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingTrace = false,
                    agentTrace = result.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingTrace = false,
                    traceError = result.exception.message
                )
                else -> {}
            }
        }
    }

    private fun loadBaselineComparison() {
        _uiState.value = _uiState.value.copy(isLoadingBaseline = true, baselineError = null)
        viewModelScope.launch {
            when (val result = repo.getBaselineComparison()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingBaseline = false,
                    baselineComparison = result.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingBaseline = false,
                    baselineError = result.exception.message
                )
                else -> {}
            }
        }
    }

    private fun loadCurrentCrisis() {
        _uiState.value = _uiState.value.copy(isLoadingCrisis = true, crisisError = null)
        viewModelScope.launch {
            when (val result = repo.getCurrentCrisis()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingCrisis = false,
                    currentCrisis = result.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingCrisis = false,
                    crisisError = result.exception.message
                )
                else -> {}
            }
        }
    }

    // ── Convenience helpers for the UI ────────────────────────────────────────

    fun agentSteps(agentNumber: Int): List<AgentTraceStep> {
        val trace = _uiState.value.agentTrace ?: return emptyList()
        return when (agentNumber) {
            1 -> trace.agent1 ?: emptyList()
            2 -> trace.agent2 ?: emptyList()
            3 -> trace.agent3 ?: emptyList()
            4 -> trace.agent4 ?: emptyList()
            else -> emptyList()
        }
    }
}
