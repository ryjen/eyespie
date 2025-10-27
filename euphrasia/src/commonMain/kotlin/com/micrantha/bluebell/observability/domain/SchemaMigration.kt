package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.TelemetryEvent

interface SchemaMigration {
    fun migrate(event: TelemetryEvent): TelemetryEvent
}
