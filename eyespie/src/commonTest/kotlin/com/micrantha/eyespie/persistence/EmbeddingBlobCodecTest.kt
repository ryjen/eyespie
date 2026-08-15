package com.micrantha.eyespie.persistence

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class EmbeddingBlobCodecTest {
    @Test
    fun roundTripsFloatBitsExactly() {
        val embedding = listOf(0f, -0f, 1.25f, -3.5f, Float.MIN_VALUE, Float.MAX_VALUE)

        val decoded = EmbeddingBlobCodec.decode(EmbeddingBlobCodec.encode(embedding))

        assertContentEquals(
            embedding.map(Float::toRawBits),
            decoded.map(Float::toRawBits),
        )
    }

    @Test
    fun rejectsMalformedBlob() {
        assertFailsWith<IllegalArgumentException> {
            EmbeddingBlobCodec.decode(byteArrayOf(1, 2, 3))
        }
    }
}
