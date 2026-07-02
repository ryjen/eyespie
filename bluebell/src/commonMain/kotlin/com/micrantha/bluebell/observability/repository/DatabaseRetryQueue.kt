package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.domain.PersistentRetryQueue
import com.micrantha.bluebell.observability.entity.RetryableEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class DatabaseRetryQueue(
//    private val database: EventDatabase
) : PersistentRetryQueue {

    override suspend fun add(event: RetryableEvent): Result<Unit> = withContext(Dispatchers.IO) {
//        database.retryQueueDao().insert(event.toEntity())
        Result.success(Unit)
    }

    override suspend fun addBatch(events: List<RetryableEvent>): Result<Unit> =
        withContext(Dispatchers.IO) {
            Result.success(Unit)
        }

    override suspend fun update(event: RetryableEvent): Result<Unit> = withContext(Dispatchers.IO) {
//        database.retryQueueDao().update(event.toEntity())
        Result.success(Unit)
    }

    override suspend fun remove(eventIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
//        database.retryQueueDao().deleteByIds(eventIds)
            Result.success(Unit)
        }

    override suspend fun getReady(): List<RetryableEvent> = withContext(Dispatchers.IO) {
//        database.retryQueueDao().getAll().map { it.toRetryableEvent() }
        emptyList()
    }

    override suspend fun markFailed(eventId: String, error: Throwable): Result<Unit> =
        withContext(Dispatchers.IO) {
            Result.success(Unit)
        }

    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.IO) {
//        database.retryQueueDao().deleteAll()
        Result.success(Unit)
    }

    override suspend fun size(): Int = 0

    override suspend fun isEmpty(): Boolean = true
}


/*
@Entity(tableName = "retry_queue")
data class RetryQueueEntity(
    @PrimaryKey val eventId: String,
    val eventData: String, // JSON serialized TelemetryEvent
    val destination: String,
    val retryAfterMillis: Long,
    val attempts: Int,
    val maxAttempts: Int,
    val firstAttemptMillis: Long,
    val lastError: String?
)

@Dao
interface RetryQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: RetryQueueEntity)

    @Update
    suspend fun update(event: RetryQueueEntity)

    @Query("DELETE FROM retry_queue WHERE eventId IN (:eventIds)")
    suspend fun deleteByIds(eventIds: List<String>)

    @Query("SELECT * FROM retry_queue ORDER BY retryAfterMillis ASC")
    suspend fun getAll(): List<RetryQueueEntity>

    @Query("DELETE FROM retry_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM retry_queue")
    suspend fun count(): Int

    @Query("DELETE FROM retry_queue WHERE firstAttemptMillis < :timestampMillis")
    suspend fun deleteOlderThan(timestampMillis: Long): Int
}
*/
