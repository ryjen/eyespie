package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import okio.Path

/**
 * iOS camera capture has not yet been implemented against the current shared contract.
 * Preserve the expect/actual surface and report the unsupported capability explicitly.
 */
@Composable
actual fun CameraCapture(
    modifier: Modifier,
    regionOfInterest: Rect?,
    onCameraError: (Throwable) -> Unit,
    onCameraImage: (Path) -> Unit,
    captureButton: @Composable (() -> Unit) -> Unit,
) {
    LaunchedEffect(Unit) {
        onCameraError(UnsupportedOperationException("Camera capture is not yet available on iOS"))
    }

    captureButton { }
}
