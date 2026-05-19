package com.traffic_guard.ai.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.CommunityRepository
import com.traffic_guard.ai.data.Incident
import com.traffic_guard.ai.data.VoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertDetailUiState(
    val incident: Incident? = null,
    val totalVotes: Int = 0,
    val userVoteState: VoteType = VoteType.NONE,
    val isLoading: Boolean = true,
    val isSubmittingVote: Boolean = false,
    val error: String? = null
)

class AlertDetailViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertDetailUiState())
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    fun loadIncidentDetails(incidentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val incidentResult = communityRepository.getIncidentDetails(incidentId)
            val statsResult = communityRepository.getIncidentVoteStats(incidentId)
            
            if (incidentResult is com.traffic_guard.ai.data.CommunityResult.Success && statsResult is com.traffic_guard.ai.data.CommunityResult.Success) {
                _uiState.update { 
                    it.copy(
                        incident = incidentResult.data,
                        totalVotes = statsResult.data.first,
                        userVoteState = statsResult.data.second,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load details."
                    ) 
                }
            }
        }
    }

    fun submitVote(incidentId: String, isUpvote: Boolean) {
        if (_uiState.value.isSubmittingVote) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingVote = true) }
            
            // Optimistic update
            val previousVote = _uiState.value.userVoteState
            val newVote = if (isUpvote) VoteType.UPVOTE else VoteType.DOWNVOTE
            
            _uiState.update { it.copy(userVoteState = newVote) }
            
            val result = communityRepository.submitVote(incidentId, isUpvote)
            if (result is com.traffic_guard.ai.data.CommunityResult.Error) {
                // Revert
                _uiState.update { it.copy(userVoteState = previousVote) }
            } else {
                // Reload stats
                val statsResult = communityRepository.getIncidentVoteStats(incidentId)
                if (statsResult is com.traffic_guard.ai.data.CommunityResult.Success) {
                    _uiState.update { 
                        it.copy(
                            totalVotes = statsResult.data.first
                        )
                    }
                }
            }
            _uiState.update { it.copy(isSubmittingVote = false) }
        }
    }
}
