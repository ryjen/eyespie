package com.micrantha.eyespie.imaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageEmbeddingTest {
    @Test
    fun capturedImageOwnsEncodedBytes() {
        val source = byteArrayOf(1, 2, 3)
        val image = CapturedImage.fromEncoded(source)
        source[0] = 9

        val exported = image.encodedBytes()
        assertEquals(1, exported[0])
        exported[0] = 8
        assertEquals(1, image.encodedBytes()[0])
    }

    @Test
    fun canonicalEmbeddingRequiresExpectedFiniteShape() {
        val values = List(IMAGE_EMBEDDING_DIMENSIONS) { index -> index / 1024f }
        assertEquals(values, canonicalImageEmbedding(values))

        assertFailsWith<IllegalArgumentException> {
            canonicalImageEmbedding(values.dropLast(1))
        }
        assertFailsWith<IllegalArgumentException> {
            canonicalImageEmbedding(values.toMutableList().apply { this[0] = Float.NaN })
        }
    }
}
