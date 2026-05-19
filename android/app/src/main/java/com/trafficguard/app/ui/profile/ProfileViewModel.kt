package com.traffic_guard.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.Badge
import com.traffic_guard.ai.data.ContributorStats
import com.traffic_guard.ai.data.ProfileRepository
import com.traffic_guard.ai.data.NearbyAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userStats: ContributorStats? = null,
    val badgeList: List<Badge> = emptyList(),
    val username: String = "Driver",
    val age: Int = 25,
    val gender: String = "Male",
    val photoUrl: String = "",
    val userReports: List<NearbyAlert> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
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
            
            val profileResult = profileRepository.getUserProfile()
            val statsResult = profileRepository.getContributorStats()
            val badgesResult = profileRepository.getBadges()
            val reportsResult = profileRepository.getUserReports()
            
            if (profileResult is com.traffic_guard.ai.data.AppResult.Success && 
                statsResult is com.traffic_guard.ai.data.AppResult.Success && 
                badgesResult is com.traffic_guard.ai.data.AppResult.Success &&
                reportsResult is com.traffic_guard.ai.data.AppResult.Success) {
                
                val profile = profileResult.data
                _uiState.update {
                    it.copy(
                        username = profile.displayName,
                        age = profile.age,
                        gender = profile.gender,
                        photoUrl = profile.photoUrl,
                        userStats = statsResult.data,
                        badgeList = badgesResult.data ?: emptyList(),
                        userReports = reportsResult.data ?: emptyList(),
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

    fun saveProfile(
        name: String,
        age: Int,
        gender: String,
        photoUrl: String,
        localBytes: ByteArray?,
        mimeType: String?,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            var finalPhotoUrl = photoUrl
            
            // 1. Upload picture first if selected locally
            if (localBytes != null && mimeType != null) {
                val uploadResult = profileRepository.uploadProfilePicture(localBytes, mimeType)
                if (uploadResult is com.traffic_guard.ai.data.AppResult.Success) {
                    finalPhotoUrl = uploadResult.data
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    onComplete(false)
                    return@launch
                }
            }
            
            // 2. Save profile updates
            val updateResult = profileRepository.updateUserProfile(name, age, gender, finalPhotoUrl)
            if (updateResult is com.traffic_guard.ai.data.AppResult.Success) {
                _uiState.update {
                    it.copy(
                        username = name,
                        age = age,
                        gender = gender,
                        photoUrl = finalPhotoUrl,
                        isSaving = false
                    )
                }
                onComplete(true)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                onComplete(false)
            }
        }
    }

    fun updateProfile(name: String, age: Int, gender: String, photoUrl: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = profileRepository.updateUserProfile(name, age, gender, photoUrl)
            if (result is com.traffic_guard.ai.data.AppResult.Success) {
                _uiState.update {
                    it.copy(
                        username = name,
                        age = age,
                        gender = gender,
                        photoUrl = photoUrl,
                        isSaving = false
                    )
                }
                onComplete(true)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                onComplete(false)
            }
        }
    }

    fun uploadProfilePicture(fileBytes: ByteArray, mimeType: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = profileRepository.uploadProfilePicture(fileBytes, mimeType)
            if (result is com.traffic_guard.ai.data.AppResult.Success) {
                val url = result.data
                _uiState.update { it.copy(photoUrl = url, isSaving = false) }
                onComplete(url)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                onComplete(null)
            }
        }
    }
}
