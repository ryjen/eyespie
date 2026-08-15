package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticImageInput
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceExecutionSnapshot
import com.micrantha.eyespie.domain.ai.SemanticInferenceOutput
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class ClueDataRepository(
    private val inferenceProvider: SemanticInferenceProvider,
    private val cluePromptSource: CluePromptSource,
    private val timeout: Duration = 1.minutes,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        allowSpecialFloatingPointValues = false
    },
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

    override suspend fun clues(image: Path): Result<GeneratedClues> =
        withTimeout(timeout) {
            val generated = inferenceProvider.generateWithExecution(
                request(
                    prompt = cluePromptSource.clues(),
                    image = image,
                )
            )
            val output = generated.getOrElse { return@withTimeout Result.failure(it) }

            val firstAttempt = parseEnvelope(output, repaired = false)
            if (firstAttempt.isSuccess || !canRepairLocally(output.execution)) {
                return@withTimeout firstAttempt
            }

            val repair = inferenceProvider.generateWithExecution(
                SemanticInferenceRequest(
                    prompt = cluePromptSource.repair(output.text.take(MAX_REPAIR_INPUT_LENGTH)),
                    images = emptyList(),
                )
            )
            repair.fold(
                onSuccess = {
                    if (it.execution != output.execution) {
                        Result.failure(GeneratedClueRepairExecutionChangedException())
                    } else {
                        parseEnvelope(it, repaired = true)
                    }
                },
                onFailure = { Result.failure(it) },
            )
        }

    private fun request(prompt: String, image: Path): SemanticInferenceRequest {
        require(image.isAbsolute) { "image path must be absolute" }
        return SemanticInferenceRequest(
            prompt = prompt,
            images = listOf(SemanticImageInput(image)),
        )
    }

    private fun parseEnvelope(
        output: SemanticInferenceOutput,
        repaired: Boolean,
    ): Result<GeneratedClues> = try {
        val response = json.decodeFromString<GeneratedClueResponse>(output.text)
        Result.success(
            response.validateAndMap(
                identity = output.execution.identity,
                executionConfiguration = output.execution.configuration,
                promptId = cluePromptSource.cluePromptId,
                promptVersion = cluePromptSource.cluePromptVersion,
                repaired = repaired,
            )
        )
    } catch (error: GeneratedClueResponseException) {
        Result.failure(error)
    } catch (_: SerializationException) {
        Result.failure(MalformedGeneratedClueResponseException())
    } catch (_: IllegalArgumentException) {
        Result.failure(MalformedGeneratedClueResponseException())
    }

    private fun canRepairLocally(initialExecution: SemanticInferenceExecutionSnapshot): Boolean {
        val availability = inferenceProvider.availability.value as? SemanticInferenceAvailability.Available
            ?: return false
        return initialExecution.identity.locality == InferenceLocality.LOCAL &&
            inferenceProvider.identity == initialExecution.identity &&
            inferenceProvider.executionConfiguration == initialExecution.configuration &&
            availability.capabilities.textGeneration
    }

    private companion object {
        const val MAX_REPAIR_INPUT_LENGTH = 4096
    }
}
