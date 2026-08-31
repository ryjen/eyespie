package com.micrantha.eyespie.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes cached display thumbnail bytes (a device-local UX cache, never matching
 * authority) into a Compose image. Returns null if the bytes cannot be decoded so
 * callers can fall back to an icon avatar. Platform-specific (skiko-backed).
 */
expect fun decodeThumbnail(bytes: ByteArray): ImageBitmap?

@Composable
fun ThumbnailOrAvatar(
    thumbnail: ByteArray?,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
) {
    val bitmap = thumbnail?.let { decodeThumbnail(it) }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.fillMaxSize())
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            avatar()
        }
    }
}
