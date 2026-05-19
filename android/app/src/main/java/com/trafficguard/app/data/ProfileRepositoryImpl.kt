package com.traffic_guard.ai.data

import kotlinx.coroutines.delay

class ProfileRepositoryImpl : ProfileRepository {
    
    private var currentSettings = SettingsState()

    override suspend fun getContributorStats(): AppResult<ContributorStats> {
        delay(400)
        return AppResult.Success(
            ContributorStats(
                userId = "me",
                totalReports = 42,
                verifiedReports = 38,
                peopleHelped = 1500,
                currentStreakDays = 7
            )
        )
    }

    override suspend fun getBadges(): AppResult<List<Badge>> {
        delay(400)
        val mockBadges = listOf(
            Badge(id = "b1", name = "First Report", description = "Reported your first hazard.", isUnlocked = true),
            Badge(id = "b2", name = "Trust Guardian", description = "Verified 50 reports.", isUnlocked = false),
            Badge(id = "b3", name = "7-Day Streak", description = "Active for 7 days in a row.", isUnlocked = true)
        )
        return AppResult.Success(mockBadges)
    }

    override suspend fun getSettings(): AppResult<SettingsState> {
        delay(200)
        return AppResult.Success(currentSettings)
    }

    override suspend fun updateSettings(settings: SettingsState): AppResult<Unit> {
        delay(300)
        currentSettings = settings
        return AppResult.Success(Unit)
    }
}
