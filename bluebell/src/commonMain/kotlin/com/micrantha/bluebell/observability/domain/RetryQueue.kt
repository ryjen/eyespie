package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.RetryableEvent

interface RetryQueue {
    suspend fun add(event: RetryableEvent)
    suspend fun addBatch(events: List<RetryableEvent>)
    suspend fun getReady(): List<RetryableEvent>
    suspend fun remove(eventIds: List<String>)
    suspend fun markFailed(eventId: String, error: Throwable)
    suspend fun clear()
    suspend fun size(): Int
    suspend fun isEmpty(): Boolean
}
