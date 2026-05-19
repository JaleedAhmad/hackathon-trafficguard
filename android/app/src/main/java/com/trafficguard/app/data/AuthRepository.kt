package com.traffic_guard.ai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    suspend fun signInAnonymously(): Result<UserProfile>
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun signUpWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun sendOtpCode(phoneNumber: String): Result<String>
    suspend fun verifyOtpCode(verificationId: String, code: String): Result<UserProfile>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut()
}

class AuthRepositoryImpl : AuthRepository {
    
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: Flow<UserProfile?> = _currentUser.asStateFlow()

    // Store dummy verification states
    private var activeOtpVerificationId: String? = null
    private var targetPhoneNumber: String? = null

    override suspend fun signInAnonymously(): Result<UserProfile> {
        val user = UserProfile(
            uid = "anon_${System.currentTimeMillis()}",
            email = null,
            phoneNumber = null,
            displayName = "Guest User",
            isAnonymous = true
        )
        _currentUser.value = user
        return Result.Success(user)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        if (password.length < 8) {
            return Result.Error(Exception("Password must be at least 8 characters"))
        }
        val user = UserProfile(
            uid = "user_${email.hashCode()}",
            email = email,
            phoneNumber = null,
            displayName = email.substringBefore("@"),
            isAnonymous = false
        )
        _currentUser.value = user
        return Result.Success(user)
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserProfile> {
        if (password.length < 8) {
            return Result.Error(Exception("Password must be at least 8 characters"))
        }
        val user = UserProfile(
            uid = "user_${email.hashCode()}",
            email = email,
            phoneNumber = null,
            displayName = email.substringBefore("@"),
            isAnonymous = false
        )
        _currentUser.value = user
        return Result.Success(user)
    }

    override suspend fun sendOtpCode(phoneNumber: String): Result<String> {
        if (phoneNumber.length < 10) {
            return Result.Error(Exception("Invalid phone number format"))
        }
        activeOtpVerificationId = "verify_${System.currentTimeMillis()}"
        targetPhoneNumber = phoneNumber
        return Result.Success(activeOtpVerificationId!!)
    }

    override suspend fun verifyOtpCode(verificationId: String, code: String): Result<UserProfile> {
        if (code != "123456") { // 123456 represents our standard debug verification code
            return Result.Error(Exception("Incorrect verification OTP code"))
        }
        val user = UserProfile(
            uid = "phone_${System.currentTimeMillis()}",
            email = null,
            phoneNumber = targetPhoneNumber,
            displayName = "Mobile User",
            isAnonymous = false
        )
        _currentUser.value = user
        return Result.Success(user)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (!email.contains("@")) {
            return Result.Error(Exception("Invalid email format"))
        }
        return Result.Success(Unit)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }
}
