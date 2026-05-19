package com.traffic_guard.ai

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.data.PreferencesRepository
import com.traffic_guard.ai.data.ThemeMode
import com.traffic_guard.ai.ui.language.LanguageSelectionScreen
import com.traffic_guard.ai.ui.language.LanguageViewModel
import com.traffic_guard.ai.ui.onboarding.OnboardingScreen
import com.traffic_guard.ai.ui.onboarding.OnboardingViewModel
import com.traffic_guard.ai.ui.permissions.PermissionsOnboardingScreen
import com.traffic_guard.ai.ui.permissions.PermissionsViewModel
import com.traffic_guard.ai.ui.auth.WelcomeScreen
import com.traffic_guard.ai.ui.auth.LoginScreen
import com.traffic_guard.ai.ui.auth.SignupScreen
import com.traffic_guard.ai.ui.auth.ForgotPasswordScreen
import com.traffic_guard.ai.ui.auth.AuthViewModel
import com.traffic_guard.ai.ui.auth.ForgotPasswordViewModel

import com.traffic_guard.ai.ui.auth.AccountSetupSuccessScreen
import com.traffic_guard.ai.ui.main.MainDashboardScreen
import com.traffic_guard.ai.ui.showcase.ErrorShowcaseScreen
import com.traffic_guard.ai.ui.showcase.ShowcaseScreen
import com.traffic_guard.ai.ui.splash.SplashScreen
import com.traffic_guard.ai.ui.splash.SplashViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.traffic_guard.ai.ui.home.HomeScreen
import com.traffic_guard.ai.ui.home.HomeViewModel
import com.traffic_guard.ai.ui.mapnavigation.MapNavigationScreen
import com.traffic_guard.ai.ui.mapnavigation.NavigationViewModel
import com.traffic_guard.ai.ui.mapnavigation.AiRerouteViewModel
import com.traffic_guard.ai.ui.drivingmode.DrivingModeScreen
import com.traffic_guard.ai.ui.drivingmode.VoiceGuidanceViewModel
import com.traffic_guard.ai.data.LocationRepositoryImpl
import com.traffic_guard.ai.data.NavigationRepositoryImpl
import com.traffic_guard.ai.data.ReportRepositoryImpl
import com.traffic_guard.ai.ui.report.ReportWizardViewModel
import com.traffic_guard.ai.ui.report.ReportWizardScreen
import com.traffic_guard.ai.ui.report.MediaAttachmentViewModel
import com.traffic_guard.ai.ui.report.DuplicateCheckViewModel
import com.traffic_guard.ai.ui.report.DuplicateWarningScreen
import com.traffic_guard.ai.ui.report.AiProcessingViewModel
import com.traffic_guard.ai.ui.report.AiProcessingScreen
import com.traffic_guard.ai.ui.report.ReportSuccessScreen
import com.traffic_guard.ai.data.CommunityRepositoryImpl
import com.traffic_guard.ai.ui.community.AlertsFeedViewModel
import com.traffic_guard.ai.ui.community.AlertsFeedScreen
import com.traffic_guard.ai.ui.community.AlertDetailViewModel
import com.traffic_guard.ai.ui.community.AlertDetailScreen
import com.traffic_guard.ai.ui.community.DiscussionViewModel
import com.traffic_guard.ai.ui.community.IncidentDiscussionScreen
import com.traffic_guard.ai.ui.community.ReputationViewModel
import com.traffic_guard.ai.ui.community.LeaderboardScreen
import com.traffic_guard.ai.ui.community.ContributorProfileScreen
import com.traffic_guard.ai.ui.community.FalseReportWarningScreen
import com.traffic_guard.ai.data.EmergencyRepositoryImpl
import com.traffic_guard.ai.ui.emergency.SosViewModel
import com.traffic_guard.ai.ui.emergency.SosScreen
import com.traffic_guard.ai.ui.emergency.EmergencyRoutingViewModel
import com.traffic_guard.ai.ui.emergency.EmergencyRoutingScreen
import com.traffic_guard.ai.data.ProfileRepositoryImpl
import com.traffic_guard.ai.ui.profile.ProfileViewModel
import com.traffic_guard.ai.ui.profile.ProfileScreen
import com.traffic_guard.ai.ui.profile.SettingsViewModel
import com.traffic_guard.ai.ui.profile.SettingsHubScreen

@Composable
fun MainNavigation(
    preferencesRepository: PreferencesRepository,
    authRepository: AuthRepository,
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit
) {
    val backStack = rememberNavBackStack(Splash)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            
            entry<Splash> {
                val splashViewModel: SplashViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SplashViewModel(preferencesRepository) as T
                        }
                    }
                )
                SplashScreen(
                    onNavigateToLanguageSelection = {
                        backStack.removeLastOrNull()
                        backStack.add(LanguageSelection)
                    },
                    onNavigateToOnboarding = {
                        backStack.removeLastOrNull()
                        backStack.add(Onboarding)
                    },
                    onNavigateToWelcome = {
                        backStack.removeLastOrNull()
                        backStack.add(Welcome)
                    },
                    viewModel = splashViewModel
                )
            }

            entry<LanguageSelection> {
                val langViewModel: LanguageViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LanguageViewModel(preferencesRepository) as T
                        }
                    }
                )
                LanguageSelectionScreen(
                    onNavigateToOnboarding = {
                        backStack.removeLastOrNull()
                        backStack.add(Onboarding)
                    },
                    viewModel = langViewModel
                )
            }

            entry<Onboarding> {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return OnboardingViewModel(preferencesRepository) as T
                        }
                    }
                )
                OnboardingScreen(
                    onNavigateToPermissions = {
                        backStack.removeLastOrNull()
                        backStack.add(Permissions)
                    },
                    viewModel = onboardingViewModel
                )
            }

            entry<Permissions> {
                val permissionsViewModel: PermissionsViewModel = viewModel()
                PermissionsOnboardingScreen(
                    onNavigateToWelcome = {
                        backStack.removeLastOrNull()
                        backStack.add(Welcome)
                    },
                    viewModel = permissionsViewModel
                )
            }

            entry<Welcome> {
                val authViewModel: AuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AuthViewModel(authRepository) as T
                        }
                    }
                )
                WelcomeScreen(
                    onNavigateToLogin = { backStack.add(Login) },
                    onNavigateToSignup = { backStack.add(Signup) },
                    onNavigateToMain = {
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    },
                    viewModel = authViewModel
                )
            }

            entry<Login> {
                val authViewModel: AuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AuthViewModel(authRepository) as T
                        }
                    }
                )
                LoginScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToSignup = {
                        backStack.removeLastOrNull()
                        backStack.add(Signup)
                    },
                    onNavigateToForgotPassword = { backStack.add(ForgotPassword) },
                    onNavigateToSuccess = {
                        backStack.removeLastOrNull()
                        backStack.add(AuthSuccess)
                    },
                    viewModel = authViewModel
                )
            }

            entry<Signup> {
                val authViewModel: AuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AuthViewModel(authRepository) as T
                        }
                    }
                )
                SignupScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToLogin = {
                        backStack.removeLastOrNull()
                        backStack.add(Login)
                    },
                    onNavigateToSuccess = {
                        backStack.removeLastOrNull()
                        backStack.add(AuthSuccess)
                    },
                    viewModel = authViewModel
                )
            }

            entry<ForgotPassword> {
                val forgotViewModel: ForgotPasswordViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ForgotPasswordViewModel(authRepository) as T
                        }
                    }
                )
                ForgotPasswordScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    viewModel = forgotViewModel
                )
            }



            entry<AuthSuccess> {
                AccountSetupSuccessScreen(
                    onNavigateToMain = {
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    }
                )
            }

            entry<Showcase> {
                ShowcaseScreen(
                    onNavigateToErrorShowcase = {
                        backStack.add(ErrorShowcase)
                    },
                    onThemeModeChanged = onThemeModeChanged,
                    currentThemeMode = currentThemeMode
                )
            }

            entry<ErrorShowcase> {
                ErrorShowcaseScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<Main> {
                val context = LocalContext.current
                val homeViewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val locationRepo = LocationRepositoryImpl(context.applicationContext)
                            return HomeViewModel(locationRepo) as T
                        }
                    }
                )
                val feedVM: AlertsFeedViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return AlertsFeedViewModel(repo) as T
                        }
                    }
                )
                val reputationVM: ReputationViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return ReputationViewModel(repo) as T
                        }
                    }
                )
                val profileVM: ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = ProfileRepositoryImpl()
                            return ProfileViewModel(repo) as T
                        }
                    }
                )
                
                com.traffic_guard.ai.ui.main.MainDashboardScreen(
                    authRepository = authRepository,
                    homeViewModel = homeViewModel,
                    feedViewModel = feedVM,
                    reputationViewModel = reputationVM,
                    profileViewModel = profileVM,
                    onNavigateToMap = { backStack.add(MapNavigation) },
                    onNavigateToReport = { backStack.add(ReportWizard) },
                    onNavigateToAlertDetail = { incidentId -> backStack.add(AlertDetail(incidentId)) },
                    onNavigateToSettings = { backStack.add(SettingsHub) },
                    onNavigateToSos = { backStack.add(SosDashboard) },
                    onNavigateToAuth = {
                        backStack.removeLastOrNull()
                        backStack.add(Welcome)
                    }
                )
            }

            entry<Home> {
                val context = LocalContext.current
                val homeViewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val locationRepo = LocationRepositoryImpl(context.applicationContext)
                            return HomeViewModel(locationRepo) as T
                        }
                    }
                )
                HomeScreen(
                    onNavigateToMap = { backStack.add(MapNavigation) },
                    onNavigateToReport = { backStack.add(ReportWizard) },
                    onNavigateToSos = { backStack.add(SosDashboard) },
                    viewModel = homeViewModel
                )
            }

            entry<MapNavigation> {
                val context = LocalContext.current
                val navigationViewModel: NavigationViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val locationRepo = LocationRepositoryImpl(context.applicationContext)
                            val navRepo = NavigationRepositoryImpl()
                            return NavigationViewModel(navRepo, locationRepo) as T
                        }
                    }
                )
                MapNavigationScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToDrivingMode = { backStack.add(DrivingMode) },
                    viewModel = navigationViewModel
                )
            }

            entry<DrivingMode> {
                val context = LocalContext.current
                val voiceViewModel: VoiceGuidanceViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return VoiceGuidanceViewModel(context.applicationContext as android.app.Application) as T
                        }
                    }
                )
                DrivingModeScreen(
                    onExitDriving = { backStack.removeLastOrNull() },
                    viewModel = voiceViewModel
                )
            }

            entry<ReportWizard> {
                val context = LocalContext.current
                val wizardVM: ReportWizardViewModel = viewModel()
                val mediaVM: MediaAttachmentViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MediaAttachmentViewModel(context.applicationContext as android.app.Application) as T
                        }
                    }
                )
                val dupVM: DuplicateCheckViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val reportRepo = ReportRepositoryImpl(context.applicationContext)
                            return DuplicateCheckViewModel(reportRepo) as T
                        }
                    }
                )
                ReportWizardScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToAiProcessing = { backStack.add(AiProcessing) },
                    onNavigateToDuplicateCheck = { backStack.add(DuplicateWarning) },
                    viewModel = wizardVM,
                    mediaViewModel = mediaVM,
                    duplicateViewModel = dupVM
                )
            }

            entry<DuplicateWarning> {
                val context = LocalContext.current
                val dupVM: DuplicateCheckViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val reportRepo = ReportRepositoryImpl(context.applicationContext)
                            return DuplicateCheckViewModel(reportRepo) as T
                        }
                    }
                )
                DuplicateWarningScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToAiProcessing = { backStack.add(AiProcessing) },
                    onNavigateToSuccess = { backStack.add(ReportSuccess(isOffline = false)) },
                    viewModel = dupVM
                )
            }

            entry<AiProcessing> {
                val context = LocalContext.current
                val processingVM: AiProcessingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val reportRepo = ReportRepositoryImpl(context.applicationContext)
                            return AiProcessingViewModel(reportRepo) as T
                        }
                    }
                )
                val wizardVM: ReportWizardViewModel = viewModel()
                AiProcessingScreen(
                    onNavigateToSuccess = { isOffline -> backStack.add(ReportSuccess(isOffline = isOffline)) },
                    viewModel = processingVM,
                    formState = wizardVM.formState.value
                )
            }

            entry<ReportSuccess> { key ->
                ReportSuccessScreen(
                    isOffline = key.isOffline,
                    onNavigateHome = {
                        backStack.removeLastOrNull() // Return clean to parent dashboard
                    }
                )
            }

            entry<AlertsFeed> {
                val feedVM: AlertsFeedViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return AlertsFeedViewModel(repo) as T
                        }
                    }
                )
                AlertsFeedScreen(
                    viewModel = feedVM,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToDetail = { incidentId -> backStack.add(AlertDetail(incidentId)) }
                )
            }

            entry<AlertDetail> { key ->
                val detailVM: AlertDetailViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return AlertDetailViewModel(repo) as T
                        }
                    }
                )
                AlertDetailScreen(
                    incidentId = key.incidentId,
                    viewModel = detailVM,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToDiscussion = { incidentId -> backStack.add(IncidentDiscussion(incidentId)) }
                )
            }

            entry<IncidentDiscussion> { key ->
                val discussionVM: DiscussionViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return DiscussionViewModel(repo) as T
                        }
                    }
                )
                IncidentDiscussionScreen(
                    incidentId = key.incidentId,
                    viewModel = discussionVM,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<Leaderboard> {
                val reputationVM: ReputationViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return ReputationViewModel(repo) as T
                        }
                    }
                )
                LeaderboardScreen(
                    viewModel = reputationVM,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<ContributorProfile> {
                val reputationVM: ReputationViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = CommunityRepositoryImpl()
                            return ReputationViewModel(repo) as T
                        }
                    }
                )
                ContributorProfileScreen(
                    viewModel = reputationVM,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<FalseReportWarning> {
                FalseReportWarningScreen(
                    onAcknowledge = { backStack.removeLastOrNull() }
                )
            }

            entry<SosDashboard> {
                val sosVM: SosViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = EmergencyRepositoryImpl()
                            return SosViewModel(repo) as T
                        }
                    }
                )
                SosScreen(
                    viewModel = sosVM,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToEmergencyRouting = { backStack.add(EmergencyRouting) }
                )
            }

            entry<EmergencyRouting> {
                val routingVM: EmergencyRoutingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = EmergencyRepositoryImpl()
                            return EmergencyRoutingViewModel(repo) as T
                        }
                    }
                )
                EmergencyRoutingScreen(
                    viewModel = routingVM,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<Profile> {
                val profileVM: ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = ProfileRepositoryImpl()
                            return ProfileViewModel(repo) as T
                        }
                    }
                )
                ProfileScreen(
                    viewModel = profileVM,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToSettings = { backStack.add(SettingsHub) },
                    onLogOut = {
                        coroutineScope.launch {
                            authRepository.signOut()
                            backStack.clear()
                            backStack.add(Welcome)
                        }
                    }
                )
            }

            entry<SettingsHub> {
                val settingsVM: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = ProfileRepositoryImpl()
                            return SettingsViewModel(repo) as T
                        }
                    }
                )
                SettingsHubScreen(
                    viewModel = settingsVM,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
        }


    )
}
