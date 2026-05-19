package com.traffic_guard.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    AUTO, ALWAYS_LIGHT, ALWAYS_DARK
}

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val preferredLanguage: Flow<String?>
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setPreferredLanguage(language: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trafficguard_prefs")

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[Keys.THEME_MODE] ?: ThemeMode.AUTO.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.AUTO
        }
    }

    override val preferredLanguage: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.PREFERRED_LANGUAGE]
    }

    override val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setPreferredLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PREFERRED_LANGUAGE] = language
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
}
