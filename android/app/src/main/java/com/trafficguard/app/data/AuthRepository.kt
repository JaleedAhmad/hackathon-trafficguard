package com.traffic_guard.ai.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// ─── Interface ────────────────────────────────────────────────────────────────

interface AuthRepository {
    /** Emits the current signed-in user, or null when signed out. */
    val currentUser: Flow<UserProfile?>

    /** Guest mode — no Firebase account, restricted access. */
    suspend fun signInAsGuest(): Result<UserProfile>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun signUpWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /** Returns a fresh Firebase ID token, forcing refresh when stale. */
    suspend fun getFreshIdToken(): String?

    suspend fun signOut()
}

// ─── Implementation ───────────────────────────────────────────────────────────

class AuthRepositoryImpl : AuthRepository {

    private val tag = "AuthRepository"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ── currentUser as a cold callbackFlow so callers always get updates ──────
    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val fb = firebaseAuth.currentUser
            trySend(fb?.toUserProfile(provider = detectProvider(fb)))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ── Guest ─────────────────────────────────────────────────────────────────

    override suspend fun signInAsGuest(): Result<UserProfile> {
        val guest = UserProfile(
            uid = "guest_${System.currentTimeMillis()}",
            email = null,
            phoneNumber = null,
            displayName = "Guest",
            isAnonymous = true,
            authProvider = "guest"
        )
        return Result.Success(guest)
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> =
        runAuthCatching("email") {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            val profile = user.toUserProfile(provider = "email")
            upsertFirestoreProfile(profile, isNew = false)
            profile
        }

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserProfile> =
        runAuthCatching("email") {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            val profile = user.toUserProfile(provider = "email")
            upsertFirestoreProfile(profile, isNew = true)
            profile
        }

    // ── Google ────────────────────────────────────────────────────────────────

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> =
        runAuthCatching("google.com") { provider ->
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            val isNew = result.additionalUserInfo?.isNewUser == true
            val profile = user.toUserProfile(provider = provider)
            upsertFirestoreProfile(profile, isNew = isNew)
            profile
        }

    // ── Password Reset ────────────────────────────────────────────────────────

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        try {
            auth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapFirebaseError(e))
        }

    // ── Token Refresh ─────────────────────────────────────────────────────────

    override suspend fun getFreshIdToken(): String? =
        try {
            auth.currentUser?.getIdToken(/* forceRefresh= */ true)?.await()?.token
        } catch (e: Exception) {
            Log.w(tag, "Token refresh failed: ${e.message}")
            null
        }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    override suspend fun signOut() {
        auth.signOut()
    }

    // ── Firestore Profile Sync ────────────────────────────────────────────────

    private suspend fun upsertFirestoreProfile(profile: UserProfile, isNew: Boolean) {
        val now = com.google.firebase.Timestamp.now()
        val data = buildMap<String, Any?> {
            put("uid", profile.uid)
            put("email", profile.email)
            put("displayName", profile.displayName)
            put("photoUrl", profile.photoUrl)
            put("authProvider", profile.authProvider)
            put("isActive", true)
            put("lastLoginAt", now)
            if (isNew) {
                put("createdAt", now)
                put("updatedAt", now)
            } else {
                put("updatedAt", now)
            }
        }
        try {
            firestore.collection("users")
                .document(profile.uid)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            // Non-fatal: auth succeeded even if Firestore write fails
            Log.w(tag, "Firestore profile upsert failed: ${e.message}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun FirebaseUser.toUserProfile(provider: String) = UserProfile(
        uid = uid,
        email = email,
        phoneNumber = phoneNumber,
        displayName = displayName ?: email?.substringBefore("@") ?: "User",
        photoUrl = photoUrl?.toString(),
        authProvider = provider,
        isAnonymous = false
    )

    private fun detectProvider(user: FirebaseUser?): String =
        user?.providerData?.firstOrNull { it.providerId != "firebase" }?.providerId ?: "email"

    /**
     * Wraps a Firebase auth call: runs [block], emitting Result.Success on success
     * or Result.Error with a friendly mapped message on failure.
     */
    private suspend fun runAuthCatching(
        provider: String,
        block: suspend (provider: String) -> UserProfile
    ): Result<UserProfile> =
        try {
            Result.Success(block(provider))
        } catch (e: Exception) {
            Result.Error(mapFirebaseError(e))
        }

    /**
     * Maps raw Firebase exception messages to user-friendly English strings.
     */
    private fun mapFirebaseError(e: Exception): Exception {
        val msg = e.message ?: ""
        val friendly = when {
            "ERROR_INVALID_EMAIL" in msg || "badly formatted" in msg ->
                "Invalid email address format."
            "ERROR_WRONG_PASSWORD" in msg || "password is invalid" in msg ->
                "Incorrect password. Please try again."
            "ERROR_USER_NOT_FOUND" in msg || "no user record" in msg ->
                "No account found with this email."
            "ERROR_EMAIL_ALREADY_IN_USE" in msg || "already in use" in msg ->
                "An account with this email already exists."
            "ERROR_WEAK_PASSWORD" in msg || "at least 6 characters" in msg ->
                "Password must be at least 6 characters."
            "ERROR_NETWORK_REQUEST_FAILED" in msg || "network error" in msg ->
                "Network error. Please check your connection."
            "ERROR_TOO_MANY_REQUESTS" in msg ->
                "Too many attempts. Please try again later."
            "ERROR_USER_DISABLED" in msg ->
                "This account has been disabled."
            "ERROR_INVALID_CREDENTIAL" in msg || "INVALID_LOGIN_CREDENTIALS" in msg ->
                "Incorrect email or password."
            else -> "Authentication failed. Please try again."
        }
        Log.w("AuthRepository", "Firebase error mapped: $msg -> $friendly")
        return Exception(friendly)
    }
}
