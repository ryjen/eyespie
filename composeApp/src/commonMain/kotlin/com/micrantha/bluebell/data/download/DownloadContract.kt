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
data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null
)
