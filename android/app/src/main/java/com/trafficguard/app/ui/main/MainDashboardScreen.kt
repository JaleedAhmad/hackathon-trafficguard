package com.traffic_guard.ai.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.community.AlertsFeedScreen
import com.traffic_guard.ai.ui.community.AlertsFeedViewModel
import com.traffic_guard.ai.ui.community.LeaderboardScreen
import com.traffic_guard.ai.ui.community.ReputationViewModel
import com.traffic_guard.ai.ui.home.HomeScreen
import com.traffic_guard.ai.ui.home.HomeViewModel
import com.traffic_guard.ai.ui.profile.ProfileScreen
import com.traffic_guard.ai.ui.profile.ProfileViewModel
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.ButtonVariant
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FEED("Feed", Icons.Default.Warning),
    LEADERBOARD("Rank", Icons.Default.Leaderboard),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun MainDashboardScreen(
    authRepository: AuthRepository,
    homeViewModel: HomeViewModel,
    feedViewModel: AlertsFeedViewModel,
    reputationViewModel: ReputationViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToAlertDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val bgColor = if (isDark) DarkBgDeep else LightBgDeep
    val scope = rememberCoroutineScope()

    val currentUser by authRepository.currentUser.collectAsState(initial = null)
    val isGuest = currentUser?.isAnonymous == true

    var showReportRestrictionDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = bgColor,
                tonalElevation = 8.dp
            ) {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tab == MainTab.FEED) {
                                BadgedBox(badge = { Badge { Text("!") } }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = if (isDark) Color.LightGray else Color.Gray,
                            unselectedTextColor = if (isDark) Color.LightGray else Color.Gray,
                            indicatorColor = AccentBlue.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToMap = onNavigateToMap,
                    onNavigateToReport = {
                        if (isGuest) {
                            showReportRestrictionDialog = true
                        } else {
                            onNavigateToReport()
                        }
                    },
                    onNavigateToSos = onNavigateToSos
                )
                1 -> AlertsFeedScreen(
                    viewModel = feedViewModel,
                    onNavigateBack = { selectedTab = 0 },
                    onNavigateToDetail = onNavigateToAlertDetail,
                    isNested = true
                )
                2 -> LeaderboardScreen(
                    viewModel = reputationViewModel,
                    onNavigateBack = { selectedTab = 0 },
                    isNested = true
                )
                3 -> {
                    if (isGuest) {
                        RestrictedProfileView(
                            isDark = isDark,
                            onSignInClick = {
                                scope.launch {
                                    authRepository.signOut()
                                    onNavigateToAuth()
                                }
                            }
                        )
                    } else {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onNavigateBack = { selectedTab = 0 },
                            onNavigateToSettings = onNavigateToSettings,
                            isNested = true
                        )
                    }
                }
            }
        }
    }

    if (showReportRestrictionDialog) {
        AlertDialog(
            onDismissRequest = { showReportRestrictionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Sign In Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
            },
            text = {
                Text(
                    text = "Reporting incidents, verifying active road hazards, and earning contributor badges requires a verified account. Sign in to help keep your local driving community safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                AppButton(
                    text = "Sign In / Register",
                    onClick = {
                        showReportRestrictionDialog = false
                        scope.launch {
                            authRepository.signOut()
                            onNavigateToAuth()
                        }
                    },
                    variant = ButtonVariant.SOLID
                )
            },
            dismissButton = {
                TextButton(onClick = { showReportRestrictionDialog = false }) {
                    Text(
                        text = "Dismiss",
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }
            },
            containerColor = if (isDark) DarkBgDeep else LightBgDeep,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun RestrictedProfileView(
    isDark: Boolean,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Beautiful vibrant glassmorphic lock card
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentBlue, AccentRed)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Unlock Contributor Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Join our secure AI-driven network to view your driver reputation score, verify dynamic road alerts, and trace the processing agents pipeline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        AppButton(
            text = "Create Account or Sign In",
            onClick = onSignInClick,
            variant = ButtonVariant.SOLID,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
