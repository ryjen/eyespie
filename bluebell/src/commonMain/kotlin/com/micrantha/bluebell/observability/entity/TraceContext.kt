package com.micrantha.bluebell.observability.entity

data class TraceContext(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String? = null,
    val traceFlags: Int = 0
)
