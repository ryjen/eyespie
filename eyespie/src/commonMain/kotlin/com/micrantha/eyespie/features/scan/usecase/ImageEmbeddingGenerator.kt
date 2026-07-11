package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.ByteString.Companion.toByteString

/**
 * Generates an image embedding for match lookup.
 *
 * This is intentionally small and deterministic so the scan pipeline no longer
 * depends on `Embedding.EMPTY`. Model-backed providers can replace this behind
 * the same boundary without giving the LLM authority over match decisions.
 */
interface ImageEmbeddingGenerator {
    suspend fun generate(image: CameraImage): Embedding
}

class DeterministicImageEmbeddingGenerator : ImageEmbeddingGenerator {
    override suspend fun generate(image: CameraImage): Embedding {
        val bytes = image.toByteArray()
        require(bytes.isNotEmpty()) { "camera image produced no bytes" }

        val buckets = ByteArray(EMBEDDING_DIMENSIONS)
        bytes.forEachIndexed { index, value ->
            val bucket = index % EMBEDDING_DIMENSIONS
            buckets[bucket] = (buckets[bucket].toInt() xor value.toInt()).toByte()
        }

        return buckets.toByteString()
    }

    private companion object {
        const val EMBEDDING_DIMENSIONS = 1024
    }
}
