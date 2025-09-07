package com.micrantha.eyespie.platform.scan.analyzer

import android.content.Context
import com.micrantha.bluebell.app.Log
import com.micrantha.eyespie.domain.entities.LabelProof
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer

actual class LabelCaptureAnalyzer(
    context: Context,
) : CaptureAnalyzer<LabelProof> {

    actual override suspend fun analyze(image: CameraImage): Result<LabelProof> = try {
        Result.failure(NotImplementedError())
    } catch (err: Throwable) {
        Log.e("analyzer", err) { "unable to label image" }
        Result.failure(err)
    }

    companion object {
        const val defaultPrompt = """
            Examine the image and classify it with labels to a maximum of 5.
            Provide output as JSON with the following format: 
            [{name: string, confidence: number}]
        """
    }
}
