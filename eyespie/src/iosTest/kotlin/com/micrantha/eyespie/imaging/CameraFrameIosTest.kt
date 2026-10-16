package com.micrantha.eyespie.imaging

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
