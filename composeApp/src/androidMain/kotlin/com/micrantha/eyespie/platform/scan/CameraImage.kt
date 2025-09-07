package com.micrantha.eyespie.platform.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.micrantha.bluebell.platform.toByteArray
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

actual class CameraImage @kotlin.OptIn(ExperimentalTime::class) constructor(
    private var _image: Image? = null,
    private var _bitmap: Bitmap? = null,
    private var _width: Int,
    private var _height: Int,
    private var _rotation: Int = 0,
    private var _timestamp: Long = Clock.System.now().epochSeconds,
    private var regionOfInterest: RectF? = null,
) {

    private var imageBitmapBuffer: Bitmap? = null

    actual val width get() = _width
    actual val height get() = _height

    val timestamp get() = _timestamp
    val rotation get() = _rotation

    @OptIn(ExperimentalGetImage::class)
    fun copy(image: ImageProxy, region: RectF? = null) {
        _width = image.width
        _height = image.height
        _rotation = image.imageInfo.rotationDegrees
        _timestamp = image.imageInfo.timestamp
        regionOfInterest = region ?: regionOfInterest
        _image = image.image
        _bitmap = null
        imageBitmapBuffer = null
    }

    @kotlin.OptIn(ExperimentalTime::class)
    fun copy(
        bitmap: Bitmap,
        rotation: Int = 0,
        timestamp: Long = Clock.System.now().epochSeconds,
        region: RectF? = null
    ) {
        _width = bitmap.width
        _height = bitmap.height
        _rotation = rotation
        _timestamp = timestamp
        regionOfInterest = region ?: regionOfInterest
        _image = null
        _bitmap = bitmap
        imageBitmapBuffer = null
    }

    actual fun toImageBitmap() = toBitmap().asImageBitmap()

    actual fun toByteArray() = toBitmap().toByteArray()

    fun toBitmap(): Bitmap {
        if (imageBitmapBuffer != null) return imageBitmapBuffer!!

        val result = _image?.let { image ->
            createBitmap(_width, _height).apply {
                copyPixelsFromBuffer(
                    image.planes[0].buffer
                )
            }
        } ?: _bitmap?.let { bitmap ->
            bitmap.copy(bitmap.config!!, true)
        } ?: throw IllegalStateException("unable to convert image to bitmap")

        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())

        imageBitmapBuffer = Bitmap.createBitmap(
            result,
            0,
            0,
            _width,
            _height,
            matrix,
            false
        )

        return imageBitmapBuffer!!
    }
}
