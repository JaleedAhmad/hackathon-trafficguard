package com.traffic_guard.ai.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.CommunityRepository
import com.traffic_guard.ai.data.UserRank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReputationUiState(
    val leaderboardUsers: List<UserRank> = emptyList(),
    val currentUserRank: UserRank? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ReputationViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReputationUiState())
    val uiState: StateFlow<ReputationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val boardResult = communityRepository.getLeaderboard()
            val userResult = communityRepository.getCurrentUserRank()
            
            if (boardResult is com.traffic_guard.ai.data.CommunityResult.Success && userResult is com.traffic_guard.ai.data.CommunityResult.Success) {
                _uiState.update {
                    it.copy(
                        leaderboardUsers = boardResult.data,
                        currentUserRank = userResult.data,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load leaderboard."
                    )
                }
            }
        }
    }
}
