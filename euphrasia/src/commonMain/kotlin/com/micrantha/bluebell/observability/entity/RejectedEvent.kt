package com.micrantha.bluebell.observability.entity


data class RejectedEvent(
    val eventId: String,
    val reason: RejectionReason,
    val message: String
)
