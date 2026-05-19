package com.traffic_guard.ai.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBgDeep
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.LightBgDeep
import com.traffic_guard.ai.theme.LightBorder
import com.traffic_guard.ai.ui.components.AppButton
import com.traffic_guard.ai.ui.components.ButtonVariant
import com.traffic_guard.ai.ui.components.OnboardingSlide
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel()
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        state.slides.size
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateSlideIndex(pagerState.currentPage)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBgDeep else LightBgDeep)
            .padding(24.dp)
    ) {
        // Skip Button in Top Right
        TextButton(
            onClick = {
                viewModel.completeOnboarding {
                    onNavigateToPermissions()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.bodyLarge,
                color = AccentBlue
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val slide = state.slides[page]
                OnboardingSlide(
                    title = slide.title,
                    description = slide.description,
                    illustrationIcon = slide.icon
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slide Page Indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0 until state.slides.size) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isSelected) AccentBlue else (if (isDark) DarkBorder else LightBorder)
                            )
                    )
                }
            }
        }

        // Bottom CTA Buttons
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            if (pagerState.currentPage > 0) {
                AppButton(
                    text = "Back",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    variant = ButtonVariant.OUTLINED,
                    modifier = Modifier.width(100.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(100.dp))
            }

            AppButton(
                text = if (pagerState.currentPage == state.slides.size - 1) "Get Started" else "Next",
                onClick = {
                    if (pagerState.currentPage == state.slides.size - 1) {
                        viewModel.completeOnboarding {
                            onNavigateToPermissions()
                        }
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                variant = ButtonVariant.SOLID,
                modifier = Modifier.width(140.dp)
            )
        }
    }
}
