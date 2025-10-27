package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class NewRelicConfig(
    override val enabled: Boolean = true,
    override val batchSize: Int = 100,
    override val flushInterval: Duration = 60.seconds,
    override val retryPolicy: RetryPolicy = RetryPolicy.default(),
    override val timeout: Duration = 30.seconds,
    val apiKey: String,
    val accountId: String,
    val region: String = "US",
    val customAttributes: Map<String, String> = emptyMap()
) : DestinationConfig
