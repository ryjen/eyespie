package com.micrantha.eyespie.presentation

import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticOutcome
import com.micrantha.eyespie.telemetry.OperationalTelemetry

/**
 * Converts the existing platform-neutral camera availability callback into a bounded diagnostic
 * sequence. The observer owns no permission authority and deliberately suppresses duplicate state.
 */
internal class CameraAvailabilityDiagnosticObserver(
    private val telemetry: OperationalTelemetry,
) {
    private var previous: CameraAvailability? = null

    fun onAvailabilityChanged(availability: CameraAvailability) {
        if (availability == previous) return
        previous = availability

        when (availability) {
            CameraAvailability.Requestable -> Unit
            CameraAvailability.Ready -> telemetry.record(
                DiagnosticOperation.CAMERA_AVAILABILITY,
                DiagnosticOutcome.Success,
            )
            CameraAvailability.PermissionDenied -> telemetry.record(
                DiagnosticOperation.CAMERA_AVAILABILITY,
                DiagnosticOutcome.failed(DiagnosticCode.CAMERA_PERMISSION_DENIED),
            )
            CameraAvailability.Unavailable -> telemetry.record(
                DiagnosticOperation.CAMERA_AVAILABILITY,
                DiagnosticOutcome.failed(DiagnosticCode.CAMERA_UNAVAILABLE),
            )
        }
    }
}
