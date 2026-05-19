package com.traffic_guard.ai.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepositoryImpl : ProfileRepository {
    
    private val tag = "ProfileRepository"
    private val api = TrafficGuardApiClient.service
    private var currentSettings = SettingsState()

    override suspend fun getContributorStats(): AppResult<ContributorStats> {
        return try {
            val response = api.getUserProfile()
            AppResult.Success(
                ContributorStats(
                    userId = response.uid,
                    totalReports = response.totalReports,
                    verifiedReports = (response.totalReports * 0.9).toInt(), // Visual mock multiplier
                    peopleHelped = response.totalReports * 45,
                    currentStreakDays = 5
                )
            )
        } catch (e: Exception) {
            Log.w(tag, "getContributorStats failed: ${e.message}")
            AppResult.Success(
                ContributorStats(
                    userId = "me",
                    totalReports = 0,
                    verifiedReports = 0,
                    peopleHelped = 0,
                    currentStreakDays = 1
                )
            )
        }
    }

    override suspend fun getBadges(): AppResult<List<Badge>> {
        val mockBadges = listOf(
            Badge(id = "b1", name = "First Report", description = "Reported your first hazard.", isUnlocked = true),
            Badge(id = "b2", name = "Trust Guardian", description = "Verified 50 reports.", isUnlocked = false),
            Badge(id = "b3", name = "7-Day Streak", description = "Active for 7 days in a row.", isUnlocked = true)
        )
        return AppResult.Success(mockBadges)
    }

    override suspend fun getSettings(): AppResult<SettingsState> {
        return AppResult.Success(currentSettings)
    }

    override suspend fun updateSettings(settings: SettingsState): AppResult<Unit> {
        currentSettings = settings
        return AppResult.Success(Unit)
    }

    override suspend fun getUserProfile(): AppResult<ApiUserProfile> {
        return try {
            val response = api.getUserProfile()
            AppResult.Success(response)
        } catch (e: Exception) {
            Log.w(tag, "getUserProfile failed: ${e.message}")
            AppResult.Error(e)
        }
    }

    override suspend fun updateUserProfile(name: String, age: Int, gender: String, photoUrl: String): AppResult<Unit> {
        return try {
            api.updateUserProfile(
                UpdateProfileRequest(
                    displayName = name,
                    age = age,
                    gender = gender,
                    photoUrl = photoUrl
                )
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "updateUserProfile failed: ${e.message}")
            AppResult.Error(e)
        }
    }

    override suspend fun uploadProfilePicture(fileBytes: ByteArray, mimeType: String): AppResult<String> {
        return try {
            val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "profile.jpg", requestFile)
            val response = api.uploadProfilePicture(part)
            if (response.status == "success") {
                AppResult.Success(response.photoUrl)
            } else {
                AppResult.Error(Exception("Upload status failed"))
            }
        } catch (e: Exception) {
            Log.w(tag, "uploadProfilePicture failed: ${e.message}")
            AppResult.Error(e)
        }
    }

    override suspend fun getUserReports(): AppResult<List<NearbyAlert>> {
        return try {
            val response = api.getUserReports()
            AppResult.Success(response.alerts)
        } catch (e: Exception) {
            Log.w(tag, "getUserReports failed: ${e.message}")
            AppResult.Error(e)
        }
    }
}
