package com.micrantha.eyespie.platform.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import okio.Path

actual class LoadCameraImageUseCaseImpl(
    private val context: Context
) : LoadCameraImageUseCase {
    override operator fun invoke(path: Path, regionOfInterest: Rect?): Result<CameraImage> = try {
        val uri = path.toFile().toUri()
        val rotation = getCameraImageRotation(uri)
        val image = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it)
        }
        val result = PlatformCameraImage(
            _bitmap = image,
            _width = image.width,
            _height = image.height,
            _rotation = exifOrientationToDegrees(rotation),
            regionOfInterest = regionOfInterest?.toAndroidRectF()
        )
        Result.success(result)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    private fun getCameraImageRotation(uri: Uri) =
        context.contentResolver.openInputStream(uri).use {
            val exif = ExifInterface(it!!)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        }
}
