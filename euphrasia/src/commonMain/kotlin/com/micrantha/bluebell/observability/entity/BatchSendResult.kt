package com.micrantha.bluebell.observability.entity

data class BatchSendResult(
    val totalEvents: Int,
    val acceptedEvents: Int,
    val rejectedEvents: List<RejectedEvent>,
    val latencyMs: Long,
    val metadata: Map<String, Any> = emptyMap()
)
