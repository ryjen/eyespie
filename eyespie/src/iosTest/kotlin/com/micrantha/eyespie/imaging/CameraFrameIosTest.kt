package com.micrantha.eyespie.imaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

private const val PACKED_BLACK = 0x000000
private const val PACKED_WHITE = 0xFFFFFF
private const val PACKED_MID_GRAY = 0x808080

class CameraFrameIosTest {
    @Test
    fun videoRangeNeutralEndpointsConvertWithoutClippingBias() {
        assertEquals(
            PACKED_BLACK,
            yuvToBgraPixel(
                y = 16u,
                u = 128u,
                v = 128u,
                range = CameraYuvRange.VIDEO,
            ),
        )
        assertEquals(
            PACKED_WHITE,
            yuvToBgraPixel(
                y = 235u,
                u = 128u,
                v = 128u,
                range = CameraYuvRange.VIDEO,
            ),
        )
    }

    @Test
    fun fullRangeNeutralEndpointsUseTheFullLumaDomain() {
        assertEquals(
            PACKED_BLACK,
            yuvToBgraPixel(
                y = 0u,
                u = 128u,
                v = 128u,
                range = CameraYuvRange.FULL,
            ),
        )
        assertEquals(
            PACKED_WHITE,
            yuvToBgraPixel(
                y = 255u,
                u = 128u,
                v = 128u,
                range = CameraYuvRange.FULL,
            ),
        )
        assertEquals(
            PACKED_MID_GRAY,
            yuvToBgraPixel(
                y = 128u,
                u = 128u,
                v = 128u,
                range = CameraYuvRange.FULL,
            ),
        )
    }
    @Test
    fun rotationsPreservePixelsWithoutPerPixelAllocations() {
        // Width 2, height 3, with distinct BGRA values for every pixel.
        val source = ByteArray(2 * 3 * 4) { offset ->
            (offset + 1).toByte()
        }
        val cases = listOf(
            Triple(90, 3 to 2, listOf(4, 2, 0, 5, 3, 1)),
            Triple(180, 2 to 3, listOf(5, 4, 3, 2, 1, 0)),
            Triple(270, 3 to 2, listOf(1, 3, 5, 0, 2, 4)),
        )
        for ((degrees, dimensions, pixelOrder) in cases) {
            val result = rotateBgra(source, 2, 3, degrees)
            assertEquals(dimensions.first, result.width)
            assertEquals(dimensions.second, result.height)
            pixelOrder.forEachIndexed { destinationPixel, sourcePixel ->
                assertContentEquals(
                    source.copyOfRange(sourcePixel * 4, (sourcePixel + 1) * 4),
                    result.bytes.copyOfRange(destinationPixel * 4, (destinationPixel + 1) * 4),
                    "rotation $degrees changed BGRA pixel $sourcePixel",
                )
            }
        }
    }

    @Test
    fun noRotationKeepsOriginalBuffer() {
        val pixels = ByteArray(2 * 2 * 4) { it.toByte() }
        val result = rotateBgra(pixels, 2, 2, 0)
        assertEquals(2, result.width)
        assertEquals(2, result.height)
        assertContentEquals(pixels, result.bytes)
    }

}
