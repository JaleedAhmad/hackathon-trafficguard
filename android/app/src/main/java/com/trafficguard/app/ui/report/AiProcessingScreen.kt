package com.traffic_guard.ai.ui.report

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppTopBar

@Composable
fun AiProcessingScreen(
    onNavigateToSuccess: (Boolean) -> Unit,
    viewModel: AiProcessingViewModel,
    formState: com.traffic_guard.ai.data.ReportFormState,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)

    // Trigger report submission execution on first composition
    LaunchedEffect(Unit) {
        viewModel.submitReport(formState, isNetworkAvailable = true)
    }

    // Direct transition once complete
    LaunchedEffect(state.isSuccess, state.isOfflineSuccess) {
        if (state.isSuccess || state.isOfflineSuccess) {
            onNavigateToSuccess(state.isOfflineSuccess)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "AI Preprocessing",
            onBackClick = {}
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = AccentBlue,
                strokeWidth = 6.dp,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "AI Credibility Verification",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.currentVerificationStep,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Stepper Linear Progress
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = AccentBlue,
                trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(animatedProgress * 100).toInt()}% Analyzed",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = AccentBlue
            )
        }
    }
}
