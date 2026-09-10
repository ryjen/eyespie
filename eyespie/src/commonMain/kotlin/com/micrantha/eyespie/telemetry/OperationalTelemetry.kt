package com.micrantha.eyespie.telemetry

import kotlinx.coroutines.sync.Mutex
import kotlin.time.TimeSource

/**
 * Stable, privacy-bounded operations that can be correlated with release/runtime
 * identity by higher-level diagnostic export tooling.
 *
 * Do not add user content, object identifiers, file paths, URLs, or other
 * high-cardinality values to this vocabulary.
 */
enum class DiagnosticOperation {
    GAME_SNAPSHOT_LOAD,
    GAME_CREATE,
    CLUE_ADD,
    GAME_GUESS,
    TARGET_EMBEDDING_GENERATE,
    GUESS_EMBEDDING_GENERATE,
    GAME_PERSIST,
    PROGRESS_PERSIST,
    CAMERA_CAPTURE,
}

enum class DiagnosticResult {
    SUCCESS,
    CANCELLED,
    DEGRADED,
    FAILED,
}

/**
 * Stable support codes. These intentionally do not carry exception messages or
 * arbitrary strings so the ordinary diagnostic record cannot become a content
 * or secret exfiltration channel.
 */
enum class DiagnosticCode {
    GAME_OPERATION_IN_PROGRESS,
    GAME_INVALID_NAME,
    GAME_INVALID_CLUE,
    IDENTITY_UNAVAILABLE,
    NOT_LOCAL_CREATOR,
    TARGET_EMBEDDING_FAILED,
    GUESS_EMBEDDING_FAILED,
    GAME_NOT_FOUND,
    THING_NOT_FOUND,
    MATCH_POLICY_INVALID,
    PERSISTENCE_FAILED,
    CAMERA_CAPTURE_FAILED,
    TELEMETRY_CLASSIFICATION_FAILED,
    UNEXPECTED_FAILURE,
}

data class DiagnosticOutcome(
    val result: DiagnosticResult,
    val code: DiagnosticCode? = null,
) {
    init {
        require(result != DiagnosticResult.SUCCESS || code == null) {
            "successful diagnostics must not carry a failure code"
        }
    }

    companion object {
        val Success = DiagnosticOutcome(DiagnosticResult.SUCCESS)
        val Cancelled = DiagnosticOutcome(DiagnosticResult.CANCELLED)
        val ClassificationDegraded = DiagnosticOutcome(
            result = DiagnosticResult.DEGRADED,
            code = DiagnosticCode.TELEMETRY_CLASSIFICATION_FAILED,
        )
        val UnexpectedFailure = DiagnosticOutcome(
            result = DiagnosticResult.FAILED,
            code = DiagnosticCode.UNEXPECTED_FAILURE,
        )

        fun failed(code: DiagnosticCode): DiagnosticOutcome =
            DiagnosticOutcome(DiagnosticResult.FAILED, code)
    }
}

/**
 * Deliberately small portable record. Release identity and platform/runtime
 * provenance belong to the enclosing diagnostic snapshot/export contract.
 */
data class DiagnosticRecord(
    val schemaVersion: Int = SCHEMA_VERSION,
    val operation: DiagnosticOperation,
    val result: DiagnosticResult,
    val code: DiagnosticCode? = null,
    val durationMillis: Long,
) {
    init {
        require(schemaVersion == SCHEMA_VERSION) { "unsupported diagnostic schema" }
        require(durationMillis >= 0) { "diagnostic duration must be non-negative" }
        require(result != DiagnosticResult.SUCCESS || code == null) {
            "successful diagnostics must not carry a failure code"
        }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

fun interface DiagnosticSink {
    /**
     * Implementations must remain bounded and non-authoritative. Callers still
     * protect the source operation from sink failures.
     */
    fun record(record: DiagnosticRecord)
}

interface DiagnosticHistory {
    fun snapshot(): DiagnosticSnapshot
    fun clear()
}

object NoOpDiagnosticSink : DiagnosticSink {
    override fun record(record: DiagnosticRecord) = Unit
}

/** Deterministic test/preview sink. Not intended as a production retention store. */
class FakeDiagnosticSink : DiagnosticSink {
    private val mutableRecords = mutableListOf<DiagnosticRecord>()

    val records: List<DiagnosticRecord>
        get() = mutableRecords.toList()

    override fun record(record: DiagnosticRecord) {
        mutableRecords += record
    }
}

data class DiagnosticSnapshot(
    val records: List<DiagnosticRecord>,
    val evictedRecords: Long,
)

/**
 * Small in-memory ring buffer for local diagnostics.
 *
 * The mutex is deliberately non-suspending at the write boundary: contention
 * drops observational telemetry rather than delaying gameplay. The retained
 * record count is strictly bounded and oldest records are evicted first.
 */
class BoundedDiagnosticSink(
    private val capacity: Int = DEFAULT_CAPACITY,
) : DiagnosticSink, DiagnosticHistory {
    private val mutex = Mutex()
    private val records = ArrayDeque<DiagnosticRecord>(capacity)
    private var evictedRecords: Long = 0

    init {
        require(capacity > 0) { "diagnostic capacity must be positive" }
    }

    override fun record(record: DiagnosticRecord) {
        if (!mutex.tryLock()) return
        try {
            if (records.size == capacity) {
                records.removeFirst()
                evictedRecords += 1
            }
            records.addLast(record)
        } finally {
            mutex.unlock()
        }
    }

    override fun snapshot(): DiagnosticSnapshot {
        if (!mutex.tryLock()) return DiagnosticSnapshot(emptyList(), evictedRecords = 0)
        return try {
            DiagnosticSnapshot(records.toList(), evictedRecords)
        } finally {
            mutex.unlock()
        }
    }

    override fun clear() {
        if (!mutex.tryLock()) return
        try {
            records.clear()
            evictedRecords = 0
        } finally {
            mutex.unlock()
        }
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 128
    }
}

private fun DiagnosticOperation.defaultFailureCode(): DiagnosticCode = when (this) {
    DiagnosticOperation.TARGET_EMBEDDING_GENERATE -> DiagnosticCode.TARGET_EMBEDDING_FAILED
    DiagnosticOperation.GUESS_EMBEDDING_GENERATE -> DiagnosticCode.GUESS_EMBEDDING_FAILED
    DiagnosticOperation.GAME_PERSIST,
    DiagnosticOperation.PROGRESS_PERSIST,
    -> DiagnosticCode.PERSISTENCE_FAILED
    DiagnosticOperation.CAMERA_CAPTURE -> DiagnosticCode.CAMERA_CAPTURE_FAILED
    else -> DiagnosticCode.UNEXPECTED_FAILURE
}

/**
 * Application-owned telemetry facade. Provider SDKs and transport concerns stay
 * outside game/domain code; this facade can later be backed by an OTel adapter.
 */
class OperationalTelemetry(
    private val sink: DiagnosticSink = NoOpDiagnosticSink,
) {
    suspend fun <T> observe(
        operation: DiagnosticOperation,
        failureCode: DiagnosticCode = operation.defaultFailureCode(),
        classify: (T) -> DiagnosticOutcome = { DiagnosticOutcome.Success },
        block: suspend () -> T,
    ): T {
        val started = TimeSource.Monotonic.markNow()
        return try {
            val value = block()
            val outcome = try {
                classify(value)
            } catch (_: Exception) {
                DiagnosticOutcome.ClassificationDegraded
            }
            emit(operation, outcome, started.elapsedNow().inWholeMilliseconds)
            value
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            emit(operation, DiagnosticOutcome.Cancelled, started.elapsedNow().inWholeMilliseconds)
            throw cancelled
        } catch (throwable: Throwable) {
            emit(
                operation,
                DiagnosticOutcome.failed(failureCode),
                started.elapsedNow().inWholeMilliseconds,
            )
            throw throwable
        }
    }

    private fun emit(
        operation: DiagnosticOperation,
        outcome: DiagnosticOutcome,
        durationMillis: Long,
    ) {
        val record = DiagnosticRecord(
            operation = operation,
            result = outcome.result,
            code = outcome.code,
            durationMillis = durationMillis.coerceAtLeast(0),
        )
        try {
            sink.record(record)
        } catch (_: Exception) {
            // Observability degradation must never alter the source operation.
        }
    }
}
