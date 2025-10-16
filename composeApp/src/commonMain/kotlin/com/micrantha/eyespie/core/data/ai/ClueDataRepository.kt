package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.AgentLocalSource
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.core.data.ai.source.ToolSource
import com.micrantha.eyespie.domain.entities.Clues
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.scan.data.analyzer.ColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.analyzer.DetectCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.LabelCaptureAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class ClueDataRepository(
    private val agentLocalSource: AgentLocalSource,
    private val cluePromptSource: CluePromptSource,
    private val toolSource: ToolSource,
    private val colorCaptureAnalyzer: ColorCaptureAnalyzer,
    private val labelCaptureAnalyzer: LabelCaptureAnalyzer,
    private val detectCaptureAnalyzer: DetectCaptureAnalyzer
) : ClueRepository {

     suspend fun generateLegacy(image: CameraImage): Result<Clues> = try {
        withContext(Dispatchers.Default) {
            val colors = async {
                colorCaptureAnalyzer.analyze(image).getOrThrow()
            }
            val labels = async {
                labelCaptureAnalyzer.analyze(image).getOrThrow()
            }
            val objects = async {
                detectCaptureAnalyzer.analyze(image).getOrThrow()
            }
            Result.success(
                Clues(
                    colors = colors.await(),
                    labels = labels.await(),
                    detections = objects.await()
                )
            )
        }
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun generate(image: CameraImage): Result<Clues> = try {
        val completion = agentLocalSource.generate(
            listOf(
                cluePromptSource.cluesPrompt(),
            ),
            toolSource.tools()
        ).getOrThrow()

        if (completion.toolCalls == null) {
            throw IllegalStateException("No tool calls")
        }

        val toolResults = completion.toolCalls.associate { result ->
            toolSource.execute(result.id, image).getOrThrow()
        }

        val clues = Clues(
            colors = toolResults["color"],
            detections = toolResults["detected"],
            labels = toolResults["label"],
        )

        Result.success(clues)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun embedding(image: CameraImage): Result<Embedding> {
        return Result.success(Embedding.EMPTY)
    }
}
