package com.micrantha.eyespie.platform.scan.components

import com.micrantha.eyespie.platform.scan.CameraImage

fun interface CameraScannerDispatch {
    suspend operator fun invoke(image: CameraImage)
}

interface CaptureAnalyzer<out T> {
    suspend fun analyze(image: CameraImage): Result<T>
}

interface StreamAnalyzer {
    fun analyze(image: CameraImage)
}

interface AnalyzerCallback<in T> {
    fun onAnalyzerResult(result: T)
    fun onAnalyzerError(error: Throwable)
}
