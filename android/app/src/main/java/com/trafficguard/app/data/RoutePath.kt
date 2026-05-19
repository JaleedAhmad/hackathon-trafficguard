package com.traffic_guard.ai.data

import kotlinx.serialization.Serializable

@Serializable
data class RoutePath(
    val points: List<MapLatLng>,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val isHazardSegment: Boolean = false,
    val summary: String = ""
)

@Serializable
data class GuidanceStep(
    val instruction: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val maneuver: String = "straight" // straight, turn-left, turn-right, round-about
)
