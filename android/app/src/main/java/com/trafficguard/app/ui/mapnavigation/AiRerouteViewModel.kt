package com.traffic_guard.ai.ui.mapnavigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiRerouteUiState(
    val hazardDetected: Incident? = null,
    val proposedRoute: RoutePath? = null,
    val timeSavedMinutes: Int = 0,
    val showModal: Boolean = false,
    val isCalculatingDetour: Boolean = false
)

class AiRerouteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiRerouteUiState())
    val uiState: StateFlow<AiRerouteUiState> = _uiState.asStateFlow()

    private val api = TrafficGuardApiClient.service
    private var monitoringJob: Job? = null
    private var monitoredRoute: RoutePath? = null

    fun startRouteMonitoring(activeRoute: RoutePath, userLocation: MapLatLng?) {
        if (monitoredRoute == activeRoute) return
        monitoredRoute = activeRoute
        monitoringJob?.cancel()

        monitoringJob = viewModelScope.launch {
            Log.d("AiRerouteViewModel", "Started monitoring route: ${activeRoute.summary}")
            while (true) {
                delay(5000) // Poll every 5 seconds
                try {
                    val response = api.getNearbyAlerts(
                        lat = userLocation?.latitude ?: 33.6844,
                        lng = userLocation?.longitude ?: 73.0479
                    )
                    
                    val incidents = response.alerts.map { it.toIncident() }
                    
                    // Check if any incident is close to the active route
                    var foundHazard: Incident? = null
                    for (incident in incidents) {
                        for (pt in activeRoute.points) {
                            val dist = calculateDistance(
                                incident.location.latitude, incident.location.longitude,
                                pt.latitude, pt.longitude
                            )
                            if (dist <= 500.0) {
                                foundHazard = incident
                                break
                            }
                        }
                        if (foundHazard != null) break
                    }

                    if (foundHazard != null && _uiState.value.hazardDetected?.id != foundHazard.id) {
                        Log.i("AiRerouteViewModel", "Hazard detected on active route: ${foundHazard.title}")
                        triggerRerouteProposal(foundHazard, userLocation)
                    }
                } catch (e: Exception) {
                    Log.w("AiRerouteViewModel", "Failed to fetch active route updates: ${e.message}")
                }
            }
        }
    }

    fun stopRouteMonitoring() {
        monitoringJob?.cancel()
        monitoredRoute = null
    }

    private fun triggerRerouteProposal(hazard: Incident, userLocation: MapLatLng?) {
        _uiState.value = _uiState.value.copy(
            hazardDetected = hazard,
            isCalculatingDetour = true,
            showModal = true
        )

        viewModelScope.launch {
            try {
                // Fetch new route options bypassing this hazard segment
                val response = api.getRoute(
                    ApiRouteRequest(
                        sourceLat = userLocation?.latitude ?: 33.6844,
                        sourceLng = userLocation?.longitude ?: 73.0479,
                        destLat = monitoredRoute?.points?.last()?.latitude ?: 33.7220,
                        destLng = monitoredRoute?.points?.last()?.longitude ?: 73.0580
                    )
                )

                // Select the first alternative that is not a hazard segment
                val alternative = response.alternateRoutes.firstOrNull { !it.isHazardSegment }
                    ?: response.alternateRoutes.firstOrNull()
                    ?: response.activeRoute

                _uiState.value = _uiState.value.copy(
                    isCalculatingDetour = false,
                    proposedRoute = alternative.toRoutePath(),
                    timeSavedMinutes = if (hazard.severity > 3) 10 else 4
                )
            } catch (e: Exception) {
                Log.e("AiRerouteViewModel", "Failed to calculate detour route: ${e.message}")
                _uiState.value = _uiState.value.copy(isCalculatingDetour = false)
            }
        }
    }

    fun acceptDetour(onAccepted: (RoutePath) -> Unit) {
        _uiState.value.proposedRoute?.let {
            onAccepted(it)
        }
        dismissModal()
    }

    fun dismissModal() {
        _uiState.value = _uiState.value.copy(
            showModal = false
        )
    }

    private fun ApiRoutePath.toRoutePath(): RoutePath = RoutePath(
        points = points.map { MapLatLng(it.latitude, it.longitude) },
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        isHazardSegment = isHazardSegment,
        summary = "$summary\n$pros\n$cons"
    )

    private fun NearbyAlert.toIncident(): Incident = Incident(
        id = alertId,
        title = type.replace("_", " ").uppercase(),
        description = message,
        location = MapLatLng(lat ?: 33.7220, lng ?: 73.0580),
        severity = when (severity.lowercase()) {
            "critical" -> 5
            "high" -> 4
            "medium" -> 3
            else -> 2
        },
        type = when (type.lowercase()) {
            "flood", "water_logging", "rain" -> IncidentType.FLOOD
            "accident", "crash" -> IncidentType.ACCIDENT
            "road_blockage", "protest" -> IncidentType.TRAFFIC
            else -> IncidentType.TRAFFIC
        }
    )

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        stopRouteMonitoring()
    }
}

