package com.micrantha.bluebell.observability.entity

data class EventFilter(
    val eventTypes: Set<String>? = null,
    val userId: String? = null,
    val properties: Map<String, Any>? = null,
    val spanId: String? = null,
    val campaignId: String? = null
)
