package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class MixpanelConfig(
    override val enabled: Boolean = true,
    override val batchSize: Int = 50,
    override val flushInterval: Duration = 60.seconds,
    override val retryPolicy: RetryPolicy = RetryPolicy.default(),
    override val timeout: Duration = 30.seconds,
    val token: String,
    val serverUrl: String? = null,
    val useIpAddressForGeolocation: Boolean = true
) : DestinationConfig
