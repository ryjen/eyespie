package com.micrantha.eyespie.imaging

import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.OperationalTelemetry

/**
 * Adds privacy-bounded operational timing/result telemetry around a platform
 * capture without changing capture ownership, lifecycle, or image contents.
 */
internal class TelemetryImageCapture(
    private val delegate: ImageCapture,
    private val telemetry: OperationalTelemetry,
) : ImageCapture {
    override suspend fun capture(): CapturedImage = telemetry.observe(
        DiagnosticOperation.CAMERA_CAPTURE,
    ) {
        delegate.capture()
    }
}
