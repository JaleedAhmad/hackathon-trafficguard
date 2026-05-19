package com.traffic_guard.ai.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ReportRepositoryImpl(private val context: Context) : ReportRepository {

    private val tag = "ReportRepository"
    private val dbHelper = ReportDatabaseHelper(context)
    private val workManager = WorkManager.getInstance(context)
    private val api = TrafficGuardApiClient.service

    private val _syncQueueCountFlow = MutableStateFlow(0)
    override val syncQueueCountFlow: StateFlow<Int> = _syncQueueCountFlow

    init {
        updateSyncQueueCount()
    }

    override suspend fun submitReport(
        report: ReportFormState,
        forceOffline: Boolean
    ): Result<String> {
        val reportId = UUID.randomUUID().toString()

        val entity = ReportQueueEntity(
            id = reportId,
            category = report.category,
            severity = report.severity.name,
            latitude = report.latitude,
            longitude = report.longitude,
            description = report.description,
            voiceFilePath = report.voiceFilePath,
            imageUrisJson = report.imageUris.joinToString(",")
        )

        if (forceOffline) {
            dbHelper.insertReport(entity)
            updateSyncQueueCount()
            scheduleSyncWorker()
            return Result.Success(reportId)
        }

        // Build the RawSignal matching the backend schema exactly
        val isoTimestamp = isoNow()
        val rawSignal = RawSignalRequest(
            signalId = reportId,
            text = buildReportText(report),
            source = "community_report",
            lat = report.latitude,
            lng = report.longitude,
            timestamp = isoTimestamp,
            language = "en"
        )

        return try {
            val response = api.submitReport(rawSignal)
            Log.i(tag, "Report submitted OK: ${response.reportId}, status=${response.status}")
            Result.Success(response.reportId)
        } catch (e: Exception) {
            Log.w(tag, "Submit failed — caching for retry: ${e.message}")
            // Cache locally and schedule background retry
            dbHelper.insertReport(entity)
            updateSyncQueueCount()
            scheduleSyncWorker()
            // Return success so UX is uninterrupted — report will sync later
            Result.Success(reportId)
        }
    }

    override suspend fun checkNearbyDuplicates(
        lat: Double,
        lng: Double,
        category: String
    ): Result<List<Incident>> {
        // Local heuristic — uses backend alerts to cross-check
        return try {
            val response = api.getNearbyAlerts(lat = lat, lng = lng)
            val incidents = response.alerts
                .filter { alert -> matchesCategory(alert.type, category) }
                .map { alert ->
                    Incident(
                        id = alert.alertId,
                        title = alert.type.replace("_", " ").capitalizeWords(),
                        description = alert.message,
                        location = MapLatLng(lat, lng),
                        severity = severityValue(alert.severity),
                        type = mapIncidentType(alert.type)
                    )
                }
            Result.Success(incidents)
        } catch (e: Exception) {
            Log.w(tag, "Nearby duplicate check failed, using empty: ${e.message}")
            Result.Success(emptyList())
        }
    }

    override suspend fun uploadPendingReports(): Result<Unit> {
        val pending = dbHelper.getAllQueuedReports()
        if (pending.isEmpty()) return Result.Success(Unit)

        for (queued in pending) {
            try {
                val rawSignal = RawSignalRequest(
                    signalId = queued.id,
                    text = queued.description,
                    source = "community_report",
                    lat = queued.latitude,
                    lng = queued.longitude,
                    timestamp = isoFromMillis(queued.timestamp)
                )
                api.submitReport(rawSignal)
                dbHelper.markReportSynced(queued.id)
                Log.i(tag, "Synced queued report: ${queued.id}")
            } catch (e: Exception) {
                Log.w(tag, "Failed to sync ${queued.id}: ${e.message}")
                // Continue to next report — don't block the whole queue
            }
        }
        updateSyncQueueCount()
        return Result.Success(Unit)
    }

    override fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<ReportSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateSyncQueueCount() {
        _syncQueueCountFlow.value = dbHelper.getAllQueuedReports().size
    }

    private fun buildReportText(report: ReportFormState): String {
        return buildString {
            append(report.category)
            if (report.description.isNotBlank()) {
                append(": ")
                append(report.description)
            }
            append(" [Severity: ${report.severity.name}]")
        }
    }

    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun isoFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    private fun matchesCategory(type: String, category: String): Boolean {
        val t = type.lowercase()
        val c = category.lowercase()
        return t.contains(c) || c.contains(t) ||
            (c == "flood" && t.contains("flood")) ||
            (c == "accident" && (t.contains("accident") || t.contains("crash"))) ||
            (c == "traffic" && t.contains("traffic"))
    }

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
}
