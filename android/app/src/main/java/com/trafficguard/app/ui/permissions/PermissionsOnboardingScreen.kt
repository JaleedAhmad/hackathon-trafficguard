package com.traffic_guard.ai.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.PermissionItemCard

@Composable
fun PermissionsOnboardingScreen(
    onNavigateToWelcome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = viewModel()
) {
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TrafficGuard AI requires the following permissions to ensure a smooth, secure navigation and warning experience.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PermissionItemCard(
                    title = "Location Services",
                    description = "Enable exact location reporting to route around hazard zones in real time.",
                    icon = Icons.Default.Place,
                    isGranted = state.locationPermissionGranted,
                    onToggle = { viewModel.setLocationPermissionGranted(it) }
                )

                PermissionItemCard(
                    title = "Push Notifications",
                    description = "Receive instant reports and alert triggers when approaching bottleneck routes.",
                    icon = Icons.Default.Notifications,
                    isGranted = state.notificationPermissionGranted,
                    onToggle = { viewModel.setNotificationPermissionGranted(it) }
                )
            }
        }

        AppButton(
            text = "Continue to App",
            onClick = onNavigateToWelcome,
            variant = ButtonVariant.SOLID,
            enabled = state.locationPermissionGranted, // Location is mandatory
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Rationale Dialog
        if (state.showRationaleDialog && state.rationaleMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissRationaleDialog() },
                title = { Text(text = "Location Permission Required") },
                text = { Text(text = state.rationaleMessage!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.setLocationPermissionGranted(true) }) {
                        Text(text = "Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissRationaleDialog() }) {
                        Text(text = "Dismiss")
                    }
                }
            )
        }
    }
}
