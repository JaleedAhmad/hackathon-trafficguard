package com.traffic_guard.ai.data

import kotlinx.serialization.Serializable

@Serializable
enum class IncidentType {
    FLOOD,
    TRAFFIC,
    ACCIDENT,
    WEATHER
}

@Serializable
data class MapLatLng(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class Incident(
    val id: String,
    val title: String,
    val description: String,
    val location: MapLatLng,
    val severity: Int, // 1 to 5
    val type: IncidentType,
    val timestamp: Long = System.currentTimeMillis()
)
