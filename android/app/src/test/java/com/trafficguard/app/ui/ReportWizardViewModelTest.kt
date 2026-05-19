package com.trafficguard.app.ui

import com.traffic_guard.ai.data.ReportFormState
import com.traffic_guard.ai.data.ReportQueueEntity
import com.traffic_guard.ai.data.Severity
import com.traffic_guard.ai.ui.report.ReportWizardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportWizardViewModelTest {

    private lateinit var viewModel: ReportWizardViewModel

    @Before
    fun setUp() {
        viewModel = ReportWizardViewModel()
    }

    @Test
    fun testFormValidation_initiallyInvalid_validOnlyWhenFieldsSet() {
        // Initially empty form
        assertFalse(viewModel.isFormValid())

        // Add category, still invalid due to coordinates unset
        viewModel.updateCategory("FLOOD")
        assertFalse(viewModel.isFormValid())

        // Add coordinates
        viewModel.updateLocation(33.6844, 73.0479)
        assertTrue(viewModel.isFormValid())
    }

    @Test
    fun testImageAttachmentsLimit_preventsOverridingThreeItems() {
        viewModel.addImageUri("uri_1")
        viewModel.addImageUri("uri_2")
        viewModel.addImageUri("uri_3")
        viewModel.addImageUri("uri_4") // Exceeds limit

        assertEquals(3, viewModel.formState.value.imageUris.size)
        assertTrue(viewModel.formState.value.imageUris.contains("uri_1"))
        assertFalse(viewModel.formState.value.imageUris.contains("uri_4"))
    }

    @Test
    fun testReportQueueEntityRepresentation() {
        val report = ReportQueueEntity(
            id = "test_id_123",
            category = "TRAFFIC",
            severity = "HIGH",
            latitude = 33.7,
            longitude = 73.0,
            description = "Heavy pileup near Srinagar exit",
            voiceFilePath = "/cache/voice.mp4",
            imageUrisJson = "uri1,uri2"
        )

        assertEquals("test_id_123", report.id)
        assertEquals("TRAFFIC", report.category)
        assertEquals("HIGH", report.severity)
        assertEquals(33.7, report.latitude, 0.0001)
        assertEquals("uri1,uri2", report.imageUrisJson)
    }

    @Test
    fun testMockImageCompressionScaleCalculations() {
        val maxDimension = 1080
        val originalWidth = 2000
        val originalHeight = 1000

        // Ratio verification
        val ratio = maxDimension.toFloat() / originalWidth
        val expectedHeight = (originalHeight * ratio).toInt()

        assertEquals(1080, maxDimension)
        assertEquals(540, expectedHeight)
    }
}

