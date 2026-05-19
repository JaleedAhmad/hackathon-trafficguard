package com.traffic_guard.ai.ui.drivingmode

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceUiState(
    val currentPromptText: String = "Welcome! Starting Navigation mode.",
    val isMuted: Boolean = false,
    val isTtsReady: Boolean = false
)

class VoiceGuidanceViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        // Initialize Android Text To Speech cleanly
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                _uiState.value = _uiState.value.copy(isTtsReady = true)
                speakPrompt("Starting premium driving guidance mode. Drive safely.")
            }
        }
    }

    fun speakPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(currentPromptText = prompt)
        if (!_uiState.value.isMuted && _uiState.value.isTtsReady) {
            tts?.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, "NavPrompt_${System.currentTimeMillis()}")
        }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuted = nextMuted)
        if (nextMuted) {
            tts?.stop()
        } else {
            speakPrompt("Voice prompts unmuted.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
