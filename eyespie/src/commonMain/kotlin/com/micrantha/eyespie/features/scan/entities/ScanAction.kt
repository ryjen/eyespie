package com.micrantha.eyespie.features.scan.entities

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

sealed interface ScanAction {
    data object RefreshCameraAuthorization : ScanAction
    data object RequestCameraAuthorization : ScanAction
    data object CameraAuthorizationRequestStarted : ScanAction
    data class CameraAuthorizationLoaded(
        val previous: CapabilityAuthorization,
        val authorization: CapabilityAuthorization,
    ) : ScanAction

    data class CameraAuthorizationRequestResolved(
        val authorization: CapabilityAuthorization,
    ) : ScanAction

    data object CameraAuthorizationRequestFailed : ScanAction
    data object OpenCameraSettings : ScanAction

    data class SaveScan(
        val image: CameraImage,
        val path: Path,
    ) : ScanAction

    data object ScanError : ScanAction

    data object Back : ScanAction
}
