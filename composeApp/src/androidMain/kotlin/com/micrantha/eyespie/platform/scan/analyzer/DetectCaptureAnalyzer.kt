package com.micrantha.eyespie.platform.scan.analyzer

import android.content.Context
import com.micrantha.bluebell.app.Log
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer

actual class DetectCaptureAnalyzer(
    context: Context,
) : CaptureAnalyzer<DetectProof> {

    actual override suspend fun analyze(image: CameraImage): Result<DetectProof> = try {

        Result.failure(NotImplementedError())
    } catch (err: Throwable) {
        Log.e("analyzer", err) { "unable to detect image" }
        Result.failure(err)
    }

    companion object {

        const val defaultPrompt = """
            Examine the image and detect the objects within it to a maximum of 5.
            Provide output as JSON with the following format: [{name: string, confidence: number}]
        """

    }
}
