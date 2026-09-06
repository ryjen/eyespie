package com.micrantha.eyespie.presentation

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.micrantha.eyespie.imaging.normalizeToUpright

actual fun decodeThumbnail(bytes: ByteArray): ImageBitmap? = runCatching {
    val uprightBytes = normalizeToUpright(bytes) ?: bytes
    BitmapFactory.decodeByteArray(uprightBytes, 0, uprightBytes.size)?.asImageBitmap()
}.getOrNull()
