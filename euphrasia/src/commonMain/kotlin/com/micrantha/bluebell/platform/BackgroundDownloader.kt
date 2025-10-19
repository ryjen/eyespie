package com.micrantha.bluebell.platform

import kotlinx.coroutines.flow.Flow

data class DownloadData(
    val tag: String,
    val name: String,
    val fileName: String,
)

expect class BackgroundDownloader {
    fun startDownload(
        tag: String,
        name: String,
        url: String,
        checksum: String? = null
    ): String

    fun completed(): Flow<DownloadData>

}
