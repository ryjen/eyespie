package com.micrantha.eyespie.identity

import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticResult
import com.micrantha.eyespie.telemetry.FakeDiagnosticSink
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class TelemetryPlayerIdentityRepositoryTest {
    @Test
    fun identityResolutionIsNestedUnderCallingOperationWithoutIdentityPayload() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)
        val expected = PlayerIdentity(PlayerId("private-player-id"), "Private display name")
        val repository = TelemetryPlayerIdentityRepository(
            delegate = object : PlayerIdentityRepository {
                override suspend fun current(): PlayerIdentity = expected
            },
            telemetry = telemetry,
        )

        val identity = telemetry.observe(DiagnosticOperation.GAME_SNAPSHOT_LOAD) {
            repository.current()
        }

        assertEquals(expected, identity)
        assertEquals(2, sink.records.size)
        val identityRecord = sink.records[0]
        val parentRecord = sink.records[1]
        assertEquals(DiagnosticOperation.IDENTITY_RESOLVE, identityRecord.operation)
        assertEquals(DiagnosticResult.SUCCESS, identityRecord.result)
        assertEquals(parentRecord.correlation.traceId, identityRecord.correlation.traceId)
        assertEquals(parentRecord.correlation.spanId, identityRecord.correlation.parentSpanId)
    }

    @Test
    fun identityFailureUsesStableCodeAndPreservesException() = runTest {
        val sink = FakeDiagnosticSink()
        val telemetry = OperationalTelemetry(sink)
        val repository = TelemetryPlayerIdentityRepository(
            delegate = object : PlayerIdentityRepository {
                override suspend fun current(): PlayerIdentity {
                    throw IllegalStateException("private keystore implementation detail")
                }
            },
            telemetry = telemetry,
        )

        assertFailsWith<IllegalStateException> { repository.current() }

        val record = sink.records.single()
        assertEquals(DiagnosticOperation.IDENTITY_RESOLVE, record.operation)
        assertEquals(DiagnosticResult.FAILED, record.result)
        assertEquals(DiagnosticCode.IDENTITY_UNAVAILABLE, record.code)
    }
}
