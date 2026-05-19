package com.traffic_guard.ai.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ReportRepositoryImpl(private val context: Context) : ReportRepository {

    private val dbHelper = ReportDatabaseHelper(context)
    private val workManager = WorkManager.getInstance(context)

    private val _syncQueueCountFlow = MutableStateFlow(0)
    override val syncQueueCountFlow: StateFlow<Int> = _syncQueueCountFlow

    init {
        updateSyncQueueCount()
    }

    override suspend fun submitReport(report: ReportFormState, forceOffline: Boolean): Result<String> {
        val reportId = UUID.randomUUID().toString()

        // Generate report entity to cache locally
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

        // Mock offline fallback check or direct submission based on forced parameters
        if (forceOffline) {
            dbHelper.insertReport(entity)
            updateSyncQueueCount()
            scheduleSyncWorker()
            return Result.Success(reportId) // Returns successfully with cached status
        }

        // Direct submission simulation (simulates network access)
        return try {
            // Simulated network latency
            kotlinx.coroutines.delay(1500)
            
            // Succeed direct online push
            Result.Success(reportId)
        } catch (e: Exception) {
            // Failure fallback: Cache to local DB
            dbHelper.insertReport(entity)
            updateSyncQueueCount()
            scheduleSyncWorker()
            Result.Success(reportId) // Safe graceful fallback success
        }
    }

    override suspend fun checkNearbyDuplicates(
        lat: Double,
        lng: Double,
        category: String
    ): Result<List<Incident>> {
        // Look up active reports matching targeted categories in close vicinity (Islamabad Sector F-7 demo area)
        val duplicates = mutableListOf<Incident>()
        if (category.equals("FLOOD", ignoreCase = true)) {
            duplicates.add(
                Incident(
                    id = "dup_1",
                    title = "Flash Flood",
                    description = "Confirmed flood water level 2ft deep at Street 4, Sector F-7",
                    location = MapLatLng(lat + 0.001, lng + 0.001),
                    severity = 5,
                    type = IncidentType.FLOOD
                )
            )
        }
        return Result.Success(duplicates)
    }

    override suspend fun uploadPendingReports(): Result<Unit> {
        val pending = dbHelper.getAllQueuedReports()
        for (report in pending) {
            // Simulated upload loop
            kotlinx.coroutines.delay(1000)
            dbHelper.markReportSynced(report.id)
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

    private fun updateSyncQueueCount() {
        _syncQueueCountFlow.value = dbHelper.getAllQueuedReports().size
    }
}
