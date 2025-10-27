package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.EventSchema
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.entity.ValidationResult

interface SchemaRegistry {
    suspend fun register(schema: EventSchema): Result<Unit>
    suspend fun getSchema(version: SchemaVersion): EventSchema?
    suspend fun getLatestSchema(name: String): EventSchema?
    suspend fun getAllVersions(name: String): List<EventSchema>
    suspend fun deprecateSchema(version: SchemaVersion): Result<Unit>
    suspend fun validate(event: TelemetryEvent): ValidationResult
    suspend fun migrate(event: TelemetryEvent, version: SchemaVersion): TelemetryEvent
}
