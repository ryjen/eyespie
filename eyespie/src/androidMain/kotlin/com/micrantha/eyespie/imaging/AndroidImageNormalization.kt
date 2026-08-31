package com.micrantha.eyespie.imaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Android helpers for normalizing captured images to their visual (upright)
 * orientation before they are downscaled, cached, or embedded.
 *
 * CameraX writes a JPEG whose pixels are in the sensor's native orientation and
 * records the intended display rotation in EXIF. Consumers that decode with
 * [BitmapFactory] ignore EXIF, so an upright-on-screen photo becomes rotated in
 * memory. Applying the EXIF rotation once, at the boundary, keeps the local
 * thumbnail and the matching embedding consistent with what the player sees.
 */

/** Degrees to rotate a bitmap clockwise so its EXIF orientation reads upright. */
private fun exifRotationDegrees(orientation: Int): Int = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}

/**
 * Returns the captured image re-encoded with EXIF orientation applied (pixels
 * upright), or null if the bytes cannot be read. The result carries no EXIF
 * rotation, so downstream decode is stable across platforms.
 */
fun normalizeToUpright(source: ByteArray): ByteArray? {
    val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size) ?: return null
    val rotation = runCatching {
        ByteArrayInputStream(source).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            ).let(::exifRotationDegrees)
        }
    }.getOrDefault(0)

    val upright = if (rotation == 0) bitmap else rotateBitmap(bitmap, rotation)
    if (upright !== bitmap) bitmap.recycle()

    val out = ByteArrayOutputStream()
    if (!upright.compress(Bitmap.CompressFormat.JPEG, 90, out)) return null
    val bytes = out.toByteArray()
    if (upright !== bitmap) upright.recycle()
    return bytes
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    val radians = Math.toRadians(degrees.toDouble())
    val cos = kotlin.math.abs(kotlin.math.cos(radians))
    val sin = kotlin.math.abs(kotlin.math.sin(radians))
    val width = bitmap.width
    val height = bitmap.height
    val newWidth = (width * cos + height * sin).toInt()
    val newHeight = (width * sin + height * cos).toInt()

    val rotated = Bitmap.createBitmap(newWidth, newHeight, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(rotated)
    canvas.translate(newWidth / 2f, newHeight / 2f)
    canvas.rotate(degrees.toFloat())
    canvas.translate(-width / 2f, -height / 2f)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    return rotated
}
