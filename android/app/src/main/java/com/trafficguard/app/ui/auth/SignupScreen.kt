package com.traffic_guard.ai.ui.auth

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.AuthInputCard
import com.traffic_guard.ai.ui.components.AuthInputType
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.ErrorMessage

@Composable
fun SignupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToOtp: (String, String) -> Unit, // verificationId, phoneNumber
    onNavigateToSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "Sign Up",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Register using email or mobile SMS validation",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.errorMessage != null) {
                ErrorMessage(
                    message = state.errorMessage!!,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            AuthInputCard(
                value = state.emailInput,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = "Email Address",
                inputType = AuthInputType.EMAIL,
                error = state.emailError
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthInputCard(
                value = state.passwordInput,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = "Password",
                inputType = AuthInputType.PASSWORD,
                error = state.passwordError
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuthInputCard(
                value = state.phoneInput,
                onValueChange = { viewModel.onPhoneChanged(it) },
                label = "Phone Number (for SMS Login)",
                inputType = AuthInputType.PHONE,
                error = state.phoneError
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppButton(
                    text = "Sign Up (Email)",
                    onClick = {
                        viewModel.signupWithEmail {
                            onNavigateToSuccess()
                        }
                    },
                    variant = ButtonVariant.SOLID,
                    isLoading = state.isLoading,
                    modifier = Modifier.weight(1f)
                )

                AppButton(
                    text = "Send SMS OTP",
                    onClick = {
                        viewModel.sendPhoneOtp { verificationId ->
                            onNavigateToOtp(verificationId, state.phoneInput)
                        }
                    },
                    variant = ButtonVariant.OUTLINED,
                    isLoading = state.isLoading,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Pehle se account hai? (Sign In)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentBlue
                    )
                }
            }
        }
    }
}
