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
import com.traffic_guard.ai.ui.main.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var authRepository: AuthRepository

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                preferencesRepository = PreferencesRepositoryImpl(applicationContext)
                authRepository = AuthRepositoryImpl()
                return MainViewModel(preferencesRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the Places SDK
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(applicationContext, "AIzaSyBGPletDxQfCYv9zilk_HR9ScM96A5cwss")
        }

        // Initialize API Client BASE_URL from AndroidManifest
        com.traffic_guard.ai.data.TrafficGuardApiClient.init(applicationContext)

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
}
