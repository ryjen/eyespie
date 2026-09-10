package com.micrantha.eyespie.features.utility

fun interface DiagnosticsExporter {
    suspend fun export(): DiagnosticExportResult
}

sealed interface DiagnosticExportResult {
    data object Exported : DiagnosticExportResult
    data object Cancelled : DiagnosticExportResult
    data object Busy : DiagnosticExportResult
    data object TooLarge : DiagnosticExportResult
    data object Failed : DiagnosticExportResult
    data object Unavailable : DiagnosticExportResult
}

object UnavailableDiagnosticsExporter : DiagnosticsExporter {
    override suspend fun export(): DiagnosticExportResult = DiagnosticExportResult.Unavailable
}
