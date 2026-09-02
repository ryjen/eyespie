package com.micrantha.eyespie.imaging

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

/**
 * iOS [ThumbnailCodec] backed by skiko. Decodes the captured target image,
 * downscales to at most [maxDimension] on the longest edge, and re-encodes a
 * compressed PNG. The result is a device-local display cache only and must never
 * be used for matching or bundling.
 */
object SkiaThumbnailCodec : ThumbnailCodec {
    override fun encode(image: CapturedImage, maxDimension: Int): ByteArray? {
        val source = runCatching { SkiaImage.makeFromEncoded(image.encodedBytes()) }.getOrNull() ?: return null
        try {
            val srcW = source.width
            val srcH = source.height
            if (srcW <= 0 || srcH <= 0) return null

            val scale = minOf(1f, maxDimension.toFloat() / maxOf(srcW, srcH))
            val dstW = maxOf(1, (srcW * scale).toInt())
            val dstH = maxOf(1, (srcH * scale).toInt())

            val bitmap = Bitmap()
            try {
                if (!bitmap.allocN32Pixels(dstW, dstH, false)) return null
                val canvas = Canvas(bitmap)
                canvas.clear(0x00000000)
                canvas.drawImageRect(
                    source,
                    Rect.makeXYWH(0f, 0f, srcW.toFloat(), srcH.toFloat()),
                    Rect.makeXYWH(0f, 0f, dstW.toFloat(), dstH.toFloat()),
                    Paint(),
                )

                val rendered = SkiaImage.makeFromBitmap(bitmap)
                try {
                    val data = rendered.encodeToData(EncodedImageFormat.PNG) ?: return null
                    return data.bytes
                } finally {
                    rendered.close()
                }
            } finally {
                bitmap.close()
            }
        } finally {
            source.close()
        }
    }
}
