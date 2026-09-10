package com.micrantha.eyespie.telemetry

const val DIAGNOSTIC_ARTIFACT_FILE_NAME = "eyespie-diagnostics.json"
const val DIAGNOSTIC_ARTIFACT_MIME_TYPE = "application/json"

interface DiagnosticArtifactWriter {
    suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): DiagnosticArtifactWriteResult
}

sealed interface DiagnosticArtifactWriteResult {
    data object Success : DiagnosticArtifactWriteResult
    data object Cancelled : DiagnosticArtifactWriteResult
    data object Busy : DiagnosticArtifactWriteResult
    data object TooLarge : DiagnosticArtifactWriteResult
    data object Failed : DiagnosticArtifactWriteResult
}
