package com.traffic_guard.ai.ui.report

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTextField
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.MiniMapCard
import com.traffic_guard.ai.ui.components.VoiceNoteRecorderWidget

@Composable
fun ReportWizardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiProcessing: () -> Unit,
    onNavigateToDuplicateCheck: () -> Unit,
    viewModel: ReportWizardViewModel,
    mediaViewModel: MediaAttachmentViewModel = viewModel(),
    duplicateViewModel: DuplicateCheckViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.formState.collectAsState()
    val mediaState by mediaViewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mediaViewModel.setPermissionGranted(isGranted)
        if (isGranted) {
            mediaViewModel.startVoiceRecording()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required to record voice", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "Report Details",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Category & Severity Header Display
            Text(
                text = "${state.category} - ${state.severity.name} SEVERITY",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Text Description Input
            Text(
                text = "Describe the hazard (Urdu / English)",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = "Hazard Details",
                placeholder = "Srinagar Highway par paani jama hai... (Flood details ahead)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Severity Level Selector Section
            Text(
                text = "Select Severity Level",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                com.traffic_guard.ai.data.Severity.values().forEach { severity ->
                    val isSelected = state.severity == severity
                    AppButton(
                        text = severity.name,
                        onClick = { viewModel.updateSeverity(severity) },
                        variant = if (isSelected) ButtonVariant.SOLID else ButtonVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mini Map Pinned Coordinates selector
            Text(
                text = "Hazard Location Coordinates",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiniMapCard(
                latitude = if (state.latitude == 0.0) 33.6844 else state.latitude,
                longitude = if (state.longitude == 0.0) 73.0479 else state.longitude,
                onLocationChanged = { lat, lng ->
                    viewModel.updateLocation(lat, lng)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Voice Note Recorder Panel
            VoiceNoteRecorderWidget(
                isRecording = mediaState.isRecording,
                recordDurationSeconds = mediaState.recordDurationSeconds,
                voiceFilePath = mediaState.voiceFilePath ?: state.voiceFilePath,
                onStartRecord = {
                    val audioPermission = android.Manifest.permission.RECORD_AUDIO
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        audioPermission
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        mediaViewModel.startVoiceRecording()
                    } else {
                        permissionLauncher.launch(audioPermission)
                    }
                },
                onStopRecord = {
                    mediaViewModel.stopVoiceRecording()
                    mediaViewModel.uiState.value.voiceFilePath?.let { path ->
                        viewModel.updateVoicePath(path)
                    }
                },
                onDeleteRecord = {
                    mediaViewModel.deleteVoiceRecording()
                    viewModel.updateVoicePath(null)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Submission Trigger
            AppButton(
                text = "Verify & Submit",
                onClick = {
                    // Enforce coordinates mock setting if not initialized yet
                    if (state.latitude == 0.0) {
                        viewModel.updateLocation(33.6844, 73.0479)
                    }

                    // Run AI Duplicate Checker
                    duplicateViewModel.checkForDuplicates(
                        lat = if (state.latitude == 0.0) 33.6844 else state.latitude,
                        lng = if (state.longitude == 0.0) 73.0479 else state.longitude,
                        category = state.category,
                        onFinished = { duplicatesFound ->
                            if (duplicatesFound) {
                                onNavigateToDuplicateCheck()
                            } else {
                                onNavigateToAiProcessing()
                            }
                        }
                    )
                },
                variant = ButtonVariant.SOLID,
                enabled = state.category.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
