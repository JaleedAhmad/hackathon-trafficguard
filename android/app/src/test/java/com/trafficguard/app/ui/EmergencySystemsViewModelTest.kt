package com.trafficguard.app.ui

import com.traffic_guard.ai.data.Badge
import com.traffic_guard.ai.data.ContributorStats
import com.traffic_guard.ai.data.EmergencyCenter
import com.traffic_guard.ai.data.MapLatLng
import com.traffic_guard.ai.data.SettingsState
import com.traffic_guard.ai.data.ThemeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencySystemsViewModelTest {

    @Test
    fun testSettingsState_dataSaverToggle() {
        val defaultState = SettingsState()
        assertFalse(defaultState.dataSaverEnabled)
        
        val newState = defaultState.copy(dataSaverEnabled = true)
        assertTrue(newState.dataSaverEnabled)
    }

    @Test
    fun testSettingsState_themeConfiguration() {
        val state = SettingsState(theme = ThemeType.DARK)
        assertEquals(ThemeType.DARK, state.theme)
    }

    @Test
    fun testEmergencyCenter_distanceMapping() {
        val center = EmergencyCenter(
            id = "c1",
            name = "Test Hospital",
            type = "HOSPITAL",
            distanceMeters = 500
        )
        assertEquals("HOSPITAL", center.type)
        assertTrue(center.distanceMeters < 1000)
    }

    @Test
    fun testContributorStats_accumulation() {
        val stats = ContributorStats(
            userId = "test1",
            totalReports = 10,
            verifiedReports = 8
        )
        
        assertTrue(stats.verifiedReports <= stats.totalReports)
    }

    @Test
    fun testBadge_unlockStatus() {
        val unlockedBadge = Badge(isUnlocked = true)
        val lockedBadge = Badge(isUnlocked = false)
        
        assertTrue(unlockedBadge.isUnlocked)
        assertFalse(lockedBadge.isUnlocked)
    }
}
