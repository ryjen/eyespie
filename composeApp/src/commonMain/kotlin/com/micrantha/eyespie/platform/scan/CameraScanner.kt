package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

typealias CameraScannerDispatch = suspend (CameraImage) -> Unit

@Composable
expect fun CameraScanner(
    modifier: Modifier,
    regionOfInterest: Rect? = null,
    onCameraImage: CameraScannerDispatch
)
