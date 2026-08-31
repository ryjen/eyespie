package com.micrantha.eyespie.presentation

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun decodeThumbnail(bytes: ByteArray): ImageBitmap? = runCatching {
    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()
