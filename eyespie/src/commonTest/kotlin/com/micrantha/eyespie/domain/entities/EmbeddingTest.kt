package com.micrantha.eyespie.domain.entities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EmbeddingTest {
    private val metadata = EmbeddingMetadata(
        modelId = "test:model:v1",
        dimensions = 4,
        normalization = EmbeddingNormalization.ModelDefined,
        similarity = EmbeddingSimilarity.Cosine,
    )

    @Test
    fun binaryRoundTripPreservesFloatBitsAndMetadata() {
        val embedding = Embedding.of(metadata, listOf(-1f, 0f, 0.5f, 1f))

        val restored = Embedding.fromByteArray(metadata, embedding.toByteArray())

        assertEquals(embedding, restored)
        assertEquals(16, embedding.toByteArray().size)
    }

    @Test
    fun pgVectorRoundTripPreservesValues() {
        val embedding = Embedding.of(metadata, listOf(-1f, 0f, 0.5f, 1f))

        val restored = Embedding.fromPgVector(metadata, embedding.toPgVector())

        assertEquals(embedding, restored)
    }

    @Test
    fun rejectsWrongDimensionAndNonFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            Embedding.of(metadata, listOf(1f))
        }
        assertFailsWith<IllegalArgumentException> {
            Embedding.of(metadata, listOf(0f, Float.NaN, 0f, 0f))
        }
        assertFailsWith<IllegalArgumentException> {
            Embedding.fromByteArray(metadata, ByteArray(4))
        }
    }

    @Test
    fun metadataRoundTripIsStable() {
        assertEquals(metadata, EmbeddingMetadata.parse(metadata.persistenceId))
    }

    @Test
    fun incompatibleModelsCannotBeCompared() {
        val first = Embedding.of(metadata, listOf(1f, 0f, 0f, 0f))
        val second = Embedding.of(
            metadata.copy(modelId = "test:model:v2"),
            listOf(1f, 0f, 0f, 0f),
        )

        assertFailsWith<IllegalArgumentException> {
            first.cosineSimilarity(second)
        }
    }

    @Test
    fun cosineSimilarityUsesValidatedComponents() {
        val first = Embedding.of(metadata, listOf(1f, 0f, 0f, 0f))
        val same = Embedding.of(metadata, listOf(1f, 0f, 0f, 0f))
        val orthogonal = Embedding.of(metadata, listOf(0f, 1f, 0f, 0f))

        assertTrue(first.cosineSimilarity(same) > 0.999f)
        assertEquals(0f, first.cosineSimilarity(orthogonal))
    }
}
