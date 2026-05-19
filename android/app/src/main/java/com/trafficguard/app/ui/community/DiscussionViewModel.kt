package com.traffic_guard.ai.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.Comment
import com.traffic_guard.ai.data.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscussionUiState(
    val comments: List<Comment> = emptyList(),
    val textInput: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

class DiscussionViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscussionUiState())
    val uiState: StateFlow<DiscussionUiState> = _uiState.asStateFlow()

    fun loadComments(incidentId: String) {
        viewModelScope.launch {
            communityRepository.getCommentsStream(incidentId).collect { comments ->
                _uiState.update { it.copy(comments = comments) }
            }
        }
    }

    fun updateTextInput(text: String) {
        if (text.length <= 200) {
            _uiState.update { it.copy(textInput = text) }
        }
    }

    fun sendComment(incidentId: String) {
        val text = _uiState.value.textInput
        if (text.isBlank() || _uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            val result = communityRepository.addComment(incidentId, text)
            
            if (result is com.traffic_guard.ai.data.CommunityResult.Success) {
                _uiState.update { it.copy(textInput = "", isSending = false) }
            } else {
                _uiState.update { it.copy(isSending = false, error = "Failed to send comment") }
            }
        }
    }
}
