package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class FirebaseConfig(
    override val enabled: Boolean = true,
    override val batchSize: Int = 50,
    override val flushInterval: Duration = 30.seconds,
    override val retryPolicy: RetryPolicy = RetryPolicy.default(),
    override val timeout: Duration = 15.seconds,
    val apiKey: String,
    val projectId: String,
    val appId: String,
    val analyticsCollectionEnabled: Boolean = true,
    val sessionTimeoutDuration: Duration = 30.seconds
) : DestinationConfig
