package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.geometry.Rect
import okio.Path

interface LoadCameraImageUseCase {
    operator fun invoke(path: Path, regionOfInterest: Rect? = null): Result<CameraImage>
}

expect class LoadCameraImageUseCaseImpl : LoadCameraImageUseCase
