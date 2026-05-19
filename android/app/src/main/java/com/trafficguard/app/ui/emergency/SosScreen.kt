package com.traffic_guard.ai.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults

@Composable
fun SosScreen(
    viewModel: SosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEmergencyRouting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    
    var customMessage by remember { mutableStateOf("") }
    val wordCount = remember(customMessage) {
        if (customMessage.isBlank()) 0 else customMessage.trim().split("\\s+".toRegex()).size
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.locationNotEnabled) {
        if (state.locationNotEnabled) {
            android.widget.Toast.makeText(
                context,
                "Unable to get location. Please enable location services.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.resetSosState()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.emergencyActive) {
        if (state.emergencyActive) {
            viewModel.resetSosState()
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
            text = if (state.isBroadcasting) "Broadcasting your location and message to local users..." else "Explain your emergency below and tap the button 3 times to send.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.tapsRemaining > 0 && !state.isBroadcasting) {
            Text(
                text = "Explain your emergency (Max 100 words):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = customMessage,
                onValueChange = { text ->
                    val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
                    if (words <= 100) {
                        customMessage = text
                    }
                },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Type here...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedIndicatorColor = AccentRed,
                    unfocusedIndicatorColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$wordCount / 100 words",
                color = if (wordCount >= 90) AccentRed else Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        } else {
            Text(
                text = "Broadcasting Emergency:\n${customMessage.ifBlank { "General SOS Alert" }}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentRed,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        SosCountdownButton(
            tapsRemaining = state.tapsRemaining,
            isBroadcasting = state.isBroadcasting,
            onClick = {
                val msg = customMessage.ifBlank { "General SOS Alert" }
                viewModel.registerSosTap(msg)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        if (state.tapsRemaining < 3 || state.isBroadcasting) {
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

