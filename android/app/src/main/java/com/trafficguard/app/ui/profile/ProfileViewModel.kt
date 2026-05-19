package com.traffic_guard.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.Badge
import com.traffic_guard.ai.data.ContributorStats
import com.traffic_guard.ai.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userStats: ContributorStats? = null,
    val badgeList: List<Badge> = emptyList(),
    val username: String = "Driver",
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val statsResult = profileRepository.getContributorStats()
            val badgesResult = profileRepository.getBadges()
            
            if (statsResult is com.traffic_guard.ai.data.AppResult.Success && badgesResult is com.traffic_guard.ai.data.AppResult.Success) {
                _uiState.update {
                    it.copy(
                        userStats = statsResult.data,
                        badgeList = badgesResult.data ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load profile data."
                    )
                }
            }
        }
    }
}
