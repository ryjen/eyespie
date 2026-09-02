package com.micrantha.eyespie.presentation

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap

actual fun decodeThumbnail(bytes: ByteArray): ImageBitmap? = runCatching {
    bytes.decodeToImageBitmap()
}.getOrNull()
