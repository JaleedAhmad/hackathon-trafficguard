package com.traffic_guard.ai.data

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    val incidentId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userReputationClass: String = "BRONZE",
    val text: String = "",
    val timestamp: Long = 0L,
    val isPendingSync: Boolean = false
)

@Serializable
data class UserRank(
    val id: String = "",
    val name: String = "",
    val reputationScore: Int = 0,
    val reputationClass: String = "BRONZE",
    val contributionCount: Int = 0,
    val rank: Int = 0
)

@Serializable
data class Vote(
    val incidentId: String = "",
    val userId: String = "",
    val isUpvote: Boolean = true, // true = confirm, false = report cleared
    val timestamp: Long = 0L
)

enum class VoteType {
    NONE, UPVOTE, DOWNVOTE
}
