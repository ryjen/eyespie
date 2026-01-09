package com.micrantha.bluebell.observability.entity

data class FlushResult(
    val flushedCount: Int,
    val failedCount: Int,
    val durationMs: Long
)
