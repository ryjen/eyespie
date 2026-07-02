package com.micrantha.bluebell.platform

import com.micrantha.bluebell.i18n.repository.LocalizedRepository
import okio.BufferedSource
import okio.Path

interface Platform : LocalizedRepository, FileSystem {
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

expect class PlatformImpl : Platform
