package com.micrantha.eyespie.imaging

/** Platform-neutral camera availability exposed to feature presentation. */
sealed interface CameraAvailability {
    data object Requestable : CameraAvailability
    data object Ready : CameraAvailability
    data object PermissionDenied : CameraAvailability
    data object Unavailable : CameraAvailability
}
