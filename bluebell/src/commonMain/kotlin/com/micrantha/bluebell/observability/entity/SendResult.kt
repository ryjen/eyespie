package com.micrantha.bluebell.observability.entity

// Results
data class SendResult(
    val eventId: String,
    val accepted: Boolean,
    val destination: Destination,
    val latencyMs: Long,
    val metadata: Map<String, Any> = emptyMap()
)
