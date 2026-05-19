package com.traffic_guard.ai.data

interface ProfileRepository {
    suspend fun getContributorStats(): AppResult<ContributorStats>
    suspend fun getBadges(): AppResult<List<Badge>>
    suspend fun getSettings(): AppResult<SettingsState>
    suspend fun updateSettings(settings: SettingsState): AppResult<Unit>

    // New profile operations
    suspend fun getUserProfile(): AppResult<ApiUserProfile>
    suspend fun updateUserProfile(name: String, age: Int, gender: String, photoUrl: String): AppResult<Unit>
    suspend fun uploadProfilePicture(fileBytes: ByteArray, mimeType: String): AppResult<String>
    suspend fun getUserReports(): AppResult<List<NearbyAlert>>
}
