package com.micrantha.eyespie.telemetry

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * UI-only access to the application-owned telemetry facade. Domain code should
 * receive [OperationalTelemetry] explicitly rather than reading this local.
 */
val LocalOperationalTelemetry = staticCompositionLocalOf { OperationalTelemetry() }
