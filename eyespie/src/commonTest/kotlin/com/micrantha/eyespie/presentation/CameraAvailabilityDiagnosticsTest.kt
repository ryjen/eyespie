package com.micrantha.eyespie.presentation

import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticResult
import com.micrantha.eyespie.telemetry.DiagnosticSink
import com.micrantha.eyespie.telemetry.FakeDiagnosticSink
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraAvailabilityDiagnosticsTest {
    @Test
    fun requestableIsSilentAndDuplicateStatesAreSuppressed() {
        val sink = FakeDiagnosticSink()
        val observer = CameraAvailabilityDiagnosticObserver(OperationalTelemetry(sink))

        observer.onAvailabilityChanged(CameraAvailability.Requestable)
        observer.onAvailabilityChanged(CameraAvailability.Requestable)
        observer.onAvailabilityChanged(CameraAvailability.PermissionDenied)
        observer.onAvailabilityChanged(CameraAvailability.PermissionDenied)

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.CAMERA_AVAILABILITY, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.CAMERA_PERMISSION_DENIED, record.code)
    }

    @Test
    fun deniedThenReadyRecordsRecoveryWithoutPlatformMetadata() {
        val sink = FakeDiagnosticSink()
        val observer = CameraAvailabilityDiagnosticObserver(OperationalTelemetry(sink))

        observer.onAvailabilityChanged(CameraAvailability.PermissionDenied)
        observer.onAvailabilityChanged(CameraAvailability.Ready)

        assertEquals(2, sink.records.size)
        assertEquals(DiagnosticCode.CAMERA_PERMISSION_DENIED, sink.records[0].code)
        assertEquals(DiagnosticResult.SUCCESS, sink.records[1].result)
        assertEquals(null, sink.records[1].code)
        assertEquals(0, sink.records[0].durationMillis)
        assertEquals(0, sink.records[1].durationMillis)
    }

    @Test
    fun unavailableUsesStableCode() {
        val sink = FakeDiagnosticSink()
        val observer = CameraAvailabilityDiagnosticObserver(OperationalTelemetry(sink))

        observer.onAvailabilityChanged(CameraAvailability.Unavailable)

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.CAMERA_AVAILABILITY, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.CAMERA_UNAVAILABLE, record.code)
    }

    @Test
    fun telemetryFailureCannotChangeAvailabilityCallback() {
        val observer = CameraAvailabilityDiagnosticObserver(
            OperationalTelemetry(DiagnosticSink { error("sink failure") }),
        )

        observer.onAvailabilityChanged(CameraAvailability.Unavailable)
    }
}
