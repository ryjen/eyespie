package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class CloudStorageConfig(
    override val enabled: Boolean = true,
    override val batchSize: Int = 1000,
    override val flushInterval: Duration = 5.minutes,
    override val retryPolicy: RetryPolicy = RetryPolicy.default(),
    override val timeout: Duration = 2.minutes,
    val bucket: String,
    val region: String,
    val credentials: String,
    val compression: CompressionType = CompressionType.GZIP,
    val encryption: Boolean = true
) : DestinationConfig {
    enum class CompressionType {
        NONE, GZIP, ZSTD
    }
}
