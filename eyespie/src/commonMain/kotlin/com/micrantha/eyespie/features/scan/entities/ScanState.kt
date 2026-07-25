package com.micrantha.eyespie.features.scan.entities

import androidx.compose.runtime.Stable
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization

@Stable
data class ScanState(
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val location: Location? = null,
    val cameraAuthorization: CapabilityAuthorization = CapabilityAuthorization.NotRequested,
    val cameraAuthorizationLoaded: Boolean = false,
    val cameraRequestInFlight: Boolean = false,
)
