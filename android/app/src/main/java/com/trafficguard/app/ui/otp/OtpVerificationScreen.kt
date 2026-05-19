package com.traffic_guard.ai.ui.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.ErrorMessage
import com.traffic_guard.ai.ui.components.OtpInputBlock

@Composable
fun OtpVerificationScreen(
    verificationId: String,
    phoneNumber: String,
    onNavigateBack: () -> Unit,
    onNavigateToSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OtpViewModel = viewModel()
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(verificationId) {
        viewModel.setVerificationId(verificationId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "OTP Verification",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Enter Verification Code",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hum ne aap ke mobile number $phoneNumber par verification code bheja hai.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (state.errorMessage != null) {
                ErrorMessage(
                    message = state.errorMessage!!,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Segmented digits otp block
            OtpInputBlock(
                code = state.codeBuffer,
                onCodeChanged = { viewModel.onCodeChanged(it) },
                isError = state.errorMessage != null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Countdown Timer Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.countdownTimerSeconds > 0) {
                        "Resend code in ${state.countdownTimerSeconds}s"
                    } else {
                        "Didn't receive code?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )

                if (state.countdownTimerSeconds == 0) {
                    TextButton(onClick = { viewModel.resendOtp(phoneNumber) }) {
                        Text(
                            text = "Resend OTP",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            AppButton(
                text = "Verify Code",
                onClick = {
                    viewModel.verifyOtp {
                        onNavigateToSuccess()
                    }
                },
                variant = ButtonVariant.SOLID,
                isLoading = state.isVerifying,
                enabled = state.codeBuffer.length == 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
