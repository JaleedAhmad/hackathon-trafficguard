package com.traffic_guard.ai.data

import kotlinx.coroutines.flow.Flow

sealed class CommunityResult<out T> {
    data class Success<out T>(val data: T) : CommunityResult<T>()
    data class Error(val exception: Exception) : CommunityResult<Nothing>()
}

interface CommunityRepository {
    suspend fun getAlertsFeed(
        limit: Int = 10,
        offset: Int = 0,
        filter: String = "ALL",
        title: String? = null,
        date: String? = null
    ): CommunityResult<List<Incident>>
    suspend fun getIncidentDetails(incidentId: String): CommunityResult<Incident>
    
    // Voting
    suspend fun submitVote(incidentId: String, isUpvote: Boolean): CommunityResult<Unit>
    suspend fun getIncidentVoteStats(incidentId: String): CommunityResult<Pair<Int, VoteType>>
    
    // Comments
    fun getCommentsLiveStream(incidentId: String): Flow<Comment>
    suspend fun getCommentsPage(incidentId: String, limit: Int = 100, offset: Int = 0): CommunityResult<List<Comment>>
    suspend fun addComment(incidentId: String, text: String): CommunityResult<Unit>
    
    // Reputation
    suspend fun getLeaderboard(): CommunityResult<List<UserRank>>
    suspend fun getCurrentUserRank(): CommunityResult<UserRank>
}

