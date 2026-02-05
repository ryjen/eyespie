package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for sending observability/usage events into Supabase Postgres.
 *
 * This is intentionally small: the Supabase client itself (postgrest) is provided
 * by the app layer; this config only handles destination behavior.
 */
data class SupabaseConfig(
    override val enabled: Boolean = true,
    override val batchSize: Int = 50,
    override val flushInterval: Duration = 30.seconds,
    override val retryPolicy: RetryPolicy = RetryPolicy.default(),
    override val timeout: Duration = 15.seconds,
    val table: String = "usage_events",
) : DestinationConfig
