package com.micrantha.eyespie.platform.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

@Composable
expect fun CameraScanner(
    modifier: Modifier,
    regionOfInterest: Rect? = null,
    onCameraImage: suspend (CameraImage) -> Unit
)
