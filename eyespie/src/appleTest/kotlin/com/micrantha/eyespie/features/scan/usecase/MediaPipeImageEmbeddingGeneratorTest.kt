package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.InvalidEmbeddingException
import com.micrantha.eyespie.domain.entities.floats
import org.kodein.di.DI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MediaPipeImageEmbeddingGeneratorTest {
    @Test
    fun productionCompositionUsesMediaPipeGenerator() {
        assertIs<MediaPipeImageEmbeddingGenerator>(platformImageEmbeddingGenerator(DI {}))
    }

    @Test
    fun acceptsExactlyOneCanonicalFloatHead() {
        val values = List(ImageEmbeddingContract.dimensions) { index -> index.toFloat() / 1024f }

        val embedding = canonicalMediaPipeEmbedding(listOf(values))

        assertEquals(ImageEmbeddingContract.encodedBytes, embedding.size)
        assertEquals(values, embedding.floats())
    }

    @Test
    fun rejectsMissingOrMultipleHeads() {
        assertFailsWith<IllegalStateException> {
            canonicalMediaPipeEmbedding(emptyList())
        }
        assertFailsWith<IllegalStateException> {
            canonicalMediaPipeEmbedding(
                listOf(
                    List(ImageEmbeddingContract.dimensions) { 0f },
                    List(ImageEmbeddingContract.dimensions) { 1f },
                )
            )
        }
    }

    @Test
    fun rejectsMissingOrQuantizedFloatOutput() {
        assertFailsWith<IllegalStateException> {
            canonicalMediaPipeEmbedding(listOf(null))
        }
        assertFailsWith<IllegalStateException> {
            canonicalMediaPipeEmbedding(listOf(emptyList()))
        }
    }

    @Test
    fun rejectsWrongDimensionAndNonFiniteValues() {
        assertFailsWith<InvalidEmbeddingException> {
            canonicalMediaPipeEmbedding(listOf(List(ImageEmbeddingContract.dimensions - 1) { 0f }))
        }
        assertFailsWith<InvalidEmbeddingException> {
            canonicalMediaPipeEmbedding(
                listOf(List(ImageEmbeddingContract.dimensions) { index ->
                    if (index == 0) Float.NaN else 0f
                })
            )
        }
        assertFailsWith<InvalidEmbeddingException> {
            canonicalMediaPipeEmbedding(
                listOf(List(ImageEmbeddingContract.dimensions) { index ->
                    if (index == 0) Float.POSITIVE_INFINITY else 0f
                })
            )
        }
    }
}
