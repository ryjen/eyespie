package com.micrantha.eyespie.imaging

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform camera surface for an explicit user-initiated still capture.
 *
 * The platform implementation owns camera permission, preview/session lifecycle, recovery actions,
 * and native image buffers. Common presentation receives only semantic availability and an
 * app-owned [CapturedImage].
 */
@Composable
expect fun CameraCapture(
    modifier: Modifier = Modifier,
    onAvailabilityChanged: (CameraAvailability) -> Unit = {},
    onCameraError: (Throwable) -> Unit = {},
    onCaptured: (CapturedImage) -> Unit,
    captureButton: @Composable ((capture: () -> Unit) -> Unit),
    recoveryButton: @Composable ((openSettings: () -> Unit) -> Unit) = {},
)
