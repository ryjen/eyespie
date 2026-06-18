package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.repository.ThingRepository
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlin.coroutines.coroutineContext

class MatchCaptureUseCase(
    private val thingRepository: ThingRepository,
    private val imageEmbeddingGenerator: ImageEmbeddingGenerator,
) {
    suspend operator fun invoke(
        image: CameraImage,
        thing: Thing,
    ): Result<Boolean> =
        dispatchUseCase(coroutineContext) {
            val embedding = imageEmbeddingGenerator.generate(image)
            require(embedding.size > 0) { "capture embedding must not be empty" }

            thingRepository.match(embedding).getOrThrow()
                .any { match ->
                    match.id == thing.id && match.similarity >= MATCH_THRESHOLD
                }
        }

    private companion object {
        const val MATCH_THRESHOLD = 0.70f
    }
}
