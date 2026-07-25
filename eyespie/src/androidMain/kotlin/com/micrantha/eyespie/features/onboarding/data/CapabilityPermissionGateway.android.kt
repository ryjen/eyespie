package com.micrantha.eyespie.features.onboarding.data

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA

internal actual suspend fun currentCameraAuthorization(
    permissionsController: PermissionsController,
    previous: CapabilityAuthorization,
): CapabilityAuthorization = permissionsController
    .getPermissionState(Permission.CAMERA)
    .toCapabilityAuthorization(previous)
