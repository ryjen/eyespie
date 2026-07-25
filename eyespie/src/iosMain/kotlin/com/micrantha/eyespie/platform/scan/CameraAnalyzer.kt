package com.micrantha.eyespie.platform.scan

import com.micrantha.eyespie.platform.scan.components.AnalyzerCallback
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer
import com.micrantha.eyespie.platform.scan.components.StreamAnalyzer
import platform.Vision.VNObservation
import platform.Vision.VNRequest

interface CameraAnalyzerConfig<out T, out R : VNRequest, O : VNObservation> {
    fun request(): R
    fun map(response: List<O>, image: CameraImage): T

    val filter: (List<*>?) -> List<O>
}

abstract class CameraCaptureAnalyzer<out T, R : VNRequest, O : VNObservation>(
    private val config: CameraAnalyzerConfig<T, R, O>,
) : CaptureAnalyzer<T>, CameraAnalyzerConfig<T, R, O> by config {
    override suspend fun analyze(image: CameraImage): Result<T> =
        Result.failure(UnsupportedOperationException("iOS camera analysis is not available"))
}

abstract class CameraStreamAnalyzer<out T, out R : VNRequest, O : VNObservation>(
    private val config: CameraAnalyzerConfig<T, R, O>,
    private val callback: AnalyzerCallback<T>,
) : CameraAnalyzerConfig<T, R, O> by config, StreamAnalyzer {
    override fun analyze(image: CameraImage) {
        callback.onAnalyzerError(UnsupportedOperationException("iOS camera analysis is not available"))
    }
}
