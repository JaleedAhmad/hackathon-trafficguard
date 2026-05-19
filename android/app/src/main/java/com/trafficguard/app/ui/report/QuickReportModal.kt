package com.traffic_guard.ai.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.Severity
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.CategorySelectPill
import com.traffic_guard.ai.ui.components.SeveritySelector

data class CategoryItem(
    val category: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReportModal(
    onDismiss: () -> Unit,
    onNavigateToWizard: (String, Severity) -> Unit,
    sheetState: SheetState,
    viewModel: ReportWizardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.formState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    val categories = listOf(
        CategoryItem("FLOOD", Icons.Default.Water),
        CategoryItem("TRAFFIC", Icons.Default.Traffic),
        CategoryItem("ACCIDENT", Icons.Default.CarCrash),
        CategoryItem("WEATHER", Icons.Default.Cloud)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) DarkBgDeep else LightBgDeep,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Quick Hazard Report",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select Category & Severity to alert nearby drivers",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(categories) { item ->
                    CategorySelectPill(
                        title = item.category,
                        icon = item.icon,
                        isSelected = state.category == item.category,
                        onClick = { viewModel.updateCategory(item.category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Hazard Severity Level",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SeveritySelector(
                selectedSeverity = state.severity,
                onSeveritySelected = { viewModel.updateSeverity(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                AppButton(
                    text = "Attach Details",
                    onClick = {
                        if (state.category.isNotEmpty()) {
                            onNavigateToWizard(state.category, state.severity)
                        }
                    },
                    variant = ButtonVariant.OUTLINED,
                    enabled = state.category.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppButton(
                    text = "Quick Submit",
                    onClick = {
                        if (state.category.isNotEmpty()) {
                            // Submits directly with default or center location
                            viewModel.updateLocation(33.6844, 73.0479)
                            onNavigateToWizard(state.category, state.severity)
                        }
                    },
                    variant = ButtonVariant.SOLID,
                    enabled = state.category.isNotEmpty(),
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}
