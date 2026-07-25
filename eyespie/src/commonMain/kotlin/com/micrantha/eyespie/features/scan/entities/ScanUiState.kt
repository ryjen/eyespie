package com.micrantha.eyespie.features.scan.entities

import androidx.compose.runtime.Stable
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization

@Stable
data class ScanUiState(
    val enabled: Boolean,
    val busy: Boolean,
    val cameraAuthorizationLoaded: Boolean,
    val cameraReady: Boolean,
    val cameraAuthorization: CapabilityAuthorization,
    val cameraRequestInFlight: Boolean,
    val cameraAction: CapabilityAction?,
    val cameraActionLabel: String?,
    val cameraStatus: String,
)
