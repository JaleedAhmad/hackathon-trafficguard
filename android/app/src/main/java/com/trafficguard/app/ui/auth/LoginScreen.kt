package com.traffic_guard.ai.ui.auth

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
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
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
            title = "Sign In",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to access secure navigation reports",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Error Message Display
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

            Spacer(modifier = Modifier.height(16.dp))

            AuthInputCard(
                value = state.passwordInput,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = "Password",
                inputType = AuthInputType.PASSWORD,
                error = state.passwordError
            )

            // Forgot Password Text Button aligned to Right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppButton(
                text = "Sign In",
                onClick = {
                    viewModel.loginWithEmail {
                        onNavigateToSuccess()
                    }
                },
                variant = ButtonVariant.SOLID,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Navigation to Signup Screen
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onNavigateToSignup) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentBlue
                    )
                }
            }
        }
    }
}
