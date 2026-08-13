package com.micrantha.eyespie.domain.entities

import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EmbeddingTest {

    @Test
    fun canonicalEmbeddingRoundTripsBinaryAndPgvector() {
        val values = List(ImageEmbeddingContract.dimensions) { index ->
            (index - 512) / 1024f
        }

        val embedding = values.toCanonicalEmbedding()
        val vector = embedding.toPostgresVector()
        val decoded = vector.toPostgresEmbedding()

        assertEquals(ImageEmbeddingContract.encodedBytes, embedding.size)
        assertEquals(values, embedding.floats())
        assertEquals(embedding, decoded)
    }

    @Test
    fun binaryEncodingIsExplicitBigEndianFloat32() {
        val values = MutableList(ImageEmbeddingContract.dimensions) { 0f }
        values[0] = 1f

        val embedding = values.toCanonicalEmbedding()

        assertTrue(embedding.hex().startsWith("3f800000"))
    }

    @Test
    fun canonicalEmbeddingRejectsWrongDimension() {
        assertFailsWith<InvalidEmbeddingException> {
            listOf(1f, 2f).toCanonicalEmbedding()
        }
    }

    @Test
    fun canonicalEmbeddingRejectsNonFiniteValues() {
        val values = MutableList(ImageEmbeddingContract.dimensions) { 0f }
        values[10] = Float.NaN

        assertFailsWith<InvalidEmbeddingException> {
            values.toCanonicalEmbedding()
        }
    }

    @Test
    fun pgvectorParserRejectsMalformedAndWrongDimensionValues() {
        assertFailsWith<InvalidEmbeddingException> {
            "not-a-vector".toPostgresEmbedding()
        }
        assertFailsWith<InvalidEmbeddingException> {
            "[1,2]".toPostgresEmbedding()
        }
        assertFailsWith<InvalidEmbeddingException> {
            "[1,wat]".toPostgresEmbedding()
        }
    }

    @Test
    fun binaryDecoderRejectsPartialFloat() {
        assertFailsWith<InvalidEmbeddingException> {
            byteArrayOf(1, 2, 3).toByteString().floats()
        }
    }

    @Test
    fun cosineSimilarityRequiresCanonicalInputs() {
        val canonical = List(ImageEmbeddingContract.dimensions) { if (it == 0) 1f else 0f }
            .toCanonicalEmbedding()

        assertEquals(1f, canonical.cosineSimilarity(canonical))
        assertFailsWith<InvalidEmbeddingException> {
            canonical.cosineSimilarity(byteArrayOf(1, 2, 3, 4).toByteString())
        }
    }
}
