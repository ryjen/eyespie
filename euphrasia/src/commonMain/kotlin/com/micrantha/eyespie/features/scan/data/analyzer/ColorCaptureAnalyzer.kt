package com.micrantha.eyespie.features.scan.data.analyzer

import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.platform.Platform
import com.micrantha.eyespie.domain.entities.ColorClue
import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.analyzer.DominantColorCaptureAnalyzer
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer
import okio.BufferedSource
import okio.Path.Companion.toPath
import okio.use
import kotlin.math.sqrt

private const val MODEL_ASSET = "colors.csv"

typealias DominantColor = Array<Int>

class ColorCaptureAnalyzer(
    private val platform: Platform,
    private val dominantColorCaptureAnalyzer: DominantColorCaptureAnalyzer
) : CaptureAnalyzer<ColorProof> {

    override suspend fun analyze(image: CameraImage): Result<ColorProof> = try {
        val c1 = dominantColorCaptureAnalyzer.analyze(image).getOrThrow()
        val colors = candidateColors(c1)
        Result.success(colors)
    } catch (err: Throwable) {
        Log.e("analyzer", err) { "unable to get image color" }
        Result.failure(err)
    }

    private val colorNames by lazy { readColorNames() }

    private fun candidateColors(color: Array<Int>): ColorProof {
        val colors = colorNames.map {
            it.key to colorDistance(color, it.value)
        }.sortedBy { it.second }.take(1).map { (key, value) ->
            ColorClue(
                key,
                value.toFloat(),
            )
        }.toSet()

        return colors
    }

    private fun colorDistance(c1: Array<Int>, c2: Array<Int>): Double {
        var sum = 0
        for (i in c1.indices) {
            val diff = c1[i] - c2[i]
            sum += (diff * diff)
        }
        return sqrt(sum.toDouble())
    }

    private fun readColorNames() =
        platform.resource(MODEL_ASSET.toPath()).use { reader ->
            reader.readUtf8Line()
            reader.lineSequence()
                .filter { it.isNotBlank() }
                .associate {
                    val (name, _, r, g, b) = it.split(',')
                    val c2 = arrayOf(r.toInt(), g.toInt(), b.toInt())
                    name to c2
                }
        }

    private fun BufferedSource.lineSequence(): Sequence<String> = sequence {
        while (true) {
            val line = readUtf8Line() ?: break
            yield(line)
        }
    }
}
