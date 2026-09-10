package com.micrantha.eyespie.telemetry

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
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
        assertW3cTraceId(record.correlation.traceId)
        assertW3cSpanId(record.correlation.spanId)
        assertEquals(null, record.correlation.parentSpanId)
    }

    @Test
    fun nestedOperationsShareTraceAndLinkToParentSpan() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        telemetry.observe(DiagnosticOperation.GAME_CREATE) {
            telemetry.observe(DiagnosticOperation.TARGET_EMBEDDING_GENERATE) { Unit }
        }

        assertEquals(2, sink.records.size)
        val child = sink.records[0]
        val parent = sink.records[1]
        assertEquals(DiagnosticOperation.TARGET_EMBEDDING_GENERATE, child.operation)
        assertEquals(DiagnosticOperation.GAME_CREATE, parent.operation)
        assertEquals(parent.correlation.traceId, child.correlation.traceId)
        assertEquals(parent.correlation.spanId, child.correlation.parentSpanId)
        assertEquals(null, parent.correlation.parentSpanId)
        assertNotEquals(parent.correlation.spanId, child.correlation.spanId)
        assertW3cTraceId(child.correlation.traceId)
        assertW3cSpanId(child.correlation.spanId)
    }

    @Test
    fun separateTopLevelOperationsReceiveSeparateTraces() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        telemetry.observe(DiagnosticOperation.GAME_CREATE) { Unit }
        telemetry.observe(DiagnosticOperation.GAME_GUESS) { Unit }

        assertNotEquals(sink.records[0].correlation.traceId, sink.records[1].correlation.traceId)
    }

    @Test
    fun synchronousBootstrapStagesShareTraceAndLinkToParentSpan() {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        val value = telemetry.observeSync(DiagnosticOperation.RUNTIME_INITIALIZE) { root ->
            val database = telemetry.observeSync(
                operation = DiagnosticOperation.DATABASE_OPEN,
                parent = root,
            ) { "database" }
            assertEquals("database", database)
            "runtime"
        }

        assertEquals("runtime", value)
        assertEquals(2, sink.records.size)
        val child = sink.records[0]
        val parent = sink.records[1]
        assertEquals(DiagnosticOperation.DATABASE_OPEN, child.operation)
        assertEquals(DiagnosticOperation.RUNTIME_INITIALIZE, parent.operation)
        assertEquals(parent.correlation.traceId, child.correlation.traceId)
        assertEquals(parent.correlation.spanId, child.correlation.parentSpanId)
        assertEquals(null, parent.correlation.parentSpanId)
    }

    @Test
    fun synchronousBootstrapFailureUsesStableStageAndParentCodes() {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)

        assertFailsWith<IllegalStateException> {
            telemetry.observeSync(DiagnosticOperation.RUNTIME_INITIALIZE) { root ->
                telemetry.observeSync(
                    operation = DiagnosticOperation.EMBEDDING_MODEL_LOAD,
                    parent = root,
                ) {
                    throw IllegalStateException("private model implementation detail")
                }
            }
        }

        assertEquals(2, sink.records.size)
        val stage = sink.records[0]
        val parent = sink.records[1]
        assertEquals(DiagnosticOperation.EMBEDDING_MODEL_LOAD, stage.operation)
        assertEquals(DiagnosticResult.FAILED, stage.result)
        assertEquals(DiagnosticCode.EMBEDDING_MODEL_LOAD_FAILED, stage.code)
        assertEquals(DiagnosticOperation.RUNTIME_INITIALIZE, parent.operation)
        assertEquals(DiagnosticResult.FAILED, parent.result)
        assertEquals(DiagnosticCode.RUNTIME_INITIALIZATION_FAILED, parent.code)
        assertEquals(parent.correlation.traceId, stage.correlation.traceId)
        assertEquals(parent.correlation.spanId, stage.correlation.parentSpanId)
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
        assertEquals(0, snapshot.droppedRecords)
        assertEquals(
            listOf(DiagnosticOperation.GAME_CREATE, DiagnosticOperation.GAME_GUESS),
            snapshot.records.map(DiagnosticRecord::operation),
        )
    }

    @Test
    fun diagnosticSnapshotRejectsNegativeLossCounters() {
        assertFailsWith<IllegalArgumentException> {
            DiagnosticSnapshot(
                records = emptyList(),
                evictedRecords = -1,
                droppedRecords = 0,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DiagnosticSnapshot(
                records = emptyList(),
                evictedRecords = 0,
                droppedRecords = -1,
            )
        }
    }

    @Test
    fun diagnosticCorrelationRequiresW3cShapedNonZeroIds() {
        assertFailsWith<IllegalArgumentException> {
            DiagnosticCorrelation(traceId = "1", spanId = spanId(1))
        }
        assertFailsWith<IllegalArgumentException> {
            DiagnosticCorrelation(traceId = "0".repeat(32), spanId = spanId(1))
        }
        assertFailsWith<IllegalArgumentException> {
            DiagnosticCorrelation(traceId = traceId(1), spanId = "0".repeat(16))
        }
        assertFailsWith<IllegalArgumentException> {
            DiagnosticCorrelation(traceId = traceId(1), spanId = spanId(1), parentSpanId = spanId(1))
        }
    }

    @Test
    fun successfulRecordCannotCarryFailureCode() {
        assertFailsWith<IllegalArgumentException> {
            DiagnosticRecord(
                operation = DiagnosticOperation.GAME_CREATE,
                result = DiagnosticResult.SUCCESS,
                code = DiagnosticCode.PERSISTENCE_FAILED,
                durationMillis = 1,
                correlation = DiagnosticCorrelation(traceId = traceId(1), spanId = spanId(1)),
            )
        }
    }

    private fun assertW3cTraceId(value: String) {
        assertTrue(Regex("[0-9a-f]{32}").matches(value))
        assertTrue(value.any { it != '0' })
    }

    private fun assertW3cSpanId(value: String) {
        assertTrue(Regex("[0-9a-f]{16}").matches(value))
        assertTrue(value.any { it != '0' })
    }

    private fun record(operation: DiagnosticOperation): DiagnosticRecord = DiagnosticRecord(
        operation = operation,
        result = DiagnosticResult.SUCCESS,
        durationMillis = 0,
        correlation = DiagnosticCorrelation(
            traceId = traceId(1),
            spanId = spanId(operation.ordinal + 1),
        ),
    )

    private fun traceId(value: Int): String = value.toString(16).padStart(32, '0')
    private fun spanId(value: Int): String = value.toString(16).padStart(16, '0')
}
