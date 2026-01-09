package com.micrantha.bluebell.platform

import com.micrantha.bluebell.data.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okio.Path

actual class BackgroundDownloader {
    actual suspend fun startDownload(
        id: Long,
        name: String?,
        url: String,
        filePath: Path,
        tag: String?
    ): Result<Unit> = Result.success(Unit)

    actual fun observe(): Flow<DownloadState> = emptyFlow()
}
