package com.traffic_guard.ai.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.StatDisplayCard

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isNested: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (!isNested) {
                AppTopBar(
                    title = "Profile",
                    onBackClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).weight(1f)
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isDark) Color.White else Color.Black
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Pic",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = state.username,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            text = "Trusted Contributor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Grid
                Text(
                    text = "Your Impact",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                val stats = state.userStats
                if (stats != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatDisplayCard(
                            title = "Reports",
                            value = "${stats.totalReports}",
                            modifier = Modifier.weight(1f)
                        )
                        StatDisplayCard(
                            title = "Verified",
                            value = "${stats.verifiedReports}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatDisplayCard(
                            title = "People Helped",
                            value = "${stats.peopleHelped}",
                            modifier = Modifier.weight(1f)
                        )
                        StatDisplayCard(
                            title = "Day Streak",
                            value = "${stats.currentStreakDays}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Badges
                Text(
                    text = "Earned Badges",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                state.badgeList.filter { it.isUnlocked }.forEach { badge ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏅", style = MaterialTheme.typography.headlineSmall)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = badge.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
