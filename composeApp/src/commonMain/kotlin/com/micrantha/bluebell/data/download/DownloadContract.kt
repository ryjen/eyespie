package com.micrantha.bluebell.data.download

import kotlin.uuid.ExperimentalUuidApi

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@OptIn(ExperimentalUuidApi::class)
data class DownloadTask<ID>(
    val id: ID,
    val url: String,
    val fileName: String,
    val checksum: String? = null,
    val filePath: String? = null,
    val status: DownloadStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null
)
