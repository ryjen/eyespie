package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import okio.Path

@Composable
expect fun CameraCapture(
    modifier: Modifier = Modifier,
    regionOfInterest: Rect? = null,
    onCameraError: (Throwable) -> Unit = {},
    onCameraImage: (Path) -> Unit,
    captureButton: @Composable (() -> Unit) -> Unit
)
