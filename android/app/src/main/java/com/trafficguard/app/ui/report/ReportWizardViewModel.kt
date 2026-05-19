package com.traffic_guard.ai.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.LocationRepository
import com.traffic_guard.ai.data.ReportFormState
import com.traffic_guard.ai.data.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportWizardViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(ReportFormState())
    val formState: StateFlow<ReportFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.startLocationUpdates()
            locationRepository.locationFlow.collectLatest { latLng ->
                if (_formState.value.latitude == 0.0 && _formState.value.longitude == 0.0) {
                    _formState.value = _formState.value.copy(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude
                    )
                }
            }
        }
    }

    fun updateCategory(category: String) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updateSeverity(severity: Severity) {
        _formState.value = _formState.value.copy(severity = severity)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description)
    }

    fun updateLocation(lat: Double, lng: Double) {
        _formState.value = _formState.value.copy(latitude = lat, longitude = lng)
    }

    fun updateVoicePath(path: String?) {
        _formState.value = _formState.value.copy(voiceFilePath = path)
    }

    fun addImageUri(uri: String) {
        val current = _formState.value.imageUris.toMutableList()
        if (current.size < 3) {
            current.add(uri)
            _formState.value = _formState.value.copy(imageUris = current)
        }
    }

    fun removeImageUri(uri: String) {
        val current = _formState.value.imageUris.toMutableList()
        current.remove(uri)
        _formState.value = _formState.value.copy(imageUris = current)
    }

    fun isFormValid(): Boolean {
        val state = _formState.value
        return state.category.isNotEmpty() && state.latitude != 0.0 && state.longitude != 0.0
    }

    fun clearForm() {
        _formState.value = ReportFormState()
    }
}
