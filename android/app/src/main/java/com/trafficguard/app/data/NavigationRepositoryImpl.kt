package com.traffic_guard.ai.data

import android.util.Log

class NavigationRepositoryImpl : NavigationRepository {

    private val api = TrafficGuardApiClient.service
    private var cachedAlternatives = listOf<RoutePath>()
    private var cachedStart: MapLatLng? = null
    private var cachedEnd: MapLatLng? = null

    override suspend fun getRoute(start: MapLatLng, end: MapLatLng): Result<RoutePath> {
        return try {
            val response = api.getRoute(
                ApiRouteRequest(
                    sourceLat = start.latitude,
                    sourceLng = start.longitude,
                    destLat = end.latitude,
                    destLng = end.longitude
                )
            )
            
            val activeRoute = response.activeRoute.toRoutePath()
            cachedAlternatives = response.alternateRoutes.map { it.toRoutePath() }
            cachedStart = start
            cachedEnd = end
            
            Result.Success(activeRoute)
        } catch (e: Exception) {
            Log.w("NavigationRepository", "getRoute failed: ${e.message}", e)
            Result.Error(e)
        }
    }

    override suspend fun getAlternatives(start: MapLatLng, end: MapLatLng): Result<List<RoutePath>> {
        if (cachedStart == start && cachedEnd == end) {
            return Result.Success(cachedAlternatives)
        }
        return try {
            val response = api.getRoute(
                ApiRouteRequest(
                    sourceLat = start.latitude,
                    sourceLng = start.longitude,
                    destLat = end.latitude,
                    destLng = end.longitude
                )
            )
            val alts = response.alternateRoutes.map { it.toRoutePath() }
            Result.Success(alts)
        } catch (e: Exception) {
            Log.w("NavigationRepository", "getAlternatives failed: ${e.message}", e)
            Result.Error(e)
        }
    }

    private fun ApiRoutePath.toRoutePath(): RoutePath = RoutePath(
        points = points.map { MapLatLng(it.latitude, it.longitude) },
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        isHazardSegment = isHazardSegment,
        summary = "$summary\n$pros\n$cons"
    )
}

