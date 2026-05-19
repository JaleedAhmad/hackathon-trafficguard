package com.traffic_guard.ai.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.ui.components.AppTopBar
import com.traffic_guard.ai.ui.components.IncidentAlertFeedCard

@Composable
fun AlertsFeedScreen(
    viewModel: AlertsFeedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    isNested: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    val listState = rememberLazyListState()

    // Pagination trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadFeed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
    ) {
        AppTopBar(
            title = "Community Alerts Feed",
            onBackClick = onNavigateBack
        )

        // Filters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val filters = listOf("ALL", "FLOOD", "ACCIDENT", "TRAFFIC")
            items(filters) { filter ->
                val isSelected = state.activeFilter == filter
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            color = if (isSelected) AccentBlue else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.setFilter(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color.DarkGray)
                    )
                }
            }
        }

        if (state.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(state.alerts) { incident ->
                    IncidentAlertFeedCard(
                        incident = incident,
                        onClick = { onNavigateToDetail(incident.id) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentBlue)
                        }
                    }
                }

                if (state.alerts.isEmpty()) {
                    item {
                        Text(
                            text = "No alerts found.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
