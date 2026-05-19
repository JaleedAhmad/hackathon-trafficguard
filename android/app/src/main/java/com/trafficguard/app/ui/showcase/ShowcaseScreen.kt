package com.traffic_guard.ai.ui.showcase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.data.ThemeMode
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.AccentGreen
import com.traffic_guard.ai.theme.AccentOrange
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.DarkTextSecondary
import com.traffic_guard.ai.theme.LightBgCard
import com.traffic_guard.ai.theme.LightBorder
import com.traffic_guard.ai.theme.LightTextSecondary
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTextField
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.SkeletonShimmer

@Composable
fun ShowcaseScreen(
    onNavigateToErrorShowcase: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    currentThemeMode: ThemeMode,
    modifier: Modifier = Modifier,
    viewModel: ShowcaseViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTopBar(
            title = "Design System Showcase",
            actions = {
                IconButton(onClick = onNavigateToErrorShowcase) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Test Error/Empty Screens",
                        tint = if (isDark) Color.White else Color.Black
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Section: Theme Configuration Selector
            ShowcaseSection(title = "App Theme Mode", isDark = isDark) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        AppButton(
                            text = mode.name,
                            onClick = { onThemeModeChanged(mode) },
                            variant = if (isSelected) ButtonVariant.SOLID else ButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section: Typography Auditing
            ShowcaseSection(title = "Typography (Outfit Google Font)", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Display Large", style = MaterialTheme.typography.displayLarge)
                    Text("Headline Large", style = MaterialTheme.typography.headlineLarge)
                    Text("Title Large", style = MaterialTheme.typography.titleLarge)
                    Text("Title Medium", style = MaterialTheme.typography.titleMedium)
                    Text("Body Large", style = MaterialTheme.typography.bodyLarge)
                    Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
                    Text("Label Small", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Section: Button Variants
            ShowcaseSection(title = "AppButton Component Library", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = "Solid Primary",
                            onClick = {},
                            variant = ButtonVariant.SOLID,
                            modifier = Modifier.weight(1f)
                        )
                        AppButton(
                            text = "Outlined Action",
                            onClick = {},
                            variant = ButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = "Text Variant",
                            onClick = {},
                            variant = ButtonVariant.TEXT,
                            modifier = Modifier.weight(1f)
                        )
                        AppButton(
                            text = "Danger Red",
                            onClick = {},
                            variant = ButtonVariant.DANGER,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = "Loading Solid",
                            onClick = {},
                            variant = ButtonVariant.SOLID,
                            isLoading = true,
                            modifier = Modifier.weight(1f)
                        )
                        AppButton(
                            text = "With Icon",
                            onClick = {},
                            variant = ButtonVariant.SOLID,
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section: Input TextFields
            ShowcaseSection(title = "AppTextField Components", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onEvent(ShowcaseEvent.InputTextChanged(it)) },
                        label = "Active Input field",
                        placeholder = "Type here...",
                        leadingIcon = Icons.Default.Info,
                        error = state.inputError
                    )
                    
                    AppTextField(
                        value = "Readonly field content",
                        onValueChange = {},
                        label = "Disabled / Read-only field",
                        enabled = false
                    )
                }
            }

            // Section: Loading Shimmer
            ShowcaseSection(title = "SkeletonShimmer Loading States", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.size(50.dp)) {
                            SkeletonShimmer(modifier = Modifier.fillMaxSize())
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SkeletonShimmer(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(16.dp)
                            )
                            SkeletonShimmer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                            )
                        }
                    }
                    
                    SkeletonShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Navigation trigger to test non-ideal states
            AppButton(
                text = "Launch Error & Empty States Screen",
                onClick = onNavigateToErrorShowcase,
                variant = ButtonVariant.SOLID,
                icon = Icons.Default.Build,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ShowcaseSection(
    title: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkBgCard else LightBgCard
        ),
        border = BorderStroke(
            1.dp, 
            if (isDark) DarkBorder else LightBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) DarkBorder else LightBorder,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            content()
        }
    }
}
