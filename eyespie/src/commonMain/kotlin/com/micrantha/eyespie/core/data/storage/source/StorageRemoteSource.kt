package com.micrantha.eyespie.core.data.storage.source

import com.micrantha.eyespie.core.data.client.SupaClient

internal interface StorageRemoteSource {
    fun url(bucketID: String, path: String): Result<String>
    suspend fun download(bucketID: String, path: String): Result<ByteArray>
    suspend fun upload(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Result<String>
}

internal class SupabaseStorageRemoteSource(
    private val supabase: SupaClient,
) : StorageRemoteSource {

    override fun url(bucketID: String, path: String): Result<String> = try {
        val result = supabase.storage(bucketID).authenticatedRenderUrl(path)
        Result.success(result)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun download(bucketID: String, path: String): Result<ByteArray> = try {
        val result = supabase.storage(bucketID)
            .downloadAuthenticated(path)
        Result.success(result)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun upload(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Result<String> = try {
        val key = supabase.storage(bucketId).upload(path, data).key
            ?: return Result.failure(IllegalStateException("storage upload returned no object key"))
        Result.success(key)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
