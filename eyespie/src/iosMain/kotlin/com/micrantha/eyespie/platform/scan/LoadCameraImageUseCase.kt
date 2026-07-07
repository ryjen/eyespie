package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.geometry.Rect
import okio.Path

actual class LoadCameraImageUseCaseImpl : LoadCameraImageUseCase {
    override operator fun invoke(path: Path, regionOfInterest: Rect?): Result<CameraImage> =
        Result.failure(UnsupportedOperationException("Not implemented on iOS"))
}
