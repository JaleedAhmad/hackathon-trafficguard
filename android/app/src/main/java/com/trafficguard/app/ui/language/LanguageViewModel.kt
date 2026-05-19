package com.traffic_guard.ai.ui.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class Lang(val code: String, val title: String, val nativeName: String)

data class LanguageUiState(
    val supportedLanguages: List<Lang> = listOf(
        Lang("en", "English", "English"),
        Lang("ur", "Urdu", "اردو"),
        Lang("r-ur", "Roman Urdu", "Roman Urdu")
    ),
    val selectedLanguage: Lang? = null,
    val isSaving: Boolean = false
)

class LanguageViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedLangCode = preferencesRepository.preferredLanguage.first()
            if (savedLangCode != null) {
                val matchingLang = _uiState.value.supportedLanguages.find { it.code == savedLangCode }
                _uiState.value = _uiState.value.copy(selectedLanguage = matchingLang)
            }
        }
    }

    fun selectLanguage(lang: Lang) {
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
    }

    fun saveLanguage(onComplete: () -> Unit) {
        val selected = _uiState.value.selectedLanguage ?: return
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            preferencesRepository.setPreferredLanguage(selected.code)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onComplete()
        }
    }
}
