package com.micrantha.eyespie.game

import com.micrantha.eyespie.telemetry.DiagnosticExportService
import com.micrantha.eyespie.telemetry.DiagnosticHistory
import com.micrantha.eyespie.telemetry.OperationalTelemetry

/**
 * Diagnostic capability that remains available when platform runtime construction
 * fails. The retained surface is observational only and contains no canonical game
 * state or transport.
 */
data class EyespieBootstrapDiagnostics(
    val history: DiagnosticHistory,
    val export: DiagnosticExportService,
    val telemetry: OperationalTelemetry,
)

sealed interface EyespieRuntimeBootstrapResult {
    data class Ready(val runtime: EyespieRuntime) : EyespieRuntimeBootstrapResult

    /**
     * [cause] is retained only for immediate platform-local logging. It is not part
     * of the diagnostic export model and must never be serialized into support data.
     */
    data class Failed(
        val diagnostics: EyespieBootstrapDiagnostics,
        val cause: Exception,
    ) : EyespieRuntimeBootstrapResult
}

internal fun EyespieRuntimeBootstrapResult.requireRuntime(): EyespieRuntime = when (this) {
    is EyespieRuntimeBootstrapResult.Ready -> runtime
    is EyespieRuntimeBootstrapResult.Failed -> throw cause
}
