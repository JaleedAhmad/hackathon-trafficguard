package com.traffic_guard.ai.data

interface NavigationRepository {
    suspend fun getRoute(start: MapLatLng, end: MapLatLng): Result<RoutePath>
    suspend fun getAlternatives(start: MapLatLng, end: MapLatLng): Result<List<RoutePath>>
}
