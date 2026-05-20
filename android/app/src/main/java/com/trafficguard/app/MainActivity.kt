package com.traffic_guard.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.data.AuthRepositoryImpl
import com.traffic_guard.ai.data.PreferencesRepository
import com.traffic_guard.ai.data.PreferencesRepositoryImpl
import com.traffic_guard.ai.theme.AndroidTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import android.os.Build
import android.util.Log
import com.traffic_guard.ai.ui.main.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var authRepository: AuthRepository

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(preferencesRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferencesRepository = PreferencesRepositoryImpl(applicationContext)
        authRepository = AuthRepositoryImpl()
        super.onCreate(savedInstanceState)
        
        // Initialize the Places SDK
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(applicationContext, "AIzaSyBGPletDxQfCYv9zilk_HR9ScM96A5cwss")
        }

        // Initialize API Client BASE_URL from AndroidManifest
        com.traffic_guard.ai.data.TrafficGuardApiClient.init(applicationContext)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Observe auth state changes to upload FCM token when logged in
        lifecycleScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    Log.i("MainActivity", "User is logged in (UID: ${user.uid}). Syncing FCM token...")
                    fetchAndUploadFcmToken()
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            
            AndroidTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        preferencesRepository = preferencesRepository,
                        authRepository = authRepository,
                        currentThemeMode = themeMode,
                        onThemeModeChanged = { viewModel.setThemeMode(it) }
                    )
                }
            }
        }
    }

    private fun fetchAndUploadFcmToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    Log.i("MainActivity", "FCM token fetched: $token")
                    
                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val response = com.traffic_guard.ai.data.TrafficGuardApiClient.service.postFcmToken(
                                com.traffic_guard.ai.data.FcmTokenRequest(token)
                            )
                            Log.i("MainActivity", "FCM token upload response: ${response.message}")
                        } catch (e: Exception) {
                            Log.w("MainActivity", "FCM token upload failed: ${e.message}")
                        }
                    }
                } else {
                    Log.w("MainActivity", "Failed to retrieve FCM token: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in fetchAndUploadFcmToken: ${e.message}")
        }
    }
}
