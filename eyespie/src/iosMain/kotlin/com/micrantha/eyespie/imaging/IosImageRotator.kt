package com.micrantha.eyespie.imaging

import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIGraphicsEndImageContext
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned

/**
 * iOS [ImageRotator] using UIKit. Rotates by a multiple of 90 degrees and
 * re-encodes as PNG via the standard image context.
 */
object IosImageRotator : ImageRotator {
    override fun rotate(image: CapturedImage, degrees: Int): CapturedImage? {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return null
        val source = image.encodedBytes().toUIImage()
        val rotated = source.rotate(degrees = normalized.toDouble()) ?: return null
        val data = UIImagePNGRepresentation(rotated) ?: return null
        return CapturedImage.fromEncoded(data.toByteArray())
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun UIImage.rotate(degrees: Double): UIImage? {
        val radians = degrees * kotlin.math.PI / 180.0
        val srcW = this.size.useContents { width }
        val srcH = this.size.useContents { height }
        val swap = (normalizedDegrees(degrees.toInt()) % 180) != 0
        val dstW = if (swap) srcH else srcW
        val dstH = if (swap) srcW else srcH
        val dstSize = CGSizeMake(dstW, dstH)

        UIGraphicsBeginImageContextWithOptions(dstSize, false, this.scale)
        val context = UIGraphicsGetCurrentContext() ?: run {
            UIGraphicsEndImageContext()
            return null
        }
        context.translateCTM(dstW / 2.0, dstH / 2.0)
        context.rotateCTM(radians)
        this.drawInRect(CGRectMake(-srcW / 2.0, -srcH / 2.0, srcW, srcH))
        val result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result
    }

    private fun normalizedDegrees(degrees: Int): Int = ((degrees % 360) + 360) % 360

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toULong().toInt()
        val pointer = this.bytes?.reinterpret<ByteVar>() ?: return ByteArray(0)
        return ByteArray(length) { index -> pointer[index] }
    }
}
