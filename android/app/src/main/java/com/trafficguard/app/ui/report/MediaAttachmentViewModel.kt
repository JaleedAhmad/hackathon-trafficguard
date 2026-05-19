package com.traffic_guard.ai.ui.report

import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AttachmentUiState(
    val isRecording: Boolean = false,
    val recordDurationSeconds: Int = 0,
    val voiceFilePath: String? = null,
    val isCompressing: Boolean = false,
    val hasPermission: Boolean = false
)

class MediaAttachmentViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AttachmentUiState())
    val uiState: StateFlow<AttachmentUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private var currentRecordFile: File? = null

    fun setPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    @SuppressLint("NewApi")
    fun startVoiceRecording() {
        if (_uiState.value.isRecording) return

        val cacheDir = getApplication<Application>().cacheDir
        currentRecordFile = File(cacheDir, "voice_rep_${System.currentTimeMillis()}.mp4")

        mediaRecorder = MediaRecorder(getApplication<Application>()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)
            setAudioSamplingRate(44100)
            setOutputFile(currentRecordFile!!.absolutePath)
            prepare()
            start()
        }

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordDurationSeconds = 0,
            voiceFilePath = null
        )

        // Launch timer job capping audio notes to 60 seconds
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRecording && _uiState.value.recordDurationSeconds < 60) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    recordDurationSeconds = _uiState.value.recordDurationSeconds + 1
                )
            }
            if (_uiState.value.recordDurationSeconds >= 60) {
                stopVoiceRecording()
            }
        }
    }

    fun stopVoiceRecording() {
        if (!_uiState.value.isRecording) return

        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Safe handling if recording stopped too quickly
        }
        mediaRecorder = null

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            voiceFilePath = currentRecordFile?.absolutePath
        )
    }

    fun deleteVoiceRecording() {
        stopVoiceRecording()
        currentRecordFile?.delete()
        currentRecordFile = null
        _uiState.value = _uiState.value.copy(
            voiceFilePath = null,
            recordDurationSeconds = 0
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
