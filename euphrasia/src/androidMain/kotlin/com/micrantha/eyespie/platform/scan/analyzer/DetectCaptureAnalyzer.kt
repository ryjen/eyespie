package com.micrantha.eyespie.platform.scan.analyzer

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Detection
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode.IMAGE
import com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector.ObjectDetectorOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.micrantha.bluebell.app.Log
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.domain.entities.LabelClue
import com.micrantha.eyespie.platform.scan.CameraAnalyzerConfig
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.baseOptions
import com.micrantha.eyespie.platform.scan.components.AnalyzerCallback
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer
import com.micrantha.eyespie.platform.scan.components.StreamAnalyzer

//private const val MODEL_ASSET = "detection_efficientnet.tflite"
private const val MODEL_ASSET = "detection_efficientnet_lite.tflite"

typealias DetectionAnalyzerConfig = CameraAnalyzerConfig<DetectProof, ObjectDetectorOptions.Builder, ObjectDetector, ObjectDetectorResult>

actual class DetectCaptureAnalyzer(
    context: Context,
) : DetectAnalyzer(context), CaptureAnalyzer<DetectProof> {

    private val client by lazy {
        super.client {
            setRunningMode(IMAGE)
                .setMaxResults(5)
        }
    }

    actual override suspend fun analyze(image: CameraImage): Result<DetectProof> = try {
        val result = client.detect(
            image.resize(320, 320).asMPImage(),
            image.processingOptions
        )
        Result.success(super.map(result))
    } catch (err: Throwable) {
        Log.e("analyzer", err) { "unable to detect image" }
        Result.failure(err)
    }

}

class DetectStreamAnalyzer(
    context: Context,
    private val callback: AnalyzerCallback<DetectProof>,
) : DetectAnalyzer(context), StreamAnalyzer {

    private val client by lazy {
        super.client {
            setRunningMode(LIVE_STREAM)
            setResultListener(::onResult)
            setErrorListener(callback::onAnalyzerError)
        }
    }

    override fun analyze(image: CameraImage) {
        client.detectAsync(
            image.asMPImage(), image.processingOptions, image.timestamp
        )
    }

    private fun onResult(result: ObjectDetectorResult, input: MPImage) {
        callback.onAnalyzerResult(super.map(result))
    }
}

abstract class DetectAnalyzer(private val context: Context) : DetectionAnalyzerConfig {
    override fun map(result: ObjectDetectorResult): DetectProof {
        return result.detections().flatMap(::detect).toSet()
    }

    private fun detect(obj: Detection) =
        obj.categories().map { LabelClue(it.categoryName(), it.score()) }.toSet()

    override fun client(block: ObjectDetectorOptions.Builder.() -> Unit): ObjectDetector {
        val options = ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions(MODEL_ASSET) {
                setDelegate(Delegate.GPU)
            })
            .setScoreThreshold(0.6f)
            .apply(block)
            .build()
        return ObjectDetector.createFromOptions(context, options)
    }
}
