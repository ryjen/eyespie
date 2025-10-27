package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class TimeRange @OptIn(ExperimentalTime::class) constructor(
    val start: Instant,
    val end: Instant
)
