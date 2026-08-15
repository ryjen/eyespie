package com.micrantha.eyespie.imaging

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform camera surface for an explicit user-initiated still capture.
 *
 * The platform implementation owns camera permission, preview/session lifecycle, and native image
 * buffers. Only an app-owned [CapturedImage] crosses back into common code.
 */
@Composable
expect fun CameraCapture(
    modifier: Modifier = Modifier,
    onCameraError: (Throwable) -> Unit = {},
    onCaptured: (CapturedImage) -> Unit,
    captureButton: @Composable ((capture: () -> Unit) -> Unit),
)
