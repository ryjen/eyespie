package com.micrantha.bluebell.platform

import com.micrantha.bluebell.domain.repository.LocalizedRepository
import okio.Path

expect class Platform : LocalizedRepository, FileSystem {
    val name: String

    val networkMonitor: NetworkMonitor

    override fun format(
        epochSeconds: Long,
        format: String,
        timeZone: String,
    ): String

    override fun format(format: String, vararg args: Any): String

    fun filePath(fileName: String): Path
}
