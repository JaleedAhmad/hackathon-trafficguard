package com.traffic_guard.ai.data

import kotlinx.serialization.Serializable

@Serializable
enum class Severity {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
data class ReportFormState(
    val category: String = "",
    val severity: Severity = Severity.LOW,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val voiceFilePath: String? = null,
    val imageUris: List<String> = emptyList(),
    val isSubmitted: Boolean = false
)

@Serializable
data class ReportQueueEntity(
    val id: String,
    val category: String,
    val severity: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val voiceFilePath: String?,
    val imageUrisJson: String, // Comma or JSON separated string
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
