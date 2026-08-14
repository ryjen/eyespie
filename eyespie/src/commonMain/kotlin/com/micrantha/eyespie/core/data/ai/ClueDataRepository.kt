package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.ai.SemanticImageInput
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import okio.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class ClueDataRepository(
    private val inferenceProvider: SemanticInferenceProvider,
    private val cluePromptSource: CluePromptSource,
    private val timeout: Duration = 1.minutes,
) : ClueRepository {

    override suspend fun guess(image: Path, clue: GuessClue): Result<String> =
        withTimeout(timeout) {
            inferenceProvider.generate(
                request(
                    prompt = cluePromptSource.guess(clue.data),
                    image = image,
                )
            )
        }

    override suspend fun clues(image: Path): Result<AiProof> =
        withTimeout(timeout) {
            inferenceProvider.generate(
                request(
                    prompt = cluePromptSource.clues(),
                    image = image,
                )
            ).map(::toProof)
        }

    fun infer(image: Path): Flow<AiProof> =
        inferenceProvider.generateFlow(
            request(
                prompt = cluePromptSource.clues(),
                image = image,
            )
        ).map(::toProof)

    private fun request(prompt: String, image: Path): SemanticInferenceRequest {
        require(image.isAbsolute) { "image path must be absolute" }
        return SemanticInferenceRequest(
            prompt = prompt,
            images = listOf(SemanticImageInput(image)),
        )
    }

    private fun toProof(output: String) =
        output.lines().chunked(3).map { (clue, answer, confidence) ->
            AiClue(
                clue,
                confidence.toFloat(),
                answer,
            )
        }.toSet()
}
