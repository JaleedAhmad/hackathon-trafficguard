package com.traffic_guard.ai

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Splash : NavKey

@Serializable
data object Showcase : NavKey

@Serializable
data object ErrorShowcase : NavKey

@Serializable
data object LanguageSelection : NavKey

@Serializable
data object Onboarding : NavKey

@Serializable
data object Permissions : NavKey

@Serializable
data object Welcome : NavKey

@Serializable
data object Login : NavKey

@Serializable
data object Signup : NavKey

@Serializable
data object ForgotPassword : NavKey

@Serializable
data class OtpVerification(val verificationId: String, val phoneNumber: String) : NavKey

@Serializable
data object AuthSuccess : NavKey

@Serializable
data object Main : NavKey

@Serializable
data object Home : NavKey

@Serializable
data object MapNavigation : NavKey

@Serializable
data object DrivingMode : NavKey

@Serializable
data object ReportWizard : NavKey

@Serializable
data object DuplicateWarning : NavKey

@Serializable
data object AiProcessing : NavKey

@Serializable
data class ReportSuccess(val isOffline: Boolean) : NavKey

@Serializable
data object AlertsFeed : NavKey

@Serializable
data class AlertDetail(val incidentId: String) : NavKey

@Serializable
data class IncidentDiscussion(val incidentId: String) : NavKey

@Serializable
data object Leaderboard : NavKey

@Serializable
data object ContributorProfile : NavKey

@Serializable
data object FalseReportWarning : NavKey

@Serializable
data object SosDashboard : NavKey

@Serializable
data object EmergencyRouting : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data object SettingsHub : NavKey
