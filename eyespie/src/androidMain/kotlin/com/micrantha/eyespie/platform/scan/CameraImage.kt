package com.micrantha.eyespie.platform.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.micrantha.bluebell.platform.toByteArray
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

actual class PlatformCameraImage @kotlin.OptIn(ExperimentalTime::class) constructor(
    private var _bitmap: Bitmap? = null,
    private var _width: Int,
    private var _height: Int,
    private var _rotation: Int? = null,
    private var _timestamp: Long = Clock.System.now().epochSeconds,
    private var regionOfInterest: RectF? = null,
) : CameraImage {

    private var imageBitmapBuffer: Bitmap? = null
    private var mediaImage: MPImage? = null

    override val width get() = _width
    override val height get() = _height

    val timestamp get() = _timestamp
    val rotation get() = _rotation

    /**
     * Copies the ImageProxy pixels into app-owned memory. The caller still owns the proxy and
     * must close it after this method returns.
     */
    fun copy(image: ImageProxy, region: RectF? = null) {
        _width = image.width
        _height = image.height
        _rotation = image.imageInfo.rotationDegrees
        _timestamp = image.imageInfo.timestamp
        regionOfInterest = region ?: regionOfInterest
        _bitmap = image.toBitmap()
        imageBitmapBuffer = null
        mediaImage = null
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
        _bitmap = bitmap
        imageBitmapBuffer = null
        mediaImage = null
    }

    override fun toImageBitmap() = toBitmap().asImageBitmap()

    override fun toByteArray() = toBitmap().toByteArray()

    val processingOptions: ImageProcessingOptions
        get() = ImageProcessingOptions.builder().apply {
            rotation?.let { setRotationDegrees(it) }
            regionOfInterest?.let { setRegionOfInterest(it) }
        }.build()

    /**
     * MediaPipe receives the unrotated owned bitmap and applies rotation/ROI through
     * ImageProcessingOptions. Presentation conversions use [toBitmap] instead.
     */
    fun asMPImage(): MPImage {
        if (mediaImage != null) return mediaImage!!

        val bitmap = _bitmap ?: throw IllegalStateException("camera image has no owned bitmap")
        mediaImage = BitmapImageBuilder(bitmap).build()
        return mediaImage!!
    }

    fun toBitmap(): Bitmap {
        if (imageBitmapBuffer != null) return imageBitmapBuffer!!

        val bitmap = _bitmap ?: throw IllegalStateException("unable to convert image to bitmap")
        imageBitmapBuffer = if (rotation == null || rotation == 0) {
            bitmap
        } else {
            rotate(bitmap, rotation!!)
        }
        return imageBitmapBuffer!!
    }

    fun resize(width: Int, height: Int): PlatformCameraImage {
        val resized = toBitmap().scale(width, height, false)
        _width = width
        _height = height
        _rotation = 0
        _bitmap = resized
        imageBitmapBuffer = resized
        mediaImage = null
        return this
    }

    private fun rotate(bitmap: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            false
        )
    }
}
