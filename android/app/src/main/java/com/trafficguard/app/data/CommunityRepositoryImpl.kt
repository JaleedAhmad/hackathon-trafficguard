package com.traffic_guard.ai.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import com.google.gson.JsonParser
import java.util.UUID

class CommunityRepositoryImpl : CommunityRepository {

    private val tag = "CommunityRepository"
    private val api = TrafficGuardApiClient.service
    private val activeSockets = java.util.concurrent.ConcurrentHashMap<String, WebSocket>()

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
        filter: String,
        title: String?,
        date: String?
    ): CommunityResult<List<Incident>> {
        return try {
            val response = api.getNearbyAlerts(title = title, date = date)
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

    override fun getCommentsLiveStream(incidentId: String): Flow<Comment> = callbackFlow {
        val client = OkHttpClient()
        val wsUrl = TrafficGuardApiClient.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "ws/discussion/$incidentId"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                activeSockets[incidentId] = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JsonParser.parseString(text).asJsonObject
                    if (json.has("comment")) {
                        val commentObj = json.getAsJsonObject("comment")
                        val newComment = Comment(
                            id = commentObj.get("comment_id").asString,
                            incidentId = commentObj.get("report_id").asString,
                            userId = if (commentObj.has("user_id")) commentObj.get("user_id").asString else "",
                            userName = commentObj.get("display_name").asString,
                            text = commentObj.get("text").asString,
                            timestamp = commentObj.get("timestamp").asLong
                        )
                        trySend(newComment)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "WS json parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WS connection failure: ${t.message}")
                activeSockets.remove(incidentId)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                activeSockets.remove(incidentId)
            }
        }

        val webSocket = client.newWebSocket(request, listener)

        awaitClose {
            activeSockets.remove(incidentId)
            webSocket.close(1000, "Flow closed")
        }
    }

    override suspend fun getCommentsPage(incidentId: String, limit: Int, offset: Int): CommunityResult<List<Comment>> {
        return try {
            val response = api.getComments(incidentId, limit, offset)
            val mappedComments = response.comments.map { apiComment ->
                Comment(
                    id = apiComment.commentId,
                    incidentId = apiComment.reportId,
                    userId = apiComment.uid,
                    userName = apiComment.displayName,
                    text = apiComment.text,
                    timestamp = apiComment.timestamp
                )
            }.sortedByDescending { it.timestamp }
            CommunityResult.Success(mappedComments)
        } catch (e: Exception) {
            Log.w(tag, "getCommentsPage failed: ${e.message}")
            CommunityResult.Error(e)
        }
    }

    override suspend fun addComment(incidentId: String, text: String): CommunityResult<Unit> {
        return try {
            val socket = activeSockets[incidentId]
            if (socket != null) {
                var token: String? = null
                try {
                    token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        ?.getIdToken(false)
                        ?.await()
                        ?.token
                } catch (e: Exception) {
                    Log.w(tag, "Failed to get fresh token: ${e.message}")
                }
                
                val payload = com.google.gson.JsonObject().apply {
                    addProperty("text", text)
                    addProperty("token", token ?: "")
                }.toString()
                
                val success = socket.send(payload)
                if (success) {
                    CommunityResult.Success(Unit)
                } else {
                    api.postComment(incidentId, CommentRequest(text))
                    CommunityResult.Success(Unit)
                }
            } else {
                api.postComment(incidentId, CommentRequest(text))
                CommunityResult.Success(Unit)
            }
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

    private fun parseTimestamp(tsString: String?): Long {
        if (tsString.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            tsString.toLongOrNull() ?: java.time.Instant.parse(tsString).toEpochMilli()
        } catch (e: Exception) {
            try {
                val clean = tsString.replace(" ", "T")
                val instant = if (clean.endsWith("Z")) {
                    java.time.Instant.parse(clean)
                } else {
                    java.time.LocalDateTime.parse(clean).toInstant(java.time.ZoneOffset.UTC)
                }
                instant.toEpochMilli()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun NearbyAlert.toIncident(): Incident = Incident(
        id = alertId,
        title = type.replace("_", " ").capitalizeWords(),
        description = message,
        location = MapLatLng(lat ?: 24.8607, lng ?: 67.0011),
        severity = severityValue(severity),
        type = mapIncidentType(type),
        timestamp = parseTimestamp(timestamp),
        confirmations = confirmations ?: 0
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
        type.contains("weather", ignoreCase = true) -> IncidentType.WEATHER
        type.contains("other", ignoreCase = true) -> IncidentType.OTHER
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
