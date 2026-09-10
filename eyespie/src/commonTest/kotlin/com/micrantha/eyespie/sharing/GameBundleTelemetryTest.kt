package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.identity.SigningIdentity
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticResult
import com.micrantha.eyespie.telemetry.FakeDiagnosticSink
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class GameBundleTelemetryTest {
    @Test
    fun invalidPreviewEmitsOnlyStableFormatFailure() = runTest {
        val sink = FakeDiagnosticSink()
        val service = service(OperationalTelemetry(sink))

        assertIs<GameBundleImportPreviewResult.InvalidFormat>(
            service.previewImport(byteArrayOf()),
        )

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.BUNDLE_IMPORT_PREVIEW, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.BUNDLE_INVALID_FORMAT, record.code)
    }

    @Test
    fun invalidImportEmitsOnlyStableFormatFailure() = runTest {
        val sink = FakeDiagnosticSink()
        val service = service(OperationalTelemetry(sink))

        assertIs<GameBundleImportResult.InvalidFormat>(service.import(byteArrayOf()))

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.BUNDLE_IMPORT, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.BUNDLE_INVALID_FORMAT, record.code)
    }

    @Test
    fun exportIdentityFailureUsesExistingStableIdentityCode() = runTest {
        val sink = FakeDiagnosticSink()
        val service = service(OperationalTelemetry(sink))

        val result = assertIs<GameBundleExportResult.Failure>(
            service.export(GameId("game:test")),
        )
        assertEquals(GameBundleExportFailureCode.IDENTITY_UNAVAILABLE, result.code)

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.BUNDLE_EXPORT, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.IDENTITY_UNAVAILABLE, record.code)
    }

    private fun service(telemetry: OperationalTelemetry): GameBundleService = GameBundleService(
        identityRepository = object : PlayerIdentityRepository {
            override suspend fun current(): PlayerIdentity = error("identity unavailable")
        },
        signingIdentity = object : SigningIdentity {
            override suspend fun publicKey(): ByteArray = error("not reached")
            override suspend fun sign(payload: ByteArray): ByteArray = error("not reached")
            override suspend fun verify(
                publicKey: ByteArray,
                payload: ByteArray,
                signature: ByteArray,
            ): Boolean = error("not reached")
        },
        gameRepository = object : GameRepository {
            override suspend fun list(): List<Game> = emptyList()
            override suspend fun get(id: GameId): Game? = null
            override suspend fun save(game: Game) = Unit
        },
        telemetry = telemetry,
    )
}
