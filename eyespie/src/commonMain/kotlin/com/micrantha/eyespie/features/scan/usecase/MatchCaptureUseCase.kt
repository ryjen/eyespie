package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlin.coroutines.coroutineContext

class MatchCaptureUseCase(
    private val imageEmbeddingGenerator: ImageEmbeddingGenerator,
) {
    suspend operator fun invoke(
        image: CameraImage,
        thing: Thing,
    ): Result<Boolean> =
        dispatchUseCase(coroutineContext) {
            val embedding = imageEmbeddingGenerator.generate(image)
            require(embedding != Embedding.EMPTY) { "capture embedding must not be empty" }

            // Semantic matching is intentionally not routed to ThingRepository.match yet.
            // The current Supabase migrations do not define a compatible match_things RPC
            // and the checked-in schema uses 1536-dimensional vectors. Keep the provider
            // seam here until the model dimensions, RPC, and threshold policy are landed.
            throw UnsupportedOperationException("semantic capture matching is not available")
        }
}
