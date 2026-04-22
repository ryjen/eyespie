package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.geometry.Rect
import okio.Path

expect class LoadCameraImageUseCase {
    operator fun invoke(path: Path, regionOfInterest: Rect? = null): Result<CameraImage>
}
