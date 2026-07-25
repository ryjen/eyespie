package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanState
import com.micrantha.eyespie.features.scan.entities.ScanUiState

class ScanCaptureStateMapper : StateMapper<ScanState, ScanUiState> {

    override fun map(state: ScanState): ScanUiState {
        val (action, actionLabel) = state.cameraAction()
        return ScanUiState(
            enabled = state.enabled && !state.cameraRequestInFlight,
            busy = state.busy,
            cameraAuthorizationLoaded = state.cameraAuthorizationLoaded,
            cameraReady = state.cameraAuthorizationLoaded &&
                state.cameraAuthorization.isCameraReady(),
            cameraAuthorization = state.cameraAuthorization,
            cameraRequestInFlight = state.cameraRequestInFlight,
            cameraAction = action,
            cameraActionLabel = actionLabel,
            cameraStatus = state.cameraAuthorization.status(),
        )
    }

    private fun ScanState.cameraAction(): Pair<CapabilityAction?, String?> {
        if (!cameraAuthorizationLoaded || cameraRequestInFlight) return null to null
        return when (cameraAuthorization) {
            CapabilityAuthorization.NotRequested -> CapabilityAction.Request to "Allow camera"
            CapabilityAuthorization.Denied -> CapabilityAction.Request to "Try again"
            CapabilityAuthorization.SettingsRequired -> CapabilityAction.OpenSettings to "Open settings"
            else -> null to null
        }
    }

    private fun CapabilityAuthorization.isCameraReady(): Boolean =
        this == CapabilityAuthorization.Granted || this == CapabilityAuthorization.NotRequired

    private fun CapabilityAuthorization.status(): String = when (this) {
        CapabilityAuthorization.Unsupported -> "Camera scanning is not supported on this device."
        CapabilityAuthorization.NotRequired -> "Camera access is available without a permission request."
        CapabilityAuthorization.NotRequested -> "Camera access has not been requested."
        CapabilityAuthorization.Granted -> "Camera access is enabled."
        CapabilityAuthorization.Denied -> "Camera access was denied."
        CapabilityAuthorization.Restricted -> "Camera access is restricted on this device."
        CapabilityAuthorization.SettingsRequired ->
            "Camera access must be enabled in system settings."
    }
}
