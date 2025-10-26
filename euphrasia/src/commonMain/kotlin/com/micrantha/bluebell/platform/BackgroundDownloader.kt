package com.micrantha.bluebell.platform

import com.micrantha.bluebell.data.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import okio.Path

expect class BackgroundDownloader {
    suspend fun startDownload(
        id: Long,
        name: String?,
        url: String,
        filePath: Path,
        tag: String? = null,
    ): Result<Unit>

    fun observe(): Flow<DownloadState>
}

fun Flow<DownloadState>.filterById(id: Long): Flow<DownloadState> = filter {
    it.id == id
}
