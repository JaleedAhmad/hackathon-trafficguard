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

            // Section: Live Active Crisis from API
            ShowcaseSection(title = "GET /crisis/current — Live Active Crisis", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppButton(
                            text = "Fetch Live Crisis",
                            onClick = { viewModel.onEvent(ShowcaseEvent.LoadCurrentCrisis) },
                            variant = ButtonVariant.SOLID,
                            isLoading = state.isLoadingCrisis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    state.crisisError?.let { err ->
                        Text("Error: $err", color = AccentRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    state.currentCrisis?.let { crisis ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) DarkBgCard.copy(alpha = 0.5f) else LightBgCard.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Crisis ID: ${crisis.crisisId}", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                                Text("Type: ${crisis.type}", color = AccentOrange, fontWeight = FontWeight.SemiBold)
                                Text("Location: ${crisis.location}", color = if (isDark) Color.LightGray else Color.DarkGray)
                                Text("Severity: ${crisis.severity} (Radius: ${crisis.affectedRadiusKm} km)", color = AccentRed)
                                Text("Confidence Score: ${crisis.confidence}%", color = AccentGreen, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("AI Plan Reasoning: ${crisis.situationPlan?.aiReasoning ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.White.copy(0.7f) else Color.Black.copy(0.7f))
                            }
                        }
                    } ?: run {
                        if (!state.isLoadingCrisis) {
                            Text("No live crisis loaded. Click above to fetch.", style = MaterialTheme.typography.bodyMedium, color = if (isDark) DarkTextSecondary else LightTextSecondary)
                        }
                    }
                }
            }

            // Section: Live Heuristic vs AI Baseline Comparison
            ShowcaseSection(title = "GET /baseline/compare — Heuristic vs AI", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = "Fetch Baseline Comparison",
                        onClick = { viewModel.onEvent(ShowcaseEvent.LoadBaselineComparison) },
                        variant = ButtonVariant.OUTLINED,
                        isLoading = state.isLoadingBaseline,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    state.baselineError?.let { err ->
                        Text("Error: $err", color = AccentRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    state.baselineComparison?.let { comp ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Raw Input text: \"${comp.rawInputText}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = if (isDark) Color.LightGray else Color.DarkGray)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x203B82F6) else Color(0x103B82F6))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Simple Heuristics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AccentBlue)
                                        Text("Crisis Detected: ${comp.heuristicResult?.get("crisis_detected") ?: "false"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Confidence: ${comp.heuristicResult?.get("confidence_score") ?: "0"}%", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x2010B981) else Color(0x1010B981))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("AI Agent Pipeline", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AccentGreen)
                                        Text("Crisis Detected: ${comp.agenticResult?.detected ?: "false"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Type: ${comp.agenticResult?.type ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Confidence: ${comp.agenticResult?.confidence ?: "0%"}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    } ?: run {
                        if (!state.isLoadingBaseline) {
                            Text("No comparison data loaded.", style = MaterialTheme.typography.bodyMedium, color = if (isDark) DarkTextSecondary else LightTextSecondary)
                        }
                    }
                }
            }

            // Section: Live Multi-Agent Pipeline Trace Logger
            ShowcaseSection(title = "GET /agents/trace — Agent Trace Logs", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = "Fetch Multi-Agent Trace logs",
                        onClick = { viewModel.onEvent(ShowcaseEvent.LoadAgentTrace) },
                        variant = ButtonVariant.SOLID,
                        isLoading = state.isLoadingTrace,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    state.traceError?.let { err ->
                        Text("Error: $err", color = AccentRed, style = MaterialTheme.typography.bodyMedium)
                    }

                    state.agentTrace?.let { trace ->
                        listOf(
                            Triple("Ingestion Agent (1)", 1, AccentBlue),
                            Triple("Trust Detection Agent (2)", 2, AccentOrange),
                            Triple("Situation Planning Agent (3)", 3, AccentRed),
                            Triple("Execution Agent (4)", 4, AccentGreen)
                        ).forEach { (agentName, id, color) ->
                            val steps = viewModel.agentSteps(id)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) DarkBgCard.copy(alpha = 0.5f) else LightBgCard.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(agentName, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
                                    if (steps.isEmpty()) {
                                        Text("No trace steps available for this agent.", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.Gray else Color.LightGray)
                                    } else {
                                        steps.forEach { step ->
                                            Text("• [${step.type}] ${step.message}", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.LightGray else Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    } ?: run {
                        if (!state.isLoadingTrace) {
                            Text("No trace logs loaded. Traces log dynamically during POST /report flows.", style = MaterialTheme.typography.bodyMedium, color = if (isDark) DarkTextSecondary else LightTextSecondary)
                        }
                    }
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
