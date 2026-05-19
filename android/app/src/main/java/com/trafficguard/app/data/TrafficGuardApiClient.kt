package com.traffic_guard.ai.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ─── Retrofit API Service interface ──────────────────────────────────────────

interface TrafficGuardApiService {

    @POST("auth/verify")
    suspend fun verifyToken(
        @Header("Authorization") bearerToken: String
    ): VerifyTokenResponse

    @GET("auth/me")
    suspend fun getMe(
        @Header("Authorization") bearerToken: String
    ): MeResponse

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String
    ): LogoutResponse
}

// ─── Response models ──────────────────────────────────────────────────────────

data class VerifyTokenResponse(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isActive: Boolean,
    val message: String
)

data class MeResponse(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val authProvider: String,
    val createdAt: String?,
    val lastLoginAt: String?
)

data class LogoutResponse(val message: String)

// ─── Singleton factory ────────────────────────────────────────────────────────

object TrafficGuardApiClient {

    /**
     * Set this to your actual FastAPI backend URL.
     * Override via BuildConfig or local.properties → BuildConfig in production.
     */
    const val BASE_URL = "http://10.0.2.2:8000/"   // emulator → localhost

    private val loggingInterceptor = HttpLoggingInterceptor { msg ->
        Log.d("OkHttp", msg)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val service: TrafficGuardApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TrafficGuardApiService::class.java)
    }
}
