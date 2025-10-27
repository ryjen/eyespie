package com.micrantha.bluebell.observability.entity

import com.micrantha.eyespie.core.data.account.model.CurrentSession
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
sealed class AnalyticsEvent(
    override val eventId: String,
    override val properties: Map<String, Any> = emptyMap(),
    override val timestamp: Instant = Clock.System.now(),
    override val userId: String? = CurrentSession.userId,
    override val sessionId: String? = CurrentSession.sessionId,
    override val schema: SchemaVersion
) : TelemetryEvent {

    data class FeatureUsage(
        override val eventId: String = Uuid.random().toString(),
        override val properties: Map<String, Any> = emptyMap(),
        override val schema: SchemaVersion = SchemaVersion("analytics.feature_usage", 1)
    ) : AnalyticsEvent(
        eventId = eventId,
        properties = properties,
        schema = schema
    )

    data class UserFlow(
        override val eventId: String = Uuid.random().toString(),
        override val schema: SchemaVersion = SchemaVersion("analytics.user_flow", 1)
    ) : AnalyticsEvent(
        eventId = eventId,
        schema = schema
    )

    data class BusinessMetric(
        override val eventId: String = Uuid.random().toString(),
        override val schema: SchemaVersion = SchemaVersion("analytics.business_metric", 1)
    ) : AnalyticsEvent(
        eventId = eventId,
        schema = schema
    )
}
