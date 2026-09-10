package com.micrantha.eyespie.app

import com.micrantha.eyespie.features.utility.DiagnosticExportResult
import com.micrantha.eyespie.telemetry.BoundedDiagnosticSink
import com.micrantha.eyespie.telemetry.DIAGNOSTIC_ARTIFACT_FILE_NAME
import com.micrantha.eyespie.telemetry.DiagnosticArtifactWriteResult
import com.micrantha.eyespie.telemetry.DiagnosticArtifactWriter
import com.micrantha.eyespie.telemetry.DiagnosticExportService
import com.micrantha.eyespie.telemetry.DiagnosticIdentity
import com.micrantha.eyespie.telemetry.DiagnosticIdentityProvider
import com.micrantha.eyespie.telemetry.DiagnosticPlatform
import com.micrantha.eyespie.telemetry.DiagnosticReleaseIdentity
import com.micrantha.eyespie.telemetry.DiagnosticRuntimeIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ScopedDiagnosticsExporterTest {
    @Test
    fun exportUsesBoundedArtifactWriterAndZeroesTemporaryBytes() = runTest {
        var suggestedName: String? = null
        var writtenCopy: ByteArray? = null
        var handedOffBytes: ByteArray? = null
        val writer = object : DiagnosticArtifactWriter {
            override suspend fun write(
                suggestedFileName: String,
                bytes: ByteArray,
            ): DiagnosticArtifactWriteResult {
                suggestedName = suggestedFileName
                writtenCopy = bytes.copyOf()
                handedOffBytes = bytes
                return DiagnosticArtifactWriteResult.Success
            }
        }

        val result = ScopedDiagnosticsExporter(service(), writer).export()

        assertEquals(DiagnosticExportResult.Exported, result)
        assertEquals(DIAGNOSTIC_ARTIFACT_FILE_NAME, suggestedName)
        assertTrue(assertNotNull(writtenCopy).isNotEmpty())
        assertTrue(assertNotNull(handedOffBytes).all { it == 0.toByte() })
    }

    @Test
    fun unavailableWriterRemainsNonFatal() = runTest {
        val result = ScopedDiagnosticsExporter(service(), writer = null).export()

        assertEquals(DiagnosticExportResult.Unavailable, result)
    }

    private fun service(): DiagnosticExportService = DiagnosticExportService(
        history = BoundedDiagnosticSink(),
        identityProvider = DiagnosticIdentityProvider {
            DiagnosticIdentity(
                release = DiagnosticReleaseIdentity("0.1.0", 1),
                runtime = DiagnosticRuntimeIdentity(
                    platform = DiagnosticPlatform.ANDROID,
                    osVersion = "Android 16",
                    mediaPipeVersion = "0.10.26",
                ),
            )
        },
    )
}
