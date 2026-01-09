package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Campaign @OptIn(ExperimentalTime::class) constructor(
    val campaignId: String,
    val name: String,
    val startDate: Instant,
    val endDate: Instant,
    val targetEvents: Set<String>
)
