package com.trafficguard.app.ui

import com.traffic_guard.ai.data.LocationRepository
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.data.NavigationRepository
import com.traffic_guard.ai.data.Result
import com.traffic_guard.ai.data.RoutePath
import com.traffic_guard.ai.ui.mapnavigation.NavigationUiState
import com.traffic_guard.ai.ui.mapnavigation.NavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeLocationRepository = object : LocationRepository {
        override val locationFlow: Flow<MapLatLng> = flow {
            emit(MapLatLng(33.6844, 73.0479))
        }
        override val speedFlow: Flow<Float> = flow {
            emit(15.0f)
        }
        override fun startLocationUpdates() {}
        override fun stopLocationUpdates() {}
    }

    private val fakeNavigationRepository = object : NavigationRepository {
        override suspend fun getRoute(start: MapLatLng, end: MapLatLng): Result<RoutePath> {
            val route = RoutePath(
                points = listOf(start, end),
                distanceMeters = 8000,
                durationSeconds = 600,
                isHazardSegment = false,
                summary = "Primary Srinagar Highway Route"
            )
            return Result.Success(route)
        }

        override suspend fun getAlternatives(start: MapLatLng, end: MapLatLng): Result<List<RoutePath>> {
            val detour = RoutePath(
                points = listOf(start, MapLatLng(33.7000, 73.0500), end),
                distanceMeters = 9000,
                durationSeconds = 720,
                isHazardSegment = false,
                summary = "AI Detour Alternative"
            )
            return Result.Success(listOf(detour))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRequestRoutePlan_updatesActiveRouteAndManeuvers() = runTest {
        val viewModel = NavigationViewModel(fakeNavigationRepository, fakeLocationRepository)
        
        // Request route plan
        viewModel.requestRoutePlan(MapLatLng(33.7220, 73.0580))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeRoute)
        assertEquals(NavigationUiState.ACTIVE_ROUTING, state.navState)
        assertEquals("Primary Srinagar Highway Route", state.activeRoute?.summary)
        assertEquals(8000, state.activeRoute?.distanceMeters)
        assertEquals(600, state.activeRoute?.durationSeconds)
        assertNotNull(state.currentGuidance)
    }

    @Test
    fun testSelectAlternateRoute_swapsPrimaryWithDetour() = runTest {
        val viewModel = NavigationViewModel(fakeNavigationRepository, fakeLocationRepository)
        
        // Calculate initial route plans
        viewModel.requestRoutePlan(MapLatLng(33.7220, 73.0580))
        testDispatcher.scheduler.advanceUntilIdle()

        val stateBefore = viewModel.uiState.value
        assertEquals(1, stateBefore.alternateRoutes.size)
        val alternate = stateBefore.alternateRoutes.first()

        // Select alternate route detour
        viewModel.selectAlternateRoute(alternate)
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfter = viewModel.uiState.value
        assertEquals("AI Detour Alternative", stateAfter.activeRoute?.summary)
        assertEquals("Primary Srinagar Highway Route", stateAfter.alternateRoutes.first().summary)
    }

    @Test
    fun testTriggerProximityWarning_transitionsToProximityHazardState() = runTest {
        val viewModel = NavigationViewModel(fakeNavigationRepository, fakeLocationRepository)
        
        // Trigger hazard spotted ahead alert
        viewModel.triggerProximityWarning()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(NavigationUiState.PROXIMITY_HAZARD, state.navState)
    }
}
