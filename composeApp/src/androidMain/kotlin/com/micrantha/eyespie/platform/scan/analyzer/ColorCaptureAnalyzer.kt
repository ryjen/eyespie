package com.micrantha.eyespie.platform.scan.analyzer

import android.content.Context
import com.micrantha.bluebell.app.Log
import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer

actual class ColorCaptureAnalyzer(
    context: Context,
) : CaptureAnalyzer<ColorProof> {

    actual override suspend fun analyze(image: CameraImage): Result<ColorProof> =
        try {
            Result.failure(NotImplementedError())
        } catch (err: Throwable) {
            Log.e("analyzer", err) { "unable to get image color" }
            Result.failure(err)
        }

    companion object {

        const val defaultPrompt = """
    Examine the image and determine the dominant colors with a maximum of 5.
    Provide output as JSON with the following format: [{name: string, confidence: number}]
    Confidence should take into account:
    - the total percentage
    - the variation of hue within the color
    - the brightness of the color compared to others
"""
    }
}
