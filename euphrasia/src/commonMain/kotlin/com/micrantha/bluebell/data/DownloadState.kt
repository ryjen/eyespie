package com.micrantha.bluebell.data

/**
 * Sealed class representing download states
 */
sealed interface DownloadState {
    val id: Long

    data class Queued(override val id: Long) : DownloadState
    data class Started(override val id: Long) : DownloadState
    data class Progress(
        override val id: Long,
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val etaInMillis: Long,
        val downloadedBytesPerSecond: Long
    ) : DownloadState
    data class Completed(override val id: Long) : DownloadState
    data class Paused(override val id: Long) : DownloadState
    data class Cancelled(override val id: Long) : DownloadState
    data class Resumed(override val id: Long) : DownloadState
    data class Added(override val id: Long) : DownloadState
    data class Removed(override val id: Long) : DownloadState
    data class Deleted(override val id: Long) : DownloadState
    data class Failed(
        override val id: Long,
        val error: Error, val throwable: Throwable?) : DownloadState
    data class WaitingNetwork(override val id: Long) : DownloadState
}
