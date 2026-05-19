package com.traffic_guard.ai.data

import kotlinx.coroutines.delay

class EmergencyRepositoryImpl : EmergencyRepository {
    override suspend fun triggerSos(location: MapLatLng, notifyContacts: Boolean): AppResult<Boolean> {
        delay(1000) // Simulate network delay
        // Simulating offline fallback success
        return AppResult.Success(true)
    }

    override suspend fun getNearbyEmergencyCenters(currentLocation: MapLatLng): AppResult<List<EmergencyCenter>> {
        delay(800)
        val mockCenters = listOf(
            EmergencyCenter(id = "h1", name = "City General Hospital", address = "123 Health Ave", type = "HOSPITAL", distanceMeters = 1500, estimatedMinutes = 5, contactNumber = "111-222-3333"),
            EmergencyCenter(id = "s1", name = "Community Safe Shelter", address = "456 Safety Blvd", type = "SHELTER", distanceMeters = 3000, estimatedMinutes = 8, contactNumber = "555-000-1111")
        )
        return AppResult.Success(mockCenters)
    }

    override suspend fun cancelSos(): AppResult<Boolean> {
        delay(500)
        return AppResult.Success(true)
    }

    override fun getEmergencyContacts(): List<String> {
        return listOf("+1234567890", "+0987654321")
    }
}
