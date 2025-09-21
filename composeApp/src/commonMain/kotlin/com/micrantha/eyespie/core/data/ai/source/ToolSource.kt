package com.micrantha.eyespie.core.data.ai.source

import com.micrantha.eyespie.core.data.ai.model.AiTool
import com.micrantha.eyespie.domain.entities.DataClue
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.features.scan.data.analyzer.ColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.DetectCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.analyzer.LabelCaptureAnalyzer

class ToolSource(
    private val colorAnalyzer: ColorCaptureAnalyzer,
    private val detectAnalyzer: DetectCaptureAnalyzer,
    private val labelAnalyzer: LabelCaptureAnalyzer,
) {
    suspend fun execute(id: String, image: CameraImage): Result<Pair<String, Set<DataClue>>> = when (id) {
        "color" -> colorAnalyzer.analyze(image).map { id to it }
        "detect" -> detectAnalyzer.analyze(image).map { id to it }
        "label" -> labelAnalyzer.analyze(image).map { id to it }
        else -> Result.failure(IllegalArgumentException("Unknown tool: $id"))
    }

    fun tools() = mapOf(
        "analyzeColor" to AiTool(
            id = "color",
            description = "find colors in an image",
            function = { it: CameraImage ->
                colorAnalyzer.analyze(it)
            }
        ),
        "detectObjects" to AiTool(
            id = "detect",
            description = "detects objects in an image",
            function = {
                detectAnalyzer.analyze(it)
            }
        ),
        "classify" to AiTool(
            id = "label",
            description = "classify an image contents",
            function = {
                labelAnalyzer.analyze(it)
            }
        )
    )
}
