package com.micrantha.eyespie.telemetry

import kotlinx.coroutines.sync.Mutex
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
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
    MATCH_EVALUATE,
    GAME_PERSIST,
    PROGRESS_PERSIST,
    CAMERA_CAPTURE,
    CAMERA_AVAILABILITY,
    BUNDLE_EXPORT,
    BUNDLE_IMPORT_PREVIEW,
    BUNDLE_IMPORT,
    GAME_OPEN_HANDOFF,
    GAME_SAVE_HANDOFF,
    GAME_SHARE_HANDOFF,
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
    CAMERA_PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    SIGNING_IDENTITY_MISMATCH,
    BUNDLE_INVALID_FORMAT,
    BUNDLE_INVALID_GAME,
    BUNDLE_SIGNING_FAILED,
    BUNDLE_SIGNATURE_SELF_CHECK_FAILED,
    BUNDLE_CREATOR_ID_MISMATCH,
    BUNDLE_INVALID_SIGNATURE,
    BUNDLE_SIGNATURE_VERIFICATION_FAILED,
    BUNDLE_CONFLICT,
    HANDOFF_BUSY,
    HANDOFF_TOO_LARGE,
    HANDOFF_FAILED,
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
    val droppedRecords: Long = 0,
    val incomplete: Boolean = false,
) {
    init {
        require(evictedRecords >= 0) { "diagnostic eviction count must be non-negative" }
        require(droppedRecords >= 0) { "diagnostic drop count must be non-negative" }
    }
}

/**
 * Small in-memory ring buffer for local diagnostics.
 *
 * The mutex is deliberately non-suspending at the write boundary: contention
 * drops observational telemetry rather than delaying gameplay. Both capacity
 * evictions and contention drops are counted so an exported snapshot can expose
 * that its retained timeline is incomplete without retaining any dropped payload.
 * Snapshot lock contention also returns an explicitly incomplete view rather than
 * silently presenting an empty history as authoritative.
 */
@OptIn(ExperimentalAtomicApi::class)
class BoundedDiagnosticSink(
    private val capacity: Int = DEFAULT_CAPACITY,
) : DiagnosticSink, DiagnosticHistory {
    private val mutex = Mutex()
    private val records = ArrayDeque<DiagnosticRecord>(capacity)
    private var evictedRecords: Long = 0
    private val droppedRecords = AtomicLong(0)

    init {
        require(capacity > 0) { "diagnostic capacity must be positive" }
    }

    override fun record(record: DiagnosticRecord) {
        if (!mutex.tryLock()) {
            droppedRecords.fetchAndIncrement()
            return
        }
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
        if (!mutex.tryLock()) {
            return DiagnosticSnapshot(
                records = emptyList(),
                evictedRecords = 0,
                droppedRecords = droppedRecords.load(),
                incomplete = true,
            )
        }
        return try {
            DiagnosticSnapshot(
                records = records.toList(),
                evictedRecords = evictedRecords,
                droppedRecords = droppedRecords.load(),
                incomplete = false,
            )
        } finally {
            mutex.unlock()
        }
    }

    override fun clear() {
        if (!mutex.tryLock()) return
        try {
            droppedRecords.store(0)
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
    DiagnosticOperation.MATCH_EVALUATE -> DiagnosticCode.MATCH_POLICY_INVALID
    DiagnosticOperation.GAME_PERSIST,
    DiagnosticOperation.PROGRESS_PERSIST,
    -> DiagnosticCode.PERSISTENCE_FAILED
    DiagnosticOperation.CAMERA_CAPTURE -> DiagnosticCode.CAMERA_CAPTURE_FAILED
    DiagnosticOperation.CAMERA_AVAILABILITY -> DiagnosticCode.CAMERA_UNAVAILABLE
    DiagnosticOperation.GAME_OPEN_HANDOFF,
    DiagnosticOperation.GAME_SAVE_HANDOFF,
    DiagnosticOperation.GAME_SHARE_HANDOFF,
    -> DiagnosticCode.HANDOFF_FAILED
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

    /**
     * Records an already-classified callback/lifecycle outcome without fabricating a timed span.
     * This remains fail-open and accepts only the closed diagnostic vocabulary.
     */
    internal fun record(
        operation: DiagnosticOperation,
        outcome: DiagnosticOutcome,
    ) {
        emit(operation, outcome, durationMillis = 0)
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
