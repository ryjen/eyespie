package com.micrantha.eyespie.core.data.storage.source

import io.github.reactivecircus.cache4k.Cache
import kotlin.time.Duration.Companion.days

internal interface CacheLocalSource {
    fun get(key: String): Result<ByteArray>
    fun put(key: String, data: ByteArray)
}

internal class DefaultCacheLocalSource : CacheLocalSource {
    private val cache: Cache<String, ByteArray> = Cache.Builder<String, ByteArray>()
        .expireAfterAccess(7.days)
        .build()

    override fun get(key: String): Result<ByteArray> = try {
        Result.success(cache.get(key)!!)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun put(key: String, data: ByteArray) {
        cache.put(key, data)
    }
}
