package com.micrantha.eyespie.core.data.storage.source

import com.micrantha.eyespie.core.data.client.SupaClient
import kotlin.time.Duration.Companion.days

internal interface StorageRemoteSource {
    fun url(bucketID: String, path: String): Result<String>
    suspend fun download(bucketID: String, path: String): Result<ByteArray>
    suspend fun upload(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Result<Pair<String, String>>
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
    ): Result<Pair<String, String>> = try {
        with(supabase.storage(bucketId)) {
            val key = upload(path, data).key!!
            val url = createSignedUrl(path, 365.days)
            Result.success(Pair(key, url))
        }
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
