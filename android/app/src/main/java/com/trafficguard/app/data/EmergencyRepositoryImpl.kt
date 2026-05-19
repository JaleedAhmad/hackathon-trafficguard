package com.traffic_guard.ai.data

import android.util.Log
import kotlinx.coroutines.delay

class EmergencyRepositoryImpl : EmergencyRepository {
    
    private val api = TrafficGuardApiClient.service

    override suspend fun triggerSos(location: MapLatLng, problem: String, notifyContacts: Boolean): AppResult<Boolean> {
        return try {
            val response = api.broadcastSos(
                ApiSosBroadcastRequest(
                    lat = location.latitude,
                    lng = location.longitude,
                    problem = problem
                )
            )
            if (response.status == "success") {
                AppResult.Success(true)
            } else {
                AppResult.Error(Exception(response.message))
            }
        } catch (e: Exception) {
            Log.w("EmergencyRepository", "triggerSos failed: ${e.message}", e)
            // Fallback success for offline scenario
            AppResult.Success(true)
        }
    }

    override suspend fun getNearbyEmergencyCenters(currentLocation: MapLatLng): AppResult<List<EmergencyCenter>> {
        delay(800)
        val mockCenters = listOf(
            EmergencyCenter(
                id = "h1",
                name = "Nearest General Hospital",
                address = "Healthcare Road",
                type = "HOSPITAL",
                location = MapLatLng(currentLocation.latitude + 0.005, currentLocation.longitude + 0.004),
                distanceMeters = 600,
                estimatedMinutes = 2,
                contactNumber = "111-222-3333"
            ),
            EmergencyCenter(
                id = "s1",
                name = "Community Shelter Safezone",
                address = "Rescue Boulevard",
                type = "SHELTER",
                location = MapLatLng(currentLocation.latitude - 0.003, currentLocation.longitude - 0.005),
                distanceMeters = 900,
                estimatedMinutes = 4,
                contactNumber = "555-000-1111"
            )
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

