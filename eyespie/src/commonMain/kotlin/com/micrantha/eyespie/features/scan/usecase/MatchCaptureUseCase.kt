package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.repository.ThingRepository
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
        thingID: String,
    ): Flow<Result<MatchResult>> {
        require(thingID.isNotBlank()) { "target Thing id must not be blank" }

        val embedding = try {
            imageEmbeddingGenerator.generate(image).also {
                require(it != Embedding.EMPTY) { "capture embedding must not be empty" }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return flowOf(Result.failure(error))
        }

        return thingRepository.match(thingID, embedding).map { res ->
            res.map { match ->
                MatchResult(
                    matched = match.matched,
                    bestSimilarity = match.similarity,
                )
            }
        }
    }
}
