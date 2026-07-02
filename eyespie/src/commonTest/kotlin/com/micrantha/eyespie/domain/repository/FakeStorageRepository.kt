package com.micrantha.eyespie.domain.repository

class FakeStorageRepository : StorageRepository {
    val storage = mutableMapOf<String, ByteArray>()
    var uploadResult: Result<String>? = null

    override suspend fun download(path: String): Result<ByteArray> =
        storage[path]?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))

    override suspend fun upload(path: String, data: ByteArray): Result<String> {
        return uploadResult ?: run {
            storage[path] = data
            Result.success("http://fakeurl/$path")
        }
    }

    override fun get(path: String): Result<ByteArray> =
        storage[path]?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))
}
