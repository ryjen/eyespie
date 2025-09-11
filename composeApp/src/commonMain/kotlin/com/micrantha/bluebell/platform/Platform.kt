package com.micrantha.bluebell.platform

import com.micrantha.bluebell.domain.repository.LocalizedRepository
import okio.Path

expect class Platform : LocalizedRepository, FileSystem {
    val name: String

    val networkMonitor: NetworkMonitor

    val downloader: BackgroundDownloader

    override fun format(
        epochSeconds: Long,
        format: String,
        timeZone: String,
        locale: String
    ): String

    fun filePath(fileName: String): Path
}
