package com.micrantha.bluebell.observability.domain

/**
 * Domain interface for app usage observability.
 */
interface UsageObservability {
    suspend fun track(event: UsageEvent): Result<Unit>
    suspend fun flush(): Result<Int>
}

data class UsageEvent(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
)
