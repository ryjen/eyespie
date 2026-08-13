package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.platform.scan.CameraImage

/** Generates a canonical image embedding for match lookup. */
interface ImageEmbeddingGenerator {
    suspend fun generate(image: CameraImage): Embedding
}

/**
 * Deterministic test double only.
 *
 * The output deliberately has no semantic image meaning, but it conforms to the same 1024-float
 * representation as production providers so common tests and failure paths exercise the real data
 * contract. Production iOS composition must replace this implementation in #91's native slice.
 */
class DeterministicImageEmbeddingGenerator : ImageEmbeddingGenerator {
    override suspend fun generate(image: CameraImage): Embedding {
        val bytes = image.toByteArray()
        require(bytes.isNotEmpty()) { "camera image produced no bytes" }

        val buckets = IntArray(ImageEmbeddingContract.dimensions)
        bytes.forEachIndexed { index, value ->
            val bucket = index % ImageEmbeddingContract.dimensions
            buckets[bucket] = buckets[bucket] xor (value.toInt() and 0xFF)
        }

        return buckets.map { bucket ->
            (bucket / 255f) * 2f - 1f
        }.toCanonicalEmbedding()
    }
}
