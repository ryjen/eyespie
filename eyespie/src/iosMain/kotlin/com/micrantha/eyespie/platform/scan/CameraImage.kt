package com.micrantha.eyespie.platform.scan

import androidx.compose.ui.graphics.ImageBitmap
import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.platform.toImageBitmap
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRef
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.kCGBitmapByteOrder32Little
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
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationRight

internal data class BgraFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

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
): BgraFrame {
    require(source.size == width * height * 4) {
        "BGRA byte count does not match frame dimensions"
    }

    val normalized = ((rotationDegrees % 360) + 360) % 360
    if (normalized == 0) return BgraFrame(source, width, height)
    require(normalized == 90 || normalized == 180 || normalized == 270) {
        "unsupported camera rotation: $rotationDegrees"
    }

    val destinationWidth = if (normalized == 180) width else height
    val destinationHeight = if (normalized == 180) height else width
    val destination = ByteArray(source.size)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val sourceIndex = (y * width + x) * 4
            val destinationX: Int
            val destinationY: Int
            when (normalized) {
                90 -> {
                    destinationX = height - 1 - y
                    destinationY = x
                }

                180 -> {
                    destinationX = width - 1 - x
                    destinationY = height - 1 - y
                }

                else -> {
                    destinationX = y
                    destinationY = width - 1 - x
                }
            }
            val destinationIndex = (destinationY * destinationWidth + destinationX) * 4
            source.copyInto(destination, destinationIndex, sourceIndex, sourceIndex + 4)
        }
    }

    return BgraFrame(destination, destinationWidth, destinationHeight)
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformCameraImage(
    internal val data: CVImageBufferRef,
    val orientation: CGImagePropertyOrientation
) : CameraImage {
    override val width by lazy { CVPixelBufferGetWidth(data).toInt() }
    override val height by lazy { CVPixelBufferGetHeight(data).toInt() }

    private val log by logger()
    private var encodedBytes: ByteArray? = null

    override fun toByteArray(): ByteArray {
        if (encodedBytes == null) {
            val frame = try {
                CVPixelBufferLockBaseAddress(data, 0u)
                when (CVPixelBufferGetPixelFormatType(data)) {
                    kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange -> {
                        val bgra = yuvToBgra(data)
                        when (orientation) {
                            kCGImagePropertyOrientationDown -> rotateBgra(bgra, width, height, 180)
                            kCGImagePropertyOrientationLeft -> rotateBgra(bgra, width, height, 270)
                            kCGImagePropertyOrientationRight -> rotateBgra(bgra, width, height, 90)
                            else -> BgraFrame(bgra, width, height)
                        }
                    }

                    else -> throw IllegalStateException("invalid pixel format")
                }
            } catch (err: Throwable) {
                log.error(err) { "converting camera image" }
                throw err
            } finally {
                CVPixelBufferUnlockBaseAddress(data, 0u)
            }

            encodedBytes = frame.toPng()
        }
        return encodedBytes!!
    }

    fun asCGImage(): CGImageRef? {
        var colorSpace: CGColorSpaceRef? = null
        var context: CGContextRef? = null
        return try {
            CVPixelBufferLockBaseAddress(data, 0u)

            val bytesPerRow = CVPixelBufferGetBytesPerRow(data)
            val baseAddress = CVPixelBufferGetBaseAddress(data)

            colorSpace = CGColorSpaceCreateDeviceRGB()
            context = CGBitmapContextCreate(
                baseAddress,
                width.toULong(),
                height.toULong(),
                8u,
                bytesPerRow,
                colorSpace,
                kCGBitmapByteOrder32Little or CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value
            )
            CGBitmapContextCreateImage(context)

        } finally {
            CGColorSpaceRelease(colorSpace)
            CGContextRelease(context)
            CVPixelBufferUnlockBaseAddress(data, 0u)
        }
    }

    private fun yuvToBgra(yuvBuffer: CVImageBufferRef): ByteArray {
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
                val y = yPlane[yStart + x]
                val uvOffset = (x / 2) * 2
                val u = uvPlane[uvStart + uvOffset]
                val v = uvPlane[uvStart + uvOffset + 1]
                val bgra = yuvToBgraPixel(y.toUByte(), u.toUByte(), v.toUByte())

                bgraBuffer[bgraIndex++] = (bgra and 0xFF).toByte()
                bgraBuffer[bgraIndex++] = ((bgra shr 8) and 0xFF).toByte()
                bgraBuffer[bgraIndex++] = ((bgra shr 16) and 0xFF).toByte()
                bgraBuffer[bgraIndex++] = 0xFF.toByte()
            }
        }

        return bgraBuffer
    }

    private fun BgraFrame.toPng(): ByteArray {
        val bitmap = Bitmap()
        try {
            check(bitmap.allocN32Pixels(width, height, opaque = true)) {
                "unable to allocate camera bitmap"
            }
            check(bitmap.installPixels(bytes)) {
                "unable to install camera bitmap pixels"
            }

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

    override fun toImageBitmap(): ImageBitmap = toByteArray().toImageBitmap()
}
