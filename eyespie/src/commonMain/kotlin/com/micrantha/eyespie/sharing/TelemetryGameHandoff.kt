package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticOutcome
import com.micrantha.eyespie.telemetry.OperationalTelemetry

/**
 * Observes platform document handoff outcomes without importing platform URI/path authority into
 * common state. The delegate remains solely responsible for scoped platform access and byte bounds.
 */
internal class TelemetryGameDocumentTransfer(
    private val delegate: GameDocumentTransfer,
    private val telemetry: OperationalTelemetry,
) : GameDocumentTransfer {
    override suspend fun read(): GameDocumentReadResult = telemetry.observe(
        operation = DiagnosticOperation.GAME_OPEN_HANDOFF,
        classify = GameDocumentReadResult::toDiagnosticOutcome,
    ) {
        delegate.read()
    }

    override suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameDocumentWriteResult = telemetry.observe(
        operation = DiagnosticOperation.GAME_SAVE_HANDOFF,
        classify = GameDocumentWriteResult::toDiagnosticOutcome,
    ) {
        delegate.write(suggestedFileName, bytes)
    }
}

/**
 * Observes only whether the platform accepted the share presentation request. A successful record
 * does not assert recipient selection, transmission, delivery, or persistence.
 */
internal class TelemetryGameSharePresenter(
    private val delegate: GameSharePresenter,
    private val telemetry: OperationalTelemetry,
) : GameSharePresenter {
    override suspend fun present(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameSharePresentationResult = telemetry.observe(
        operation = DiagnosticOperation.GAME_SHARE_HANDOFF,
        classify = GameSharePresentationResult::toDiagnosticOutcome,
    ) {
        delegate.present(suggestedFileName, bytes)
    }
}

private fun GameDocumentReadResult.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
    is GameDocumentReadResult.Success -> DiagnosticOutcome.Success
    GameDocumentReadResult.Cancelled -> DiagnosticOutcome.Cancelled
    GameDocumentReadResult.Busy -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_BUSY)
    GameDocumentReadResult.TooLarge -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_TOO_LARGE)
    GameDocumentReadResult.Failed -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_FAILED)
}

private fun GameDocumentWriteResult.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
    GameDocumentWriteResult.Success -> DiagnosticOutcome.Success
    GameDocumentWriteResult.Cancelled -> DiagnosticOutcome.Cancelled
    GameDocumentWriteResult.Busy -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_BUSY)
    GameDocumentWriteResult.TooLarge -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_TOO_LARGE)
    GameDocumentWriteResult.Failed -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_FAILED)
}

private fun GameSharePresentationResult.toDiagnosticOutcome(): DiagnosticOutcome = when (this) {
    GameSharePresentationResult.Presented -> DiagnosticOutcome.Success
    GameSharePresentationResult.Busy -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_BUSY)
    GameSharePresentationResult.TooLarge -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_TOO_LARGE)
    GameSharePresentationResult.Failed -> DiagnosticOutcome.failed(DiagnosticCode.HANDOFF_FAILED)
}
