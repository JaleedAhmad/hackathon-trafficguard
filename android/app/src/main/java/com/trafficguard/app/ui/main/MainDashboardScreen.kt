package com.traffic_guard.ai.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.community.AlertsFeedScreen
import com.traffic_guard.ai.ui.community.AlertsFeedViewModel
import com.traffic_guard.ai.ui.community.ContributorProfileScreen
import com.traffic_guard.ai.ui.community.LeaderboardScreen
import com.traffic_guard.ai.ui.community.ReputationViewModel
import com.traffic_guard.ai.ui.home.HomeScreen
import com.traffic_guard.ai.ui.home.HomeViewModel
import com.traffic_guard.ai.ui.profile.ProfileScreen
import com.traffic_guard.ai.ui.profile.ProfileViewModel

enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FEED("Feed", Icons.Default.Warning),
    LEADERBOARD("Rank", Icons.Default.Leaderboard),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun MainDashboardScreen(
    homeViewModel: HomeViewModel,
    feedViewModel: AlertsFeedViewModel,
    reputationViewModel: ReputationViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToAlertDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val bgColor = if (isDark) DarkBgDeep else LightBgDeep

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
                    onNavigateToReport = onNavigateToReport,
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
                3 -> ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { selectedTab = 0 },
                    onNavigateToSettings = onNavigateToSettings,
                    isNested = true
                )
            }
        }
    }
}
