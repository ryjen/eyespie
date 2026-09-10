package com.micrantha.eyespie.imaging

import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticResult
import com.micrantha.eyespie.telemetry.FakeDiagnosticSink
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class TelemetryImageCaptureTest {
    @Test
    fun successfulCaptureEmitsBoundedCameraRecord() = runTest {
        val sink = FakeDiagnosticSink()
        val expected = CapturedImage.fromEncoded(byteArrayOf(1))
        val capture = TelemetryImageCapture(
            delegate = TestImageCapture { expected },
            telemetry = OperationalTelemetry(sink),
        )

        val actual = capture.capture()

        assertEquals(expected, actual)
        val record = sink.records.single()
        assertEquals(DiagnosticOperation.CAMERA_CAPTURE, record.operation)
        assertEquals(DiagnosticResult.SUCCESS, record.result)
        assertEquals(null, record.code)
    }

    @Test
    fun captureFailureUsesStableCameraCodeAndPropagates() = runTest {
        val sink = FakeDiagnosticSink()
        val capture = TelemetryImageCapture(
            delegate = TestImageCapture { throw IllegalStateException("private camera detail") },
            telemetry = OperationalTelemetry(sink),
        )

        assertFailsWith<IllegalStateException> { capture.capture() }

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.CAMERA_CAPTURE, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.CAMERA_CAPTURE_FAILED, record.code)
    }

    @Test
    fun cancellationIsRecordedAndStillPropagates() = runTest {
        val sink = FakeDiagnosticSink()
        val capture = TelemetryImageCapture(
            delegate = TestImageCapture { throw CancellationException("cancelled") },
            telemetry = OperationalTelemetry(sink),
        )

        assertFailsWith<CancellationException> { capture.capture() }

        val record = sink.records.single()
        assertEquals(DiagnosticResult.CANCELLED, record.result)
        assertEquals(null, record.code)
    }
}

private class TestImageCapture(
    private val block: suspend () -> CapturedImage,
) : ImageCapture {
    override suspend fun capture(): CapturedImage = block()
}
