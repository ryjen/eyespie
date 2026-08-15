package com.micrantha.eyespie.core.data.storage

import com.micrantha.eyespie.core.data.storage.source.CacheLocalSource
import com.micrantha.eyespie.core.data.storage.source.StorageRemoteSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageDataRepositoryTest {

    private class FakeStorageRemoteSource : StorageRemoteSource {
        var downloadResult: Result<ByteArray> = Result.failure(Exception("Not found"))
        var uploadResult: Result<String> = Result.failure(Exception("Upload failed"))

        override fun url(bucketID: String, path: String) = Result.success("http://$bucketID/$path")
        override suspend fun download(bucketID: String, path: String) = downloadResult
        override suspend fun upload(bucketId: String, path: String, data: ByteArray) = uploadResult
    }

    private class FakeCacheLocalSource : CacheLocalSource {
        val cache = mutableMapOf<String, ByteArray>()
        override fun get(key: String) =
            cache[key]?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))

        override fun put(key: String, data: ByteArray) {
            cache[key] = data
        }
    }

    private val remoteSource = FakeStorageRemoteSource()
    private val localSource = FakeCacheLocalSource()
    private val repository = StorageDataRepository(remoteSource, localSource)

    @Test
    fun `download should save to localSource on success`() = runTest {
        val path = "test/path"
        val data = byteArrayOf(1, 2, 3)
        remoteSource.downloadResult = Result.success(data)

        val result = repository.download(path)

        assertTrue(result.isSuccess)
        assertEquals(data, localSource.cache[path])
    }

    @Test
    fun `upload should return durable object key from remoteSource`() = runTest {
        val path = "player/test.png"
        val data = byteArrayOf(1, 2, 3)
        remoteSource.uploadResult = Result.success(path)

        val result = repository.upload(path, data)

        assertTrue(result.isSuccess)
        assertEquals(path, result.getOrNull())
    }
}
