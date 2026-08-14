package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.repository.ThingRepository
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

data class MatchResult(
    val matched: Boolean,
    val bestSimilarity: Float? = null
)

class MatchCaptureUseCase(
    private val imageEmbeddingGenerator: ImageEmbeddingGenerator,
    private val thingRepository: ThingRepository,
) {
    suspend operator fun invoke(
        image: CameraImage,
        thing: Thing,
    ): Flow<Result<MatchResult>> = flow {
        val embedding = try {
            imageEmbeddingGenerator.generate(image).also {
                require(it != Embedding.EMPTY) { "capture embedding must not be empty" }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            emit(Result.failure(error))
            return@flow
        }

        emitAll(
            thingRepository.match(embedding).map { res ->
                res.map { matches ->
                    val matched = matches.any { it.id == thing.id }
                    val bestSimilarity = matches.find { it.id == thing.id }?.similarity
                        ?: matches.maxByOrNull { it.similarity }?.similarity

                    MatchResult(matched, bestSimilarity)
                }
            }
        )
    }
}
