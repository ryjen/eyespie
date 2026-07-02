package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.graphics.ImageBitmap

interface CameraImage {

    val width: Int
    val height: Int

    fun toByteArray(): ByteArray

    fun toImageBitmap(): ImageBitmap
}

expect class PlatformCameraImage : CameraImage
