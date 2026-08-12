package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import okio.Path

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    regionOfInterest: Rect?,
    onCameraError: (Throwable) -> Unit,
    onCameraImage: (Path) -> Unit,
    captureButton: @Composable (() -> Unit) -> Unit
) {
    CameraScanner(
        modifier = modifier,
        regionOfInterest = regionOfInterest,
        onCameraError = onCameraError,
        onCameraImage = {},
    )

    captureButton {
        onCameraError(
            UnsupportedOperationException("Still-image capture is not implemented on iOS")
        )
    }
}
