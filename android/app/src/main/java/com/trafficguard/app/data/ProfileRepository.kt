package com.traffic_guard.ai.data

interface ProfileRepository {
    suspend fun getContributorStats(): AppResult<ContributorStats>
    suspend fun getBadges(): AppResult<List<Badge>>
    suspend fun getSettings(): AppResult<SettingsState>
    suspend fun updateSettings(settings: SettingsState): AppResult<Unit>
}
