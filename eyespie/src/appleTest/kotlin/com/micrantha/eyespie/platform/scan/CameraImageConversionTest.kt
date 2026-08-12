package com.micrantha.eyespie.platform.scan

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraImageConversionTest {
    @Test
    fun neutralVideoRangeBlackAndWhiteConvertWithoutOverflow() {
        assertEquals(0x000000, yuvToBgraPixel(16u, 128u, 128u))
        assertEquals(0xFFFFFF, yuvToBgraPixel(235u, 128u, 128u))
        assertEquals(0x000000, yuvToBgraPixel(0u, 128u, 128u))
    }

    @Test
    fun chromaConversionUsesSignedOffsetsAndClampsChannels() {
        // BT.601 video-range approximation for red: RGB ~= (255, 1, 0).
        assertEquals(0xFF0100, yuvToBgraPixel(82u, 90u, 240u))
    }

    @Test
    fun bgraRotationPreservesPixelsAndSwapsDimensions() {
        val source = frameWithPixelIds(1, 2, 3, 4)

        val clockwise = rotateBgra(source, width = 2, height = 2, rotationDegrees = 90)
        assertEquals(2, clockwise.width)
        assertEquals(2, clockwise.height)
        assertEquals(listOf<Byte>(3, 1, 4, 2), pixelIds(clockwise))

        val counterClockwise = rotateBgra(source, width = 2, height = 2, rotationDegrees = 270)
        assertEquals(listOf<Byte>(2, 4, 1, 3), pixelIds(counterClockwise))

        val upsideDown = rotateBgra(source, width = 2, height = 2, rotationDegrees = 180)
        assertEquals(listOf<Byte>(4, 3, 2, 1), pixelIds(upsideDown))
    }

    private fun frameWithPixelIds(vararg ids: Byte): ByteArray = ByteArray(ids.size * 4).also { bytes ->
        ids.forEachIndexed { index, id ->
            val offset = index * 4
            bytes[offset] = id
            bytes[offset + 1] = 10
            bytes[offset + 2] = 20
            bytes[offset + 3] = (-1).toByte()
        }
    }

    private fun pixelIds(frame: BgraFrame): List<Byte> =
        frame.bytes.filterIndexed { index, _ -> index % 4 == 0 }
}
