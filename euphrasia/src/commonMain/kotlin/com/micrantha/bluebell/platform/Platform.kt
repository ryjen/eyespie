package com.micrantha.bluebell.platform

import com.micrantha.bluebell.domain.repository.LocalizedRepository
import okio.BufferedSource
import okio.Path

expect class Platform : LocalizedRepository, FileSystem {
    val name: String

    val networkMonitor: NetworkMonitor

    override fun format(
        epochSeconds: Long,
        format: String,
        timeZone: String,
    ): String

    val locale: Locale

    fun asset(path: Path): BufferedSource

    fun checksum(path: Path): String?

    fun resource(path: Path): BufferedSource

    override fun format(format: String, vararg args: Any): String

    override fun filesPath(): Path

    override fun sharedFilesPath(): Path
}
