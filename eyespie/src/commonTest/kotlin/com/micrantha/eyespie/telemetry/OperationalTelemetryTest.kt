package com.micrantha.eyespie.telemetry

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OperationalTelemetryTest {
    @Test
    fun successfulOperationEmitsTypedBoundedRecord() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        val value = telemetry.observe(DiagnosticOperation.GAME_CREATE) { "created" }

        assertEquals("created", value)
        val record = sink.records.single()
        assertEquals(DiagnosticRecord.SCHEMA_VERSION, record.schemaVersion)
        assertEquals(DiagnosticOperation.GAME_CREATE, record.operation)
        assertEquals(DiagnosticResult.SUCCESS, record.result)
        assertEquals(null, record.code)
        assertTrue(record.durationMillis >= 0)
    }

    @Test
    fun typedFailureRetainsStableDiagnosticCode() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        telemetry.observe(
            operation = DiagnosticOperation.GAME_GUESS,
            classify = {
                DiagnosticOutcome.failed(DiagnosticCode.MATCH_POLICY_INVALID)
            },
        ) { Unit }

        val record = sink.records.single()
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.MATCH_POLICY_INVALID, record.code)
    }

    @Test
    fun cancellationIsObservedAndStillPropagates() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        assertFailsWith<CancellationException> {
            telemetry.observe(DiagnosticOperation.GAME_CREATE) {
                throw CancellationException("cancelled")
            }
        }

        val record = sink.records.single()
        assertEquals(DiagnosticResult.CANCELLED, record.result)
        assertEquals(null, record.code)
    }

    @Test
    fun sinkFailureCannotChangeSourceOperationResult() = runTest {
        val telemetry = OperationalTelemetry(
            DiagnosticSink { throw IllegalStateException("diagnostic storage unavailable") },
        )

        val value = telemetry.observe(DiagnosticOperation.GAME_CREATE) { 42 }

        assertEquals(42, value)
    }

    @Test
    fun boundedSinkEvictsOldestRecordDeterministically() {
        val sink = BoundedDiagnosticSink(capacity = 2)

        sink.record(record(DiagnosticOperation.GAME_SNAPSHOT_LOAD))
        sink.record(record(DiagnosticOperation.GAME_CREATE))
        sink.record(record(DiagnosticOperation.GAME_GUESS))

        val snapshot = sink.snapshot()
        assertEquals(1, snapshot.evictedRecords)
        assertEquals(
            listOf(DiagnosticOperation.GAME_CREATE, DiagnosticOperation.GAME_GUESS),
            snapshot.records.map(DiagnosticRecord::operation),
        )
    }

    @Test
    fun successfulRecordCannotCarryFailureCode() {
        assertFailsWith<IllegalArgumentException> {
            DiagnosticRecord(
                operation = DiagnosticOperation.GAME_CREATE,
                result = DiagnosticResult.SUCCESS,
                code = DiagnosticCode.PERSISTENCE_FAILED,
                durationMillis = 1,
            )
        }
    }

    private fun record(operation: DiagnosticOperation): DiagnosticRecord = DiagnosticRecord(
        operation = operation,
        result = DiagnosticResult.SUCCESS,
        durationMillis = 0,
    )
}
