package com.micrantha.eyespie.core.data.ai

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
import kotlinx.coroutines.withTimeout
import okio.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class ClueDataRepository(
    private val llm: GenAI,
    private val cluePromptSource: CluePromptSource,
    private val timeout: Duration = 1.minutes
) : ClueRepository {

    private fun imageParam(image: Path): List<String> {
        require(image.isAbsolute) { "image path must be absolute" }
        return listOf(image.asFileUri())
    }

    override suspend fun guess(image: Path, clue: GuessClue): Result<String> =
        withTimeout(timeout) {
            llm.generate(
                GenAIRequest(
                    prompt = cluePromptSource.guess(clue.data),
                    images = imageParam(image)
                )
            )
        }

    override suspend fun clues(image: Path): Result<AiProof> =
        withTimeout(timeout) {
            llm.generate(
                GenAIRequest(
                    prompt = cluePromptSource.clues(),
                    images = imageParam(image)
                )
            ).map(::toProof)
        }

    fun infer(image: Path): Flow<AiProof> =
        llm.generateFlow(
            GenAIRequest(
                prompt = cluePromptSource.clues(),
                images = imageParam(image)
            )
        ).catch {
            // Preserve the previous graceful-flow failure behavior without logging
            // prompts, generated content, image paths, or provider payloads.
        }.map(::toProof)

    private fun toProof(output: String) =
        output.lines().chunked(3).map { (clue, answer, confidence) ->
            AiClue(
                clue, confidence.toFloat(), answer
            )
        }.toSet()

    private fun Path.asFileUri(): String = "file://" + toString().encodePathForUri()

    private fun String.encodePathForUri(): String {
        val digits = "0123456789ABCDEF"
        return buildString {
            encodeToByteArray().forEach { byte ->
                val value = byte.toInt() and 255
                val safe = value in 48..57 || value in 65..90 || value in 97..122 ||
                    value == 45 || value == 46 || value == 95 || value == 126 || value == 47
                if (safe) {
                    append(value.toChar())
                } else {
                    append('%')
                    append(digits[value ushr 4])
                    append(digits[value and 15])
                }
            }
        }
    }
}
