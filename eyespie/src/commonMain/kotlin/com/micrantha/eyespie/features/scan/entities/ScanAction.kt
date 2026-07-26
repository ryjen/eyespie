package com.micrantha.eyespie.features.scan.entities

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

class CameraAuthorizationRequestId internal constructor()

sealed interface ScanAction {
    data object RefreshCameraAuthorization : ScanAction
    data class RequestCameraAuthorization(
        val requestId: CameraAuthorizationRequestId = CameraAuthorizationRequestId(),
    ) : ScanAction

    data class CameraAuthorizationLoaded(
        val previous: CapabilityAuthorization,
        val authorization: CapabilityAuthorization,
    ) : ScanAction

    data class CameraAuthorizationRequestResolved(
        val requestId: CameraAuthorizationRequestId,
        val authorization: CapabilityAuthorization,
    ) : ScanAction

    data class CameraAuthorizationRequestFailed(
        val requestId: CameraAuthorizationRequestId,
    ) : ScanAction

    data object OpenCameraSettings : ScanAction

    data class SaveScan(
        val image: CameraImage,
        val path: Path,
    ) : ScanAction

    data object ScanError : ScanAction

    data object Back : ScanAction
}
