package com.traffic_guard.ai.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.SettingsToggleRow

@Composable
fun SettingsHubScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "Settings",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentBlue,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SettingsToggleRow(
                title = "Data Saver Mode",
                subtitle = "Restrict map downloads over mobile data.",
                isChecked = state.dataSaverEnabled,
                onCheckedChange = { viewModel.toggleDataSaver(it) }
            )

            HorizontalDivider(color = if (isDark) Color.DarkGray else Color.LightGray)

            SettingsToggleRow(
                title = "Push Notifications",
                subtitle = "Receive alerts for nearby hazards.",
                isChecked = state.alertsEnabled,
                onCheckedChange = { viewModel.toggleAlerts(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentBlue,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Add other accessibility settings later (e.g. font slider)
            Text(
                text = "Font scale and custom themes can be adjusted here.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

val AccentBlue = Color(0xFF3B82F6)
