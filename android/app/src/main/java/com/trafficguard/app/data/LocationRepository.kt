package com.traffic_guard.ai.data

import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    val locationFlow: Flow<MapLatLng>
    val speedFlow: Flow<Float>
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
