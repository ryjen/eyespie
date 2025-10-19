package com.micrantha.eyespie.platform.scan.analyzer

import com.micrantha.eyespie.features.scan.data.analyzer.DominantColor
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.components.CaptureAnalyzer

expect class DominantColorCaptureAnalyzer : CaptureAnalyzer<DominantColor> {
    override suspend fun analyze(image: CameraImage): Result<DominantColor>
}
