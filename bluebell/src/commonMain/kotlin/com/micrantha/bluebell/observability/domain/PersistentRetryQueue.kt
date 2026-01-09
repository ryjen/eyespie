package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.RetryableEvent

// Persistent retry queue (for app restarts)
interface PersistentRetryQueue {
    suspend fun add(event: RetryableEvent)
    suspend fun update(event: RetryableEvent)
    suspend fun remove(eventIds: List<String>)
    suspend fun getAll(): List<RetryableEvent>
    suspend fun clear()
}
