package com.micrantha.bluebell.observability.entity

data class EventFailure(
    val event: TelemetryEvent,
    val reason: FailureReason,
    val message: String,
    val retryable: Boolean
)
