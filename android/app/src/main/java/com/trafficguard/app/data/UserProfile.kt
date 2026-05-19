package com.traffic_guard.ai.data

data class UserProfile(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String?,
    val photoUrl: String? = null,
    val authProvider: String = "email",
    val isAnonymous: Boolean = false
)
