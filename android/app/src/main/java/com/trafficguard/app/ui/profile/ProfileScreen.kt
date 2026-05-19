package com.traffic_guard.ai.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.StatDisplayCard
import com.traffic_guard.ai.data.NearbyAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogOut: () -> Unit,
    isNested: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF0F172A)
    val context = LocalContext.current

    // Form inputs state
    var name by remember(state.username) { mutableStateOf(state.username) }
    var ageString by remember(state.age) { mutableStateOf(state.age.toString()) }
    var gender by remember(state.gender) { mutableStateOf(state.gender) }
    
    // Store only the local picked Uri for local preview. Do not upload instantly!
    var selectedLocalUri by remember { mutableStateOf<Uri?>(null) }
    
    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    // Decodes the local picked Uri into a Bitmap to show instant preview
    val previewBitmap = remember(selectedLocalUri) {
        selectedLocalUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    android.graphics.BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.w("ProfileScreen", "Failed to decode preview: ${e.message}")
                null
            }
        }
    }

    // Check if the user has actually made any changes relative to initial loaded state
    val hasChanges = name.trim() != state.username.trim() ||
                     (ageString.toIntOrNull() ?: 25) != state.age ||
                     gender != state.gender ||
                     selectedLocalUri != null

    // Photo picker activity launcher - simply updates selectedLocalUri for preview
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLocalUri = uri
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            android.widget.Toast.makeText(context, toastMessage, android.widget.Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (!isNested) {
                AppTopBar(
                    title = "Profile",
                    onBackClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .weight(1f)
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isDark) Color.White else Color.Black
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header with Interactive Avatar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, AccentBlue, CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") }
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Profile Pic Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            NetworkImage(
                                url = state.photoUrl,
                                contentDescription = "Profile Pic",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Dark Overlay with Camera Icon
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit photo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Editable Fields Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Personal Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                focusedLabelColor = AccentBlue,
                                focusedBorderColor = AccentBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Age Field
                        OutlinedTextField(
                            value = ageString,
                            onValueChange = { ageString = it },
                            label = { Text("Age") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                focusedLabelColor = AccentBlue,
                                focusedBorderColor = AccentBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Gender Field
                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Male", "Female").forEach { option ->
                                val isSelected = gender == option
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) AccentBlue else Color.Gray.copy(alpha = 0.15f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) AccentBlue else Color.Gray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { gender = option }
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else (if (isDark) Color.White else Color.Black)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Section (Read-Only)
                Text(
                    text = "Reports Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                val stats = state.userStats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatDisplayCard(
                        title = "Reports Created",
                        value = "${stats?.totalReports ?: 0}",
                        modifier = Modifier.weight(1f)
                    )
                    StatDisplayCard(
                        title = "Verified Signals",
                        value = "${stats?.verifiedReports ?: 0}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reports Created By You Section
                Text(
                    text = "Reports Created By You",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.userReports.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "You haven't reported any hazards yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    state.userReports.forEach { report ->
                        UserReportItem(report = report, isDark = isDark)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                AppButton(
                    text = "Save Profile Changes",
                    onClick = {
                        var fileBytes: ByteArray? = null
                        var mimeType: String? = null
                        
                        // If user picked a local preview, read its bytes first
                        selectedLocalUri?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    fileBytes = inputStream.readBytes()
                                    mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                }
                            } catch (e: Exception) {
                                Log.w("ProfileScreen", "Failed to load selected image bytes: ${e.message}")
                            }
                        }

                        viewModel.saveProfile(
                            name = name,
                            age = ageString.toIntOrNull() ?: 25,
                            gender = gender,
                            photoUrl = state.photoUrl,
                            localBytes = fileBytes,
                            mimeType = mimeType
                        ) { success ->
                            if (success) {
                                selectedLocalUri = null // Clear local preview once fully saved on backend
                                toastMessage = "Profile updated successfully"
                            } else {
                                toastMessage = "Failed to update profile"
                            }
                        }
                    },
                    isLoading = state.isSaving,
                    enabled = hasChanges && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppButton(
                    text = "Log Out",
                    onClick = onLogOut,
                    variant = ButtonVariant.DANGER,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun UserReportItem(
    report: NearbyAlert,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val severityColor = when (report.severity.uppercase()) {
        "CRITICAL", "HIGH" -> Color(0xFFEF4444) // Red
        "MODERATE", "MEDIUM" -> Color(0xFFEAB308) // Yellow
        else -> Color(0xFF3B82F6) // Blue
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = report.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentBlue
                    )
                }

                // Severity Badge
                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = report.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = severityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = report.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Timestamp
                val cleanDate = if (report.timestamp.contains("T")) {
                    report.timestamp.substringBefore("T")
                } else {
                    report.timestamp
                }
                Text(
                    text = cleanDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Confirmations
                Text(
                    text = "👍 ${report.confirmations ?: 0} Confirmations",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentBlue
                )
            }
        }
    }
}

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    connection.inputStream.use { it.readBytes() }
                }
                bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                Log.w("NetworkImage", "Error loading profile pic: ${e.message}")
            }
        }
    }

    val bm = bitmap
    if (bm != null) {
        Image(
            bitmap = bm.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(Color.Gray.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}
