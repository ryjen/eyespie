package com.micrantha.bluebell.observability.entity

// Batch operation result
data class BatchResult(
    val totalEvents: Int,
    val successfulEvents: Int,
    val failedEvents: Int,
    val failures: List<EventFailure>,
    val processingTimeMs: Long
)
