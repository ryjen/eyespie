package com.micrantha.eyespie

import com.micrantha.eyespie.model.ModelAssetState
import com.micrantha.eyespie.model.QueueReason
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelAssetConfirmationPolicyTest {
    @Test
    fun requiresConfirmationForWifiAndPlatformConsentQueues() {
        assertTrue(
            requiresModelAssetConfirmation(
                ModelAssetState.Queued(QueueReason.WaitingForWifi),
            ),
        )
        assertTrue(
            requiresModelAssetConfirmation(
                ModelAssetState.Queued(QueueReason.WaitingForPlatformConfirmation),
            ),
        )
    }

    @Test
    fun doesNotRequireConfirmationForOrdinaryQueueOrOtherStates() {
        assertFalse(requiresModelAssetConfirmation(ModelAssetState.Queued()))
        assertFalse(requiresModelAssetConfirmation(ModelAssetState.NotInstalled))
    }
}
