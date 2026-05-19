package com.traffic_guard.ai.data

import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    val syncQueueCountFlow: Flow<Int>
    suspend fun submitReport(report: ReportFormState, forceOffline: Boolean = false): Result<String>
    suspend fun checkNearbyDuplicates(lat: Double, lng: Double, category: String): Result<List<Incident>>
    suspend fun uploadPendingReports(): Result<Unit>
    fun scheduleSyncWorker()
}
