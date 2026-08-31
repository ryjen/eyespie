package com.micrantha.eyespie.imaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

/**
 * Android [ImageRotator] using the platform bitmap APIs. Rotates by a multiple
 * of 90 degrees and re-encodes as JPEG.
 */
object AndroidImageRotator : ImageRotator {
    override fun rotate(image: CapturedImage, degrees: Int): CapturedImage? {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return null
        val src = BitmapFactory.decodeByteArray(image.encodedBytes(), 0, image.byteSize) ?: return null
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        src.recycle()
        val out = ByteArrayOutputStream()
        if (!rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)) return null
        rotated.recycle()
        return CapturedImage.fromEncoded(out.toByteArray())
    }
}
