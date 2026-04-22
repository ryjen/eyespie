package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.observability.debug
import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import okio.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ClueDataRepository(
    private val llm: GenAI,
    private val cluePromptSource: CluePromptSource,
    private val timeout: Duration = 1.minutes
) : ClueRepository {
    private val log by logger()
    private val images = mutableSetOf<String>()

    private fun imageParam(image: Path) = if (images.contains(image.toString()))
        emptyList() // already added
    else
        images.apply { add("file://$image") }.toList()

    override suspend fun guess(image: Path, clue: GuessClue): Result<String> =
        withTimeout(timeout) {
            llm.generate(
                GenAIRequest(
                    prompt = cluePromptSource.guess(clue.data),
                    images = imageParam(image)
                )
            ).onSuccess {
                log.debug(it)
            }.onFailure {
                log.error(it) { "unable to infer" }
            }
        }

    override suspend fun clues(image: Path): Result<AiProof> =
        withTimeout(timeout) {
            llm.generate(
                GenAIRequest(
                    prompt = cluePromptSource.clues(),
                    images = imageParam(image)
                )
            ).onSuccess(log::debug).onFailure {
                log.error(it) { "unable to infer" }
            }.map(::toProof)
        }


     fun infer(image: Path): Flow<AiProof> {
        return llm.generateFlow(
            GenAIRequest(
                prompt = cluePromptSource.clues(),
                images = imageParam(image)
            )
        ).onEach(log::debug).catch {
            log.error(it) {"unable to infer" }
        }.map(::toProof)
    }

    private fun toProof(output: String) = output.lines().chunked(3).map { (clue, answer, confidence) ->
        AiClue(
            clue, confidence.toFloat(), answer
        )
    }.toSet()
}
