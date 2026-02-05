package com.micrantha.bluebell.observability.domain

/**
 * Domain interface for app usage observability.
 *
 * Goal: keep app/features dependent on a small contract, while implementations can change
 * (offline queue, destinations, batching, etc.).
 */
interface UsageObservability {

    /**
     * Records a usage event.
     *
     * Implementations should be safe to call while offline.
     */
    suspend fun track(event: UsageEvent): Result<Unit>

    /**
     * Attempts to flush queued/offline events.
     *
     * Returns the number of events flushed (best-effort), or a failure if the flush could not run.
     */
    suspend fun flush(): Result<Int>
}

/**
 * Domain model for usage events.
 *
 * Keep this small and stable; map to vendor-specific schemas in infrastructure.
 */
data class UsageEvent(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
)
