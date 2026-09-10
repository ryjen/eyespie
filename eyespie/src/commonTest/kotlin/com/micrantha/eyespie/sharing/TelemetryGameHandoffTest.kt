package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticResult
import com.micrantha.eyespie.telemetry.FakeDiagnosticSink
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

class TelemetryGameHandoffTest {
    @Test
    fun cancelledOpenIsRecordedAsCancellationWithoutPlatformDetails() = runTest {
        val sink = FakeDiagnosticSink()
        val transfer = TelemetryGameDocumentTransfer(
            delegate = object : GameDocumentTransfer {
                override suspend fun read(): GameDocumentReadResult = GameDocumentReadResult.Cancelled
                override suspend fun write(
                    suggestedFileName: String,
                    bytes: ByteArray,
                ): GameDocumentWriteResult = error("not used")
            },
            telemetry = OperationalTelemetry(sink),
        )

        assertSame(GameDocumentReadResult.Cancelled, transfer.read())

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.GAME_OPEN_HANDOFF, record.operation)
        assertEquals(DiagnosticResult.CANCELLED, record.result)
        assertEquals(null, record.code)
    }

    @Test
    fun oversizedSaveUsesStableHandoffCode() = runTest {
        val sink = FakeDiagnosticSink()
        val transfer = TelemetryGameDocumentTransfer(
            delegate = object : GameDocumentTransfer {
                override suspend fun read(): GameDocumentReadResult = error("not used")
                override suspend fun write(
                    suggestedFileName: String,
                    bytes: ByteArray,
                ): GameDocumentWriteResult = GameDocumentWriteResult.TooLarge
            },
            telemetry = OperationalTelemetry(sink),
        )

        assertSame(
            GameDocumentWriteResult.TooLarge,
            transfer.write("private-name.eyespie", byteArrayOf(1, 2, 3)),
        )

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.GAME_SAVE_HANDOFF, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.HANDOFF_TOO_LARGE, record.code)
    }

    @Test
    fun presentedShareMeansOnlySuccessfulPlatformPresentation() = runTest {
        val sink = FakeDiagnosticSink()
        val presenter = TelemetryGameSharePresenter(
            delegate = sharePresenter(GameSharePresentationResult.Presented),
            telemetry = OperationalTelemetry(sink),
        )

        assertSame(
            GameSharePresentationResult.Presented,
            presenter.present("private-name.eyespie", byteArrayOf(9)),
        )

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.GAME_SHARE_HANDOFF, record.operation)
        assertEquals(DiagnosticResult.SUCCESS, record.result)
        assertEquals(null, record.code)
    }

    @Test
    fun busyShareUsesStableHandoffCode() = runTest {
        val sink = FakeDiagnosticSink()
        val presenter = TelemetryGameSharePresenter(
            delegate = sharePresenter(GameSharePresentationResult.Busy),
            telemetry = OperationalTelemetry(sink),
        )

        assertSame(
            GameSharePresentationResult.Busy,
            presenter.present("private-name.eyespie", byteArrayOf(9)),
        )

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.GAME_SHARE_HANDOFF, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.HANDOFF_BUSY, record.code)
    }

    private fun sharePresenter(result: GameSharePresentationResult): GameSharePresenter =
        object : GameSharePresenter {
            override suspend fun present(
                suggestedFileName: String,
                bytes: ByteArray,
            ): GameSharePresentationResult = result
        }
}
