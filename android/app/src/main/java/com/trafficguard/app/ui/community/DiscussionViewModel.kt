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
    val isLoadingMore: Boolean = false,
    val isLastPage: Boolean = false,
    val error: String? = null
)

class DiscussionViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscussionUiState())
    val uiState: StateFlow<DiscussionUiState> = _uiState.asStateFlow()

    private var hasSubscribedToLive = false

    fun loadComments(incidentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, isLastPage = false, comments = emptyList()) }
            val result = communityRepository.getCommentsPage(incidentId, limit = 100, offset = 0)
            if (result is com.traffic_guard.ai.data.CommunityResult.Success) {
                val list = result.data
                _uiState.update { 
                    it.copy(
                        comments = list,
                        isLoadingMore = false,
                        isLastPage = list.size < 100
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }

        if (!hasSubscribedToLive) {
            hasSubscribedToLive = true
            viewModelScope.launch {
                communityRepository.getCommentsLiveStream(incidentId).collect { newComment ->
                    _uiState.update { state ->
                        val updated = (listOf(newComment) + state.comments)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                        state.copy(comments = updated)
                    }
                }
            }
        }
    }

    fun loadMoreComments(incidentId: String) {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLastPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextOffset = state.comments.size
            val result = communityRepository.getCommentsPage(incidentId, limit = 100, offset = nextOffset)
            if (result is com.traffic_guard.ai.data.CommunityResult.Success) {
                val list = result.data
                if (list.isEmpty()) {
                    _uiState.update { it.copy(isLoadingMore = false, isLastPage = true) }
                } else {
                    _uiState.update { s ->
                        val merged = (s.comments + list)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                        s.copy(
                            comments = merged,
                            isLoadingMore = false,
                            isLastPage = list.size < 100
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoadingMore = false) }
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
