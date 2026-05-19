package com.traffic_guard.ai.data

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyCenter(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val type: String = "HOSPITAL", // HOSPITAL, SHELTER
    val location: MapLatLng = MapLatLng(0.0, 0.0),
    val distanceMeters: Int = 0,
    val estimatedMinutes: Int = 0,
    val contactNumber: String = ""
)

@Serializable
data class ContributorStats(
    val userId: String = "",
    val totalReports: Int = 0,
    val verifiedReports: Int = 0,
    val peopleHelped: Int = 0,
    val currentStreakDays: Int = 0
)

@Serializable
data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val isUnlocked: Boolean = false
)

enum class ThemeType {
    SYSTEM, LIGHT, DARK
}

data class SettingsState(
    val theme: ThemeType = ThemeType.SYSTEM,
    val dataSaverEnabled: Boolean = false,
    val alertsEnabled: Boolean = true,
    val fontSizeFactor: Float = 1.0f
)
