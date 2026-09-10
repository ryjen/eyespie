package com.micrantha.eyespie.identity

import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.OperationalTelemetry

/** Observes local signing/public-key availability without exposing identity values. */
class TelemetryPlayerIdentityRepository(
    private val delegate: PlayerIdentityRepository,
    private val telemetry: OperationalTelemetry,
) : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = telemetry.observe(
        operation = DiagnosticOperation.IDENTITY_RESOLVE,
        failureCode = DiagnosticCode.IDENTITY_UNAVAILABLE,
    ) {
        delegate.current()
    }
}
