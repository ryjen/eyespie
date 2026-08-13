package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.ALPHA_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.EmbeddingMetadata
import com.micrantha.eyespie.domain.entities.EmbeddingNormalization
import com.micrantha.eyespie.domain.entities.EmbeddingSimilarity
import com.micrantha.eyespie.platform.scan.CameraImage

/** Generates a validated image embedding for match lookup. */
interface ImageEmbeddingGenerator {
    suspend fun generate(image: CameraImage): Embedding
}

/**
 * Deterministic test/fallback generator.
 *
 * Its model identity is intentionally distinct from MediaPipe. Once model-aware matching is in
 * place, these vectors cannot be compared with production MediaPipe vectors by accident.
 */
class DeterministicImageEmbeddingGenerator : ImageEmbeddingGenerator {
    override suspend fun generate(image: CameraImage): Embedding {
        val bytes = image.toByteArray()
        require(bytes.isNotEmpty()) { "camera image produced no bytes" }

        val buckets = IntArray(ALPHA_EMBEDDING_DIMENSIONS)
        bytes.forEachIndexed { index, value ->
            val bucket = index % ALPHA_EMBEDDING_DIMENSIONS
            buckets[bucket] = buckets[bucket] xor (value.toInt() and 0xFF)
        }

        val values = buckets.map { bucket ->
            (bucket - 127.5f) / 127.5f
        }
        return Embedding.of(METADATA, values)
    }

    companion object {
        val METADATA = EmbeddingMetadata(
            modelId = "test:deterministic-image-bytes:v1",
            dimensions = ALPHA_EMBEDDING_DIMENSIONS,
            normalization = EmbeddingNormalization.ModelDefined,
            similarity = EmbeddingSimilarity.Cosine,
        )
    }
}
