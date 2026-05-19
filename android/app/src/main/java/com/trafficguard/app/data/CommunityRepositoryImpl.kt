package com.traffic_guard.ai.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class CommunityRepositoryImpl : CommunityRepository {

    private val tag = "CommunityRepository"
    private val api = TrafficGuardApiClient.service

    // Local comment store (not yet a backend endpoint)
    private val localComments = mutableListOf(
        Comment(id = "c1", incidentId = "1", userName = "Ali", text = "Still flooded, avoid this route.", timestamp = System.currentTimeMillis() - 100000),
        Comment(id = "c2", incidentId = "1", userName = "Sara", text = "Police has arrived.", timestamp = System.currentTimeMillis() - 50000)
    )

    // ── 1. Alerts Feed — backed by GET /alerts/nearby ─────────────────────────
    //    Falls back to GET /crisis/current when /alerts/nearby returns nothing.

    override suspend fun getAlertsFeed(
        limit: Int,
        offset: Int,
        filter: String
    ): CommunityResult<List<Incident>> {
        return try {
            val response = api.getNearbyAlerts()
            var incidents = response.alerts.map { it.toIncident() }

            if (filter != "ALL") {
                incidents = incidents.filter { it.type.name == filter }
            }

            val page = incidents.drop(offset).take(limit)

            // If empty, supplement from /crisis/current
            if (page.isEmpty()) {
                val crisis = api.getCurrentCrisis()
                val crisisIncident = crisis.toIncident()
                val supplemented = listOfNotNull(crisisIncident)
                    .filter { filter == "ALL" || it.type.name == filter }
                    .drop(offset).take(limit)
                return CommunityResult.Success(supplemented)
            }

            CommunityResult.Success(page)
        } catch (e: Exception) {
            Log.w(tag, "getAlertsFeed failed, using fallback: ${e.message}")
            CommunityResult.Success(fallbackAlerts(filter, offset, limit))
        }
    }

    // ── 2. Incident Detail — looks up by ID across feed + crisis ──────────────

    override suspend fun getIncidentDetails(incidentId: String): CommunityResult<Incident> {
        return try {
            val feedResult = getAlertsFeed(limit = 50, offset = 0, filter = "ALL")
            val incident = (feedResult as? CommunityResult.Success)?.data?.find { it.id == incidentId }
            if (incident != null) {
                CommunityResult.Success(incident)
            } else {
                // Try pulling from current crisis
                val crisis = api.getCurrentCrisis()
                if (crisis.crisisId == incidentId) {
                    CommunityResult.Success(crisis.toIncident())
                } else {
                    CommunityResult.Error(Exception("Incident not found"))
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "getIncidentDetails failed: ${e.message}")
            CommunityResult.Error(Exception("Could not load incident details"))
        }
    }

    // ── 3. Voting — local optimistic (no dedicated backend endpoint yet) ───────

    override suspend fun submitVote(incidentId: String, isUpvote: Boolean): CommunityResult<Unit> {
        return CommunityResult.Success(Unit)
    }

    override suspend fun getIncidentVoteStats(incidentId: String): CommunityResult<Pair<Int, VoteType>> {
        return CommunityResult.Success(Pair((10..50).random(), VoteType.NONE))
    }

    // ── 4. Comments — local store ─────────────────────────────────────────────

    override fun getCommentsStream(incidentId: String): Flow<List<Comment>> = flow {
        val filtered = localComments
            .filter { it.incidentId == incidentId }
            .sortedByDescending { it.timestamp }
        emit(filtered)
    }

    override suspend fun addComment(incidentId: String, text: String): CommunityResult<Unit> {
        localComments.add(
            Comment(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                userName = "You",
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
        return CommunityResult.Success(Unit)
    }

    // ── 5. Leaderboard — local mock (no backend endpoint) ─────────────────────

    override suspend fun getLeaderboard(): CommunityResult<List<UserRank>> {
        val list = listOf(
            UserRank(id = "u1", name = "Ahmad K.", reputationScore = 1500, reputationClass = "GOLD", contributionCount = 120, rank = 1),
            UserRank(id = "u2", name = "Zainab R.", reputationScore = 1250, reputationClass = "SILVER", contributionCount = 85, rank = 2),
            UserRank(id = "u3", name = "Usman B.", reputationScore = 980, reputationClass = "BRONZE", contributionCount = 42, rank = 3)
        )
        return CommunityResult.Success(list)
    }

    override suspend fun getCurrentUserRank(): CommunityResult<UserRank> {
        return CommunityResult.Success(
            UserRank(id = "me", name = "You", reputationScore = 450, reputationClass = "BRONZE", contributionCount = 15, rank = 45)
        )
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun NearbyAlert.toIncident(): Incident = Incident(
        id = alertId,
        title = type.replace("_", " ").capitalizeWords(),
        description = message,
        location = MapLatLng(24.8607, 67.0011), // default; real location would need reverse geocoding
        severity = severityValue(severity),
        type = mapIncidentType(type)
    )

    private fun CurrentCrisisResponse.toIncident(): Incident = Incident(
        id = crisisId,
        title = type.replace("_", " ").capitalizeWords(),
        description = "Active crisis: $location — confidence $confidence%",
        location = MapLatLng(lat, lng),
        severity = severityValue(severity),
        type = mapIncidentType(type)
    )

    private fun severityValue(severity: String): Int = when (severity.lowercase()) {
        "critical" -> 5
        "high" -> 4
        "medium" -> 3
        "low" -> 2
        else -> 1
    }

    private fun mapIncidentType(type: String): IncidentType = when {
        type.contains("flood", ignoreCase = true) -> IncidentType.FLOOD
        type.contains("accident", ignoreCase = true) || type.contains("crash", ignoreCase = true) -> IncidentType.ACCIDENT
        else -> IncidentType.TRAFFIC
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    // ── Static fallback data (only used when backend is unreachable) ──────────

    private val defaultLocation = MapLatLng(33.0, 73.0)

    private fun fallbackAlerts(filter: String, offset: Int, limit: Int): List<Incident> {
        val all = mutableListOf(
            Incident("1", "Flooding on Srinagar Highway", "Water level is rising.", defaultLocation, 5, IncidentType.FLOOD),
            Incident("2", "Accident near G-11", "Two cars involved, lane blocked.", defaultLocation, 4, IncidentType.ACCIDENT),
            Incident("3", "Heavy Traffic F-8", "Slow moving traffic due to construction.", defaultLocation, 3, IncidentType.TRAFFIC)
        )
        val filtered = if (filter == "ALL") all else all.filter { it.type.name == filter }
        return filtered.drop(offset).take(limit)
    }
}
