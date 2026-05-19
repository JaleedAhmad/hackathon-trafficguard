package com.traffic_guard.ai.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class CommunityRepositoryImpl : CommunityRepository {
    
    private val mockLocation = MapLatLng(33.0, 73.0)

    private val mockAlerts = mutableListOf(
        Incident(id = "1", title = "Flooding on Srinagar Highway", description = "Water level is rising.", severity = 5, type = IncidentType.FLOOD, location = mockLocation),
        Incident(id = "2", title = "Accident near G-11", description = "Two cars involved, lane blocked.", severity = 4, type = IncidentType.ACCIDENT, location = mockLocation),
        Incident(id = "3", title = "Heavy Traffic F-8", description = "Slow moving traffic due to construction.", severity = 3, type = IncidentType.TRAFFIC, location = mockLocation)
    )

    private val mockComments = mutableListOf(
        Comment(id = "c1", incidentId = "1", userName = "Ali", text = "Still flooded, avoid this route.", timestamp = System.currentTimeMillis() - 100000),
        Comment(id = "c2", incidentId = "1", userName = "Sara", text = "Police has arrived.", timestamp = System.currentTimeMillis() - 50000)
    )

    override suspend fun getAlertsFeed(limit: Int, offset: Int, filter: String): CommunityResult<List<Incident>> {
        delay(500) // Simulate network delay
        val filtered = if (filter == "ALL") mockAlerts else mockAlerts.filter { it.type.name == filter }
        val paginated = filtered.drop(offset).take(limit)
        return CommunityResult.Success(paginated)
    }

    override suspend fun getIncidentDetails(incidentId: String): CommunityResult<Incident> {
        delay(300)
        val incident = mockAlerts.find { it.id == incidentId }
        return if (incident != null) CommunityResult.Success(incident) else CommunityResult.Error(Exception("Not found"))
    }

    override suspend fun submitVote(incidentId: String, isUpvote: Boolean): CommunityResult<Unit> {
        delay(400)
        return CommunityResult.Success(Unit)
    }

    override suspend fun getIncidentVoteStats(incidentId: String): CommunityResult<Pair<Int, VoteType>> {
        delay(200)
        // Mock data
        return CommunityResult.Success(Pair((10..50).random(), VoteType.NONE))
    }

    override fun getCommentsStream(incidentId: String): Flow<List<Comment>> = flow {
        // Emit current comments
        val filtered = mockComments.filter { it.incidentId == incidentId }.sortedByDescending { it.timestamp }
        emit(filtered)
    }

    override suspend fun addComment(incidentId: String, text: String): CommunityResult<Unit> {
        delay(400)
        mockComments.add(
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

    override suspend fun getLeaderboard(): CommunityResult<List<UserRank>> {
        delay(600)
        val mockLeaderboard = listOf(
            UserRank(id = "u1", name = "Ahmad K.", reputationScore = 1500, reputationClass = "GOLD", contributionCount = 120, rank = 1),
            UserRank(id = "u2", name = "Zainab R.", reputationScore = 1250, reputationClass = "SILVER", contributionCount = 85, rank = 2),
            UserRank(id = "u3", name = "Usman B.", reputationScore = 980, reputationClass = "BRONZE", contributionCount = 42, rank = 3)
        )
        return CommunityResult.Success(mockLeaderboard)
    }

    override suspend fun getCurrentUserRank(): CommunityResult<UserRank> {
        delay(200)
        return CommunityResult.Success(UserRank(id = "me", name = "You", reputationScore = 450, reputationClass = "BRONZE", contributionCount = 15, rank = 45))
    }
}

