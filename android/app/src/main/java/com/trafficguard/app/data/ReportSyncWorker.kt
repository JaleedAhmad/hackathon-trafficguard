package com.traffic_guard.ai.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val dbHelper = ReportDatabaseHelper(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendingReports = dbHelper.getAllQueuedReports()
        if (pendingReports.isEmpty()) {
            return@withContext Result.success()
        }

        try {
            for (report in pendingReports) {
                // Simulate network uploading task
                kotlinx.coroutines.delay(1000)
                dbHelper.markReportSynced(report.id)
            }
            Result.success()
        } catch (e: Exception) {
            // Failure automatically retries scheduled constraints
            Result.retry()
        }
    }
}
