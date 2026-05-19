package com.traffic_guard.ai.data

class NavigationRepositoryImpl : NavigationRepository {

    override suspend fun getRoute(start: MapLatLng, end: MapLatLng): Result<RoutePath> {
        // Generate coordinates connecting start and end
        val points = generatePointsBetween(start, end)
        val route = RoutePath(
            points = points,
            distanceMeters = 8500,
            durationSeconds = 960,
            isHazardSegment = false,
            summary = "Via Srinagar Highway"
        )
        return Result.Success(route)
    }

    override suspend fun getAlternatives(start: MapLatLng, end: MapLatLng): Result<List<RoutePath>> {
        val alt1Points = generatePointsBetween(start, end, offset = 0.005)
        val alt1 = RoutePath(
            points = alt1Points,
            distanceMeters = 9200,
            durationSeconds = 1100,
            isHazardSegment = false,
            summary = "Via Jinnah Avenue (Safer Route)"
        )

        val alt2Points = generatePointsBetween(start, end, offset = -0.004)
        val alt2 = RoutePath(
            points = alt2Points,
            distanceMeters = 7900,
            durationSeconds = 850,
            isHazardSegment = true, // Hazard detected on this alternative path
            summary = "Via Islamabad Expressway (Hazard segments!)"
        )

        return Result.Success(listOf(alt1, alt2))
    }

    private fun generatePointsBetween(
        start: MapLatLng,
        end: MapLatLng,
        offset: Double = 0.0
    ): List<MapLatLng> {
        val steps = 20
        val points = mutableListOf<MapLatLng>()
        for (i in 0..steps) {
            val fraction = i.toDouble() / steps
            val lat = start.latitude + (end.latitude - start.latitude) * fraction
            val lng = start.longitude + (end.longitude - start.longitude) * fraction

            // Add simple sine wave offset curve to simulate road curves
            val waveOffset = Math.sin(fraction * Math.PI) * offset
            points.add(MapLatLng(lat + waveOffset, lng + waveOffset))
        }
        return points
    }
}
