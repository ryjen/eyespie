package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

/**
 * The previous Apple scanner depended on component APIs that are not present in this repository.
 * Keep the expect/actual surface valid and fail explicitly until the native scanner is restored.
 */
@Composable
actual fun CameraScanner(
    modifier: Modifier,
    regionOfInterest: Rect?,
    onCameraError: (Throwable) -> Unit,
    onCameraImage: suspend (CameraImage) -> Unit,
) {
    LaunchedEffect(Unit) {
        onCameraError(UnsupportedOperationException("Camera scanning is not yet available on iOS"))
    }
}
