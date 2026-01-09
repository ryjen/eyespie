package com.micrantha.bluebell.observability.entity

data class CacheConfig(
    val maxMemoryEvents: Int = 1000,
    val maxDiskEvents: Int = 10000,
    val evictionBatchSize: Int = 100,
    val maxRetentionDays: Int = 30
)
