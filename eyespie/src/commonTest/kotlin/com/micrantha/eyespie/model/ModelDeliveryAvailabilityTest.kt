package com.micrantha.eyespie.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelDeliveryAvailabilityTest {
    @Test
    fun availableStartsNotInstalled() {
        assertEquals(
            ModelAssetState.NotInstalled,
            ModelDeliveryAvailability.Available.initialModelAssetState(),
        )
    }

    @Test
    fun unsupportedOsFailsClosedAsCompatibilityFailure() {
        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Compatibility,
                recoverable = false,
                diagnosticCode = "model_delivery_os_unsupported",
            ),
            ModelDeliveryAvailability.UnsupportedOperatingSystem.initialModelAssetState(),
        )
    }

    @Test
    fun missingConfigurationFailsClosedAsSchedulingFailure() {
        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Scheduling,
                recoverable = false,
                diagnosticCode = "model_delivery_not_configured",
            ),
            ModelDeliveryAvailability.NotConfigured.initialModelAssetState(),
        )
    }
}
