package com.traffic_guard.ai.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traffic_guard.ai.data.CommunityRepository
import com.traffic_guard.ai.data.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val alerts: List<Incident> = emptyList(),
    val activeFilter: String = "ALL",
    val titleQuery: String = "",
    val dateQuery: String = "",
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

class AlertsFeedViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val PAGE_SIZE = 10

    init {
        loadFeed(refresh = true)
    }

    fun setFilter(filter: String) {
        if (_uiState.value.activeFilter != filter) {
            _uiState.update { it.copy(activeFilter = filter) }
            loadFeed(refresh = true)
        }
    }

    fun setTitleQuery(query: String) {
        _uiState.update { it.copy(titleQuery = query) }
        loadFeed(refresh = true)
    }

    fun setDateQuery(query: String) {
        _uiState.update { it.copy(dateQuery = query) }
        loadFeed(refresh = true)
    }

    fun loadFeed(refresh: Boolean = false) {
        if (refresh) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            currentOffset = 0
        } else {
            if (_uiState.value.isLoadingMore) return
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
        }

        viewModelScope.launch {
            val tQuery = _uiState.value.titleQuery.trim().takeIf { it.isNotEmpty() }
            val dQuery = _uiState.value.dateQuery.trim().takeIf { it.isNotEmpty() }
            
            when (val result = communityRepository.getAlertsFeed(
                limit = PAGE_SIZE,
                offset = currentOffset,
                filter = _uiState.value.activeFilter,
                title = tQuery,
                date = dQuery
            )) {
                is com.traffic_guard.ai.data.CommunityResult.Success -> {
                    val newAlerts = result.data
                    _uiState.update { state ->
                        state.copy(
                             alerts = if (refresh) newAlerts else state.alerts + newAlerts,
                             isRefreshing = false,
                             isLoadingMore = false
                        )
                    }
                    currentOffset += newAlerts.size
                }
                is com.traffic_guard.ai.data.CommunityResult.Error -> {
                    _uiState.update { it.copy(
                        error = result.exception.message ?: "Failed to load feed",
                        isRefreshing = false,
                        isLoadingMore = false
                    ) }
                }
            }
        }
    }
}
