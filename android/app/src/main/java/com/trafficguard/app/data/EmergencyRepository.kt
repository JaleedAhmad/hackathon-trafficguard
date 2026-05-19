package com.traffic_guard.ai.data

interface EmergencyRepository {
    suspend fun triggerSos(location: MapLatLng, notifyContacts: Boolean): AppResult<Boolean>
    suspend fun getNearbyEmergencyCenters(currentLocation: MapLatLng): AppResult<List<EmergencyCenter>>
    suspend fun cancelSos(): AppResult<Boolean>
    fun getEmergencyContacts(): List<String>
}
