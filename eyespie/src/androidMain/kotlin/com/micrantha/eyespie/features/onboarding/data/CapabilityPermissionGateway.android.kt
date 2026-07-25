package com.micrantha.eyespie.features.onboarding.data

import android.os.Build
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

internal actual suspend fun currentCameraAuthorization(
    permissionsController: PermissionsController,
    previous: CapabilityAuthorization,
): CapabilityAuthorization = permissionsController
    .getPermissionState(Permission.CAMERA)
    .toCapabilityAuthorization(previous)

internal actual suspend fun currentNotificationAuthorization(
    permissionsController: PermissionsController,
    previous: CapabilityAuthorization,
): CapabilityAuthorization = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    CapabilityAuthorization.NotRequired
} else {
    permissionsController
        .getPermissionState(Permission.REMOTE_NOTIFICATION)
        .toCapabilityAuthorization(previous)
}
