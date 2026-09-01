package com.micrantha.eyespie.imaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Android [ThumbnailCodec] backed by the platform bitmap APIs. Decodes the
 * captured target image (normalizing EXIF orientation), aspect-fits it into a
 * square canvas, center-crops, then re-encodes a compressed PNG. The result is a
 * device-local display cache only and must never be used for matching or bundling.
 */
object SkiaThumbnailCodec : ThumbnailCodec {
    override fun encode(image: CapturedImage, maxDimension: Int): ByteArray? {
        val sourceBytes = normalizeToUpright(image.encodedBytes()) ?: image.encodedBytes()
        val src = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size) ?: return null
        val (srcW, srcH) = src.width to src.height
        if (srcW <= 0 || srcH <= 0) return null

        val side = maxDimension.coerceAtMost(maxOf(srcW, srcH)).coerceAtLeast(1)
        val scale = side.toFloat() / maxOf(srcW, srcH)
        val scaledW = (srcW * scale).toInt().coerceAtLeast(1)
        val scaledH = (srcH * scale).toInt().coerceAtLeast(1)

        val fitted = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        src.recycle()

        val finalSide = minOf(side, scaledW, scaledH)
        val cropX = ((scaledW - finalSide) / 2f).toInt()
        val cropY = ((scaledH - finalSide) / 2f).toInt()
        val square = Bitmap.createBitmap(fitted, cropX, cropY, finalSide, finalSide)
        if (fitted !== square) fitted.recycle()

        val out = ByteArrayOutputStream()
        if (!square.compress(Bitmap.CompressFormat.PNG, 100, out)) return null
        square.recycle()
        return out.toByteArray()
    }
}
