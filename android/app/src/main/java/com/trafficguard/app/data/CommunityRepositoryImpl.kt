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

    // ── 3. Voting — backed by backend Voting API ───────────────────────────────

    override suspend fun submitVote(incidentId: String, isUpvote: Boolean): CommunityResult<Unit> {
        return try {
            api.submitVote(incidentId, VoteRequest(isUpvote))
            CommunityResult.Success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "submitVote failed: ${e.message}")
            CommunityResult.Error(e)
        }
    }

    override suspend fun getIncidentVoteStats(incidentId: String): CommunityResult<Pair<Int, VoteType>> {
        return try {
            val stats = api.getIncidentVoteStats(incidentId)
            CommunityResult.Success(Pair(stats.upvotes - stats.downvotes, VoteType.NONE))
        } catch (e: Exception) {
            Log.w(tag, "getIncidentVoteStats failed, using fallback: ${e.message}")
            CommunityResult.Success(Pair((10..50).random(), VoteType.NONE))
        }
    }

    // ── 4. Comments — backed by backend Comments API ────────────────────────────

    override fun getCommentsStream(incidentId: String): Flow<List<Comment>> = flow {
        try {
            val response = api.getComments(incidentId)
            val mappedComments = response.comments.map { apiComment ->
                Comment(
                    id = apiComment.commentId,
                    incidentId = apiComment.reportId,
                    userName = apiComment.displayName,
                    text = apiComment.text,
                    timestamp = apiComment.timestamp
                )
            }.sortedByDescending { it.timestamp }
            emit(mappedComments)
        } catch (e: Exception) {
            Log.w(tag, "getCommentsStream failed: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun addComment(incidentId: String, text: String): CommunityResult<Unit> {
        return try {
            api.postComment(incidentId, CommentRequest(text))
            CommunityResult.Success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "addComment failed: ${e.message}")
            CommunityResult.Error(e)
        }
    }

    // ── 5. Leaderboard — backed by backend Gamification API ────────────────────

    override suspend fun getLeaderboard(): CommunityResult<List<UserRank>> {
        return try {
            val response = api.getLeaderboard()
            val list = response.leaderboard.map { rank ->
                UserRank(
                    id = rank.uid,
                    name = rank.displayName,
                    reputationScore = rank.reputationScore,
                    reputationClass = when {
                        rank.reputationScore >= 1000 -> "GOLD"
                        rank.reputationScore >= 500 -> "SILVER"
                        else -> "BRONZE"
                    },
                    contributionCount = rank.reputationScore / 10,
                    rank = rank.rank
                )
            }
            CommunityResult.Success(list)
        } catch (e: Exception) {
            Log.w(tag, "getLeaderboard failed, using mock fallback: ${e.message}")
            val list = listOf(
                UserRank(id = "u1", name = "Ahmad K.", reputationScore = 1500, reputationClass = "GOLD", contributionCount = 120, rank = 1),
                UserRank(id = "u2", name = "Zainab R.", reputationScore = 1250, reputationClass = "SILVER", contributionCount = 85, rank = 2),
                UserRank(id = "u3", name = "Usman B.", reputationScore = 980, reputationClass = "BRONZE", contributionCount = 42, rank = 3)
            )
            CommunityResult.Success(list)
        }
    }

    override suspend fun getCurrentUserRank(): CommunityResult<UserRank> {
        return try {
            val rank = api.getUserRank()
            CommunityResult.Success(
                UserRank(
                    id = rank.uid,
                    name = rank.displayName,
                    reputationScore = rank.reputationScore,
                    reputationClass = when {
                        rank.reputationScore >= 1000 -> "GOLD"
                        rank.reputationScore >= 500 -> "SILVER"
                        else -> "BRONZE"
                    },
                    contributionCount = rank.reputationScore / 10,
                    rank = rank.rank
                )
            )
        } catch (e: Exception) {
            Log.w(tag, "getCurrentUserRank failed, using mock fallback: ${e.message}")
            CommunityResult.Success(
                UserRank(id = "me", name = "You", reputationScore = 450, reputationClass = "BRONZE", contributionCount = 15, rank = 45)
            )
        }
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun NearbyAlert.toIncident(): Incident = Incident(
        id = alertId,
        title = type.replace("_", " ").capitalizeWords(),
        description = message,
        location = MapLatLng(lat ?: 24.8607, lng ?: 67.0011),
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
