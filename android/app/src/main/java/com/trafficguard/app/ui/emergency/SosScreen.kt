package com.traffic_guard.ai.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.SosCountdownButton

@Composable
fun SosScreen(
    viewModel: SosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEmergencyRouting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.emergencyActive) {
        if (state.emergencyActive) {
            onNavigateToEmergencyRouting()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Force dark mode for SOS
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "EMERGENCY SOS",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            color = AccentRed
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (state.isBroadcasting) "Broadcasting your location to emergency contacts and local authorities..." else "Tap the button below to instantly broadcast your location.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        SosCountdownButton(
            countdownValue = state.countdownValue,
            isBroadcasting = state.isBroadcasting || (state.countdownValue in 1..2),
            onClick = {
                if (state.countdownValue == 3 && !state.isBroadcasting) {
                    viewModel.startSosCountdown()
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        if (state.countdownValue in 1..2 || state.isBroadcasting) {
            AppButton(
                text = "ABORT",
                onClick = { viewModel.abortSos() },
                variant = ButtonVariant.OUTLINED,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AppButton(
                text = "Cancel",
                onClick = onNavigateBack,
                variant = ButtonVariant.TEXT,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
