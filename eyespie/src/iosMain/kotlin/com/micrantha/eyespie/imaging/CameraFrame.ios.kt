@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.imaging

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreVideo.CVImageBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetBaseAddressOfPlane
import platform.CoreVideo.CVPixelBufferGetBytesPerRow
import platform.CoreVideo.CVPixelBufferGetBytesPerRowOfPlane
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetPixelFormatType
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight

private const val BGRA_BYTES_PER_PIXEL = 4
private const val CHANNEL_MAX = 255
private const val CHROMA_OFFSET = 128
private const val FIXED_POINT_SHIFT = 8
private const val FIXED_POINT_ROUNDING = 128

private const val VIDEO_LUMA_OFFSET = 16
private const val VIDEO_LUMA_SCALE = 298
private const val VIDEO_RED_CHROMA_SCALE = 409
private const val VIDEO_GREEN_U_SCALE = 100
private const val VIDEO_GREEN_V_SCALE = 208
private const val VIDEO_BLUE_CHROMA_SCALE = 516

private const val FULL_LUMA_SCALE = 256
private const val FULL_RED_CHROMA_SCALE = 359
private const val FULL_GREEN_U_SCALE = 88
private const val FULL_GREEN_V_SCALE = 183
private const val FULL_BLUE_CHROMA_SCALE = 454

internal enum class CameraYuvRange {
    VIDEO,
    FULL,
}

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
        val frame = when (CVPixelBufferGetPixelFormatType(data)) {
            kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange ->
                yuvToBgra(data, width, height, CameraYuvRange.VIDEO)

            kCVPixelFormatType_420YpCbCr8BiPlanarFullRange ->
                yuvToBgra(data, width, height, CameraYuvRange.FULL)

            kCVPixelFormatType_32BGRA ->
                copyBgra(data, width, height)

            else -> throw IllegalStateException("unsupported camera pixel format")
        }
        orientCameraFrame(frame, orientation)
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
    range: CameraYuvRange,
): OwnedCameraFrame {
    val yPlane = CVPixelBufferGetBaseAddressOfPlane(yuvBuffer, 0u)!!.reinterpret<ByteVar>()
    val uvPlane = CVPixelBufferGetBaseAddressOfPlane(yuvBuffer, 1u)!!.reinterpret<ByteVar>()
    val yBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(yuvBuffer, 0u).toInt()
    val uvBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(yuvBuffer, 1u).toInt()

    val bgraBuffer = ByteArray(width * height * BGRA_BYTES_PER_PIXEL)
    var bgraIndex = 0
    for (yRow in 0 until height) {
        val yStart = yRow * yBytesPerRow
        val uvStart = (yRow / 2) * uvBytesPerRow
        for (x in 0 until width) {
            val y = yPlane[yStart + x].toUByte()
            val uvOffset = (x / 2) * 2
            val u = uvPlane[uvStart + uvOffset].toUByte()
            val v = uvPlane[uvStart + uvOffset + 1].toUByte()
            val pixel = yuvToBgraPixel(y, u, v, range)
            bgraBuffer[bgraIndex++] = (pixel and 0xFF).toByte()
            bgraBuffer[bgraIndex++] = ((pixel shr 8) and 0xFF).toByte()
            bgraBuffer[bgraIndex++] = ((pixel shr 16) and 0xFF).toByte()
            bgraBuffer[bgraIndex++] = CHANNEL_MAX.toByte()
        }
    }
    return OwnedCameraFrame(bgraBuffer, width, height)
}

private fun copyBgra(
    pixelBuffer: CVImageBufferRef,
    width: Int,
    height: Int,
): OwnedCameraFrame {
    val source = CVPixelBufferGetBaseAddress(pixelBuffer)?.reinterpret<ByteVar>()
        ?: throw IllegalStateException("camera BGRA buffer has no base address")
    val sourceBytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer).toInt()
    val destinationBytesPerRow = width * BGRA_BYTES_PER_PIXEL
    val bytes = ByteArray(destinationBytesPerRow * height)

    for (row in 0 until height) {
        val sourceStart = row * sourceBytesPerRow
        val destinationStart = row * destinationBytesPerRow
        for (offset in 0 until destinationBytesPerRow) {
            bytes[destinationStart + offset] = source[sourceStart + offset]
        }
    }
    return OwnedCameraFrame(bytes, width, height)
}

internal fun yuvToBgraPixel(
    y: UByte,
    u: UByte,
    v: UByte,
    range: CameraYuvRange = CameraYuvRange.VIDEO,
): Int {
    val yValue = y.toInt()
    val uDelta = u.toInt() - CHROMA_OFFSET
    val vDelta = v.toInt() - CHROMA_OFFSET

    val (luma, red, greenU, greenV, blue) = when (range) {
        CameraYuvRange.VIDEO -> listOf(
            (yValue - VIDEO_LUMA_OFFSET).coerceAtLeast(0) * VIDEO_LUMA_SCALE,
            VIDEO_RED_CHROMA_SCALE,
            VIDEO_GREEN_U_SCALE,
            VIDEO_GREEN_V_SCALE,
            VIDEO_BLUE_CHROMA_SCALE,
        )
        CameraYuvRange.FULL -> listOf(
            yValue * FULL_LUMA_SCALE,
            FULL_RED_CHROMA_SCALE,
            FULL_GREEN_U_SCALE,
            FULL_GREEN_V_SCALE,
            FULL_BLUE_CHROMA_SCALE,
        )
    }

    val r = ((luma + red * vDelta + FIXED_POINT_ROUNDING) shr FIXED_POINT_SHIFT)
        .coerceIn(0, CHANNEL_MAX)
    val g = ((luma - greenU * uDelta - greenV * vDelta + FIXED_POINT_ROUNDING) shr FIXED_POINT_SHIFT)
        .coerceIn(0, CHANNEL_MAX)
    val b = ((luma + blue * uDelta + FIXED_POINT_ROUNDING) shr FIXED_POINT_SHIFT)
        .coerceIn(0, CHANNEL_MAX)
    return b or (g shl FIXED_POINT_SHIFT) or (r shl (FIXED_POINT_SHIFT * 2))
}

private fun orientCameraFrame(
    frame: OwnedCameraFrame,
    orientation: CGImagePropertyOrientation,
): OwnedCameraFrame =
    when (orientation) {
        kCGImagePropertyOrientationDown -> rotateBgra(frame.bytes, frame.width, frame.height, 180)
        kCGImagePropertyOrientationLeft -> rotateBgra(frame.bytes, frame.width, frame.height, 270)
        kCGImagePropertyOrientationRight -> rotateBgra(frame.bytes, frame.width, frame.height, 90)
        else -> frame
    }

internal fun rotateBgra(
    source: ByteArray,
    width: Int,
    height: Int,
    rotationDegrees: Int,
): OwnedCameraFrame {
    require(source.size == width * height * BGRA_BYTES_PER_PIXEL) {
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
            val sourceIndex = (y * width + x) * BGRA_BYTES_PER_PIXEL
            val (destinationX, destinationY) = when (normalized) {
                90 -> (height - 1 - y) to x
                180 -> (width - 1 - x) to (height - 1 - y)
                else -> y to (width - 1 - x)
            }
            val destinationIndex = (destinationY * destinationWidth + destinationX) * BGRA_BYTES_PER_PIXEL
            source.copyInto(
                destination,
                destinationIndex,
                sourceIndex,
                sourceIndex + BGRA_BYTES_PER_PIXEL,
            )
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
