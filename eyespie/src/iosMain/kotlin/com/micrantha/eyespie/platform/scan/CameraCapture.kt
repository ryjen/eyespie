package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.launch
import okio.Path

@Composable
actual fun CameraCapture(
    modifier: Modifier,
    regionOfInterest: Rect?,
    onCameraError: (Throwable) -> Unit,
    onCameraImage: (Path) -> Unit,
    captureButton: @Composable (() -> Unit) -> Unit
) {
    val controller = remember { IosCameraCaptureController() }
    val scope = rememberCoroutineScope()
    val onFrame: CameraScannerDispatch = remember(controller) {
        { image -> controller.updateFrame(image) }
    }

    LaunchedEffect(controller) {
        controller.prepare().onFailure(onCameraError)
    }

    CameraScanner(
        modifier = modifier,
        regionOfInterest = regionOfInterest,
        onCameraError = onCameraError,
        onCameraImage = onFrame,
    )

    captureButton {
        scope.launch {
            controller.capture()
                .onSuccess(onCameraImage)
                .onFailure(onCameraError)
        }
    }
}
