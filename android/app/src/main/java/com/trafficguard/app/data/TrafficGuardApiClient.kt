package com.traffic_guard.ai.data

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════════════════════════
// REQUEST MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * POST /report — matches the backend `RawSignal` Pydantic model exactly.
 */
data class RawSignalRequest(
    @SerializedName("signal_id") val signalId: String,
    @SerializedName("text") val text: String,
    @SerializedName("source") val source: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("language") val language: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// RESPONSE MODELS — match the README JSON schemas exactly
// ═══════════════════════════════════════════════════════════════════════════════

// ── POST /report ─────────────────────────────────────────────────────────────

data class SignalObject(
    @SerializedName("signal_id") val signalId: String,
    @SerializedName("text_normalized") val textNormalized: String,
    @SerializedName("text_original") val textOriginal: String,
    @SerializedName("language_detected") val languageDetected: String,
    @SerializedName("source_type") val sourceType: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("category_hint") val categoryHint: String
)

data class CrisisDetectionResult(
    @SerializedName("crisis_detected") val crisisDetected: Boolean,
    @SerializedName("status") val status: String,
    @SerializedName("crisis_type") val crisisType: String,
    @SerializedName("confidence_score") val confidenceScore: Int,
    @SerializedName("reasoning") val reasoning: String,
    @SerializedName("contradictions") val contradictions: List<String>
)

data class SituationAction(
    @SerializedName("action_id") val actionId: String,
    @SerializedName("priority") val priority: Int,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("estimated_impact") val estimatedImpact: String,
    @SerializedName("requires_api") val requiresApi: Boolean
)

data class SituationPlanResult(
    @SerializedName("severity") val severity: String,
    @SerializedName("affected_radius_km") val affectedRadiusKm: String,
    @SerializedName("eta_impact_minutes") val etaImpactMinutes: Int,
    @SerializedName("escalation_probability") val escalationProbability: String,
    @SerializedName("actions") val actions: List<SituationAction>,
    @SerializedName("ai_reasoning") val aiReasoning: String
)

data class ReportResponse(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("estimated_processing_ms") val estimatedProcessingMs: Long,
    @SerializedName("ingestion_result") val ingestionResult: SignalObject?,
    @SerializedName("trust_result") val trustResult: CrisisDetectionResult?,
    @SerializedName("situation_result") val situationResult: SituationPlanResult?,
    @SerializedName("execution_result") val executionResult: Map<String, Any>?
)

// ── GET /crisis/current ───────────────────────────────────────────────────────

data class SignalSource(
    @SerializedName("source") val source: String,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("cluster_radius_m") val clusterRadiusM: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("confirmed") val confirmed: Boolean? = null,
    @SerializedName("congestion_spike_pct") val congestionSpikePct: Int? = null
)

data class CurrentCrisisResponse(
    @SerializedName("crisis_id") val crisisId: String,
    @SerializedName("location") val location: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("type") val type: String,
    @SerializedName("confidence") val confidence: Int,
    @SerializedName("severity") val severity: String,
    @SerializedName("status") val status: String,
    @SerializedName("affected_radius_km") val affectedRadiusKm: Double,
    @SerializedName("eta_increase_mins") val etaIncreaseMins: Int,
    @SerializedName("signals") val signals: List<SignalSource>,
    @SerializedName("trust_analysis") val trustAnalysis: CrisisDetectionResult?,
    @SerializedName("situation_plan") val situationPlan: SituationPlanResult?
)

// ── GET /alerts/nearby ────────────────────────────────────────────────────────

data class NearbyAlert(
    @SerializedName("alert_id") val alertId: String,
    @SerializedName("type") val type: String,
    @SerializedName("message") val message: String,
    @SerializedName("message_urdu") val messageUrdu: String?,
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("severity") val severity: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("alternate_route") val alternateRoute: String?
)

data class NearbyAlertsResponse(
    @SerializedName("alerts") val alerts: List<NearbyAlert>
)

// ── GET /agents/trace ─────────────────────────────────────────────────────────

data class AgentTraceStep(
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("type") val type: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Map<String, Any>?
)

data class AgentTraceResponse(
    @SerializedName("Agent1") val agent1: List<AgentTraceStep>?,
    @SerializedName("Agent2") val agent2: List<AgentTraceStep>?,
    @SerializedName("Agent3") val agent3: List<AgentTraceStep>?,
    @SerializedName("Agent4") val agent4: List<AgentTraceStep>?,
    // Fields present when no trace yet
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)

// ── GET /baseline/compare ─────────────────────────────────────────────────────

data class BaselineResult(
    @SerializedName("detected") val detected: Boolean,
    @SerializedName("type") val type: String,
    @SerializedName("confidence") val confidence: String
)

data class BaselineCompareResponse(
    @SerializedName("heuristic_result") val heuristicResult: Map<String, Any>?,
    @SerializedName("agentic_result") val agenticResult: BaselineResult?,
    @SerializedName("raw_input_text") val rawInputText: String
)

// ── Auth endpoints (used by AuthRepository) ───────────────────────────────────

data class VerifyTokenResponse(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("message") val message: String
)

data class MeResponse(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("authProvider") val authProvider: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("lastLoginAt") val lastLoginAt: String?
)

data class LogoutResponse(@SerializedName("message") val message: String)

// ═══════════════════════════════════════════════════════════════════════════════
// RETROFIT SERVICE INTERFACE — all 5 backend endpoints + auth
// ═══════════════════════════════════════════════════════════════════════════════

interface TrafficGuardApiService {

    // ── 1. Submit Report — POST /report ───────────────────────────────────────
    @POST("report")
    suspend fun submitReport(
        @Body signal: RawSignalRequest
    ): ReportResponse

    // ── 2. Current Crisis — GET /crisis/current ───────────────────────────────
    @GET("crisis/current")
    suspend fun getCurrentCrisis(): CurrentCrisisResponse

    // ── 3. Nearby Alerts — GET /alerts/nearby ─────────────────────────────────
    @GET("alerts/nearby")
    suspend fun getNearbyAlerts(
        @Query("lat") lat: Double = 24.8607,
        @Query("lng") lng: Double = 67.0011
    ): NearbyAlertsResponse

    // ── 4. Agent Trace — GET /agents/trace ────────────────────────────────────
    @GET("agents/trace")
    suspend fun getAgentTrace(): AgentTraceResponse

    // ── 5. Baseline Compare — GET /baseline/compare ───────────────────────────
    @GET("baseline/compare")
    suspend fun getBaselineComparison(): BaselineCompareResponse

    // ── Auth endpoints ─────────────────────────────────────────────────────────
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

// ═══════════════════════════════════════════════════════════════════════════════
// SINGLETON RETROFIT CLIENT
// ═══════════════════════════════════════════════════════════════════════════════

object TrafficGuardApiClient {

    /**
     * Backend base URL.
     * - Emulator → http://10.0.2.2:8000/
     * - Physical device on same LAN → http://<your_machine_ip>:8000/
     */
    // const val BASE_URL = "http://10.0.2.2:8000/"
    const val BASE_URL = "http://127.0.0.1:8000/"


    private val loggingInterceptor = HttpLoggingInterceptor { msg ->
        Log.d("OkHttp", msg)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val task = user.getIdToken(false)
                val tokenResult = Tasks.await(task, 5, TimeUnit.SECONDS)
                val token = tokenResult.token
                if (!token.isNullOrEmpty()) {
                    builder.header("Authorization", "Bearer $token")
                }
            }
        } catch (e: Exception) {
            Log.w("TrafficGuardApiClient", "Failed to dynamically append ID token: ${e.message}")
        }
        chain.proceed(builder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
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
