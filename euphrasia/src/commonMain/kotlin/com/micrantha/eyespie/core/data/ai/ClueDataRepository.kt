package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.entities.Clues
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.domain.repository.LocationRepository
import com.micrantha.eyespie.features.scan.data.analyzer.ColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.DetectCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.LabelCaptureAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.Path

class ClueDataRepository(
    private val llm: GenAI,
    private val cluePromptSource: CluePromptSource,
    private val locationRepository: LocationRepository,
    private val colorCaptureAnalyzer: ColorCaptureAnalyzer,
    private val labelCaptureAnalyzer: LabelCaptureAnalyzer,
    private val detectCaptureAnalyzer: DetectCaptureAnalyzer
) : ClueRepository {

    override fun generate(image: Path): Result<Clues> {
        val prompt = cluePromptSource.cluesPrompt()
        return llm.generate(
            GenAIRequest(
                prompt = prompt.prompt,
                images = listOf(image.toString())
            )
        ).map {
            Json.decodeFromString(it)
        }
    }

    override fun infer(image: Path): Flow<Clues> {
        val prompt = cluePromptSource.cluesPrompt()
        return llm.generateFlow(
            GenAIRequest(
                prompt = prompt.prompt,
                images = listOf(image.toString())
            )
        ).map { Json.decodeFromString(it) }
    }
}
