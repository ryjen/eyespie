package com.micrantha.eyespie.features.onboarding.data

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import dev.icerock.moko.permissions.PermissionsController
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType

@Suppress("UNUSED_PARAMETER")
internal actual suspend fun currentCameraAuthorization(
    permissionsController: PermissionsController,
    previous: CapabilityAuthorization,
): CapabilityAuthorization = when (
    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
) {
    AVAuthorizationStatusAuthorized -> CapabilityAuthorization.Granted
    AVAuthorizationStatusNotDetermined -> CapabilityAuthorization.NotRequested
    AVAuthorizationStatusDenied -> CapabilityAuthorization.SettingsRequired
    AVAuthorizationStatusRestricted -> CapabilityAuthorization.Restricted
    else -> CapabilityAuthorization.Unsupported
}
