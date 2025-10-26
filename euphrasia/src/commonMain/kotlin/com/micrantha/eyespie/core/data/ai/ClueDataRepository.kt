package com.micrantha.eyespie.core.data.ai

import androidx.compose.ui.graphics.ColorProducer
import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.app.d
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
import kotlinx.serialization.json.Json
import okio.Path

class ClueDataRepository(
    private val llm: GenAI,
    private val cluePromptSource: CluePromptSource,
) : ClueRepository {

    private val images = mutableSetOf<String>()

    private fun imageParam(image: Path) = if (images.contains(image.toString()))
        emptyList() // already added
    else
        images.apply { add("file://$image") }.toList()

    override fun guess(image: Path, clue: GuessClue): Result<String> {
        return llm.generate(
            GenAIRequest(
                prompt = cluePromptSource.guess(clue.data),
                images = imageParam(image)
            )
        ).onSuccess {
            Log.d(tag = "Clues", message = it)
        }.onFailure {
            Log.e(tag = "Clues", throwable = it, messageString = "unable to infer")
        }
    }

    override fun clues(image: Path): Result<AiProof> {
        return llm.generate(
            GenAIRequest(
                prompt = cluePromptSource.clues(),
                images = imageParam(image)
            )
        ).onSuccess {
            Log.d(tag = "Clues", message = it)
        }.onFailure {
            Log.e(tag = "Clues", throwable = it, messageString = "unable to infer")
        }.map(::toProof)
    }


     fun infer(image: Path): Flow<AiProof> {
        return llm.generateFlow(
            GenAIRequest(
                prompt = cluePromptSource.clues(),
                images = imageParam(image)
            )
        ).onEach {
            Log.d(tag = "Clues", message = it)
        }.catch {
            Log.e(tag = "Clues", throwable = it, messageString = "unable to infer")
        }.map(::toProof)
    }

    private fun toProof(output: String) = output.lines().chunked(3).map { (clue, answer, confidence) ->
        AiClue(
            clue, confidence.toFloat(), answer
        )
    }.toSet()
}
