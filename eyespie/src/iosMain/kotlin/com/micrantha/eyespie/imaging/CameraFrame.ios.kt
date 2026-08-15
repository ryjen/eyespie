@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.imaging

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreVideo.CVImageBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddressOfPlane
import platform.CoreVideo.CVPixelBufferGetBytesPerRowOfPlane
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetPixelFormatType
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight

internal data class OwnedCameraFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

internal fun copyCameraFrame(
    data: CVImageBufferRef,
    orientation: CGImagePropertyOrientation,
): OwnedCameraFrame {
    val width = CVPixelBufferGetWidth(data).toInt()
    val height = CVPixelBufferGetHeight(data).toInt()
    val lockStatus = CVPixelBufferLockBaseAddress(data, 0u)
    check(lockStatus == 0) { "unable to lock camera pixel buffer: $lockStatus" }

    return try {
        when (CVPixelBufferGetPixelFormatType(data)) {
            kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange -> {
                val bgra = yuvToBgra(data, width, height)
                when (orientation) {
                    kCGImagePropertyOrientationDown -> rotateBgra(bgra, width, height, 180)
                    kCGImagePropertyOrientationLeft -> rotateBgra(bgra, width, height, 270)
                    kCGImagePropertyOrientationRight -> rotateBgra(bgra, width, height, 90)
                    else -> OwnedCameraFrame(bgra, width, height)
                }
            }

            else -> throw IllegalStateException("unsupported camera pixel format")
        }
    } finally {
        CVPixelBufferUnlockBaseAddress(data, 0u)
    }
}

internal fun OwnedCameraFrame.toCapturedImage(): CapturedImage =
    CapturedImage.fromEncoded(toPng())

private fun yuvToBgra(
    yuvBuffer: CVImageBufferRef,
    width: Int,
    height: Int,
): ByteArray {
    val yPlane = CVPixelBufferGetBaseAddressOfPlane(yuvBuffer, 0u)!!.reinterpret<ByteVar>()
    val uvPlane = CVPixelBufferGetBaseAddressOfPlane(yuvBuffer, 1u)!!.reinterpret<ByteVar>()
    val yBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(yuvBuffer, 0u).toInt()
    val uvBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(yuvBuffer, 1u).toInt()

    val bgraBuffer = ByteArray(width * height * 4)
    var bgraIndex = 0
    for (yRow in 0 until height) {
        val yStart = yRow * yBytesPerRow
        val uvStart = (yRow / 2) * uvBytesPerRow
        for (x in 0 until width) {
            val y = yPlane[yStart + x].toUByte()
            val uvOffset = (x / 2) * 2
            val u = uvPlane[uvStart + uvOffset].toUByte()
            val v = uvPlane[uvStart + uvOffset + 1].toUByte()
            val pixel = yuvToBgraPixel(y, u, v)
            bgraBuffer[bgraIndex++] = (pixel and 0xFF).toByte()
            bgraBuffer[bgraIndex++] = ((pixel shr 8) and 0xFF).toByte()
            bgraBuffer[bgraIndex++] = ((pixel shr 16) and 0xFF).toByte()
            bgraBuffer[bgraIndex++] = 0xFF.toByte()
        }
    }
    return bgraBuffer
}

internal fun yuvToBgraPixel(y: UByte, u: UByte, v: UByte): Int {
    val c = (y.toInt() - 16).coerceAtLeast(0)
    val d = u.toInt() - 128
    val e = v.toInt() - 128
    val r = ((298 * c + 409 * e + 128) shr 8).coerceIn(0, 255)
    val g = ((298 * c - 100 * d - 208 * e + 128) shr 8).coerceIn(0, 255)
    val b = ((298 * c + 516 * d + 128) shr 8).coerceIn(0, 255)
    return b or (g shl 8) or (r shl 16)
}

internal fun rotateBgra(
    source: ByteArray,
    width: Int,
    height: Int,
    rotationDegrees: Int,
): OwnedCameraFrame {
    require(source.size == width * height * 4) {
        "BGRA byte count does not match frame dimensions"
    }
    val normalized = ((rotationDegrees % 360) + 360) % 360
    if (normalized == 0) return OwnedCameraFrame(source, width, height)
    require(normalized == 90 || normalized == 180 || normalized == 270) {
        "unsupported camera rotation: $rotationDegrees"
    }

    val destinationWidth = if (normalized == 180) width else height
    val destinationHeight = if (normalized == 180) height else width
    val destination = ByteArray(source.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val sourceIndex = (y * width + x) * 4
            val (destinationX, destinationY) = when (normalized) {
                90 -> (height - 1 - y) to x
                180 -> (width - 1 - x) to (height - 1 - y)
                else -> y to (width - 1 - x)
            }
            val destinationIndex = (destinationY * destinationWidth + destinationX) * 4
            source.copyInto(destination, destinationIndex, sourceIndex, sourceIndex + 4)
        }
    }
    return OwnedCameraFrame(destination, destinationWidth, destinationHeight)
}

private fun OwnedCameraFrame.toPng(): ByteArray {
    val bitmap = Bitmap()
    try {
        check(bitmap.allocN32Pixels(width, height, opaque = true)) {
            "unable to allocate camera bitmap"
        }
        check(bitmap.installPixels(bytes)) { "unable to install camera bitmap pixels" }
        val image = Image.makeFromBitmap(bitmap)
        try {
            val data = image.encodeToData(EncodedImageFormat.PNG)
                ?: throw IllegalStateException("unable to encode camera bitmap")
            try {
                return data.bytes
            } finally {
                data.close()
            }
        } finally {
            image.close()
        }
    } finally {
        bitmap.close()
    }
}
