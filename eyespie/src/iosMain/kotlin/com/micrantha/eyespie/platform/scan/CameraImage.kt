package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.graphics.ImageBitmap

/** Placeholder image type used until the native iOS camera pipeline is restored. */
actual class PlatformCameraImage : CameraImage {
    override val width: Int = 0
    override val height: Int = 0

    override fun toByteArray(): ByteArray = ByteArray(0)

    override fun toImageBitmap(): ImageBitmap =
        error("Camera images are not yet available on iOS")
}
