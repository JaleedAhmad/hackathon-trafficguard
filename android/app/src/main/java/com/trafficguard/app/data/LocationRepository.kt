package com.traffic_guard.ai.data

import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    val locationFlow: Flow<MapLatLng>
    val speedFlow: Flow<Float>
    val currentLocation: MapLatLng?
    fun isLocationEnabled(): Boolean
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
