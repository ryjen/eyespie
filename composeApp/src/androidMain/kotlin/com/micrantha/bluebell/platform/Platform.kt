package com.micrantha.bluebell.platform

import android.content.Context
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import okio.BufferedSource
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.micrantha.bluebell.platform.FileSystem as BluebellFileSystem
import com.micrantha.bluebell.platform.Locale as BluebellLocale


actual class Platform(
    private val context: Context,
    actual val networkMonitor: NetworkMonitor,
) : LocalizedRepository, BluebellFileSystem {

    actual val locale by lazy { BluebellLocale(context) }

    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"

    actual override fun format(
        epochSeconds: Long,
        format: String,
        timeZone: String
    ): String {
        val instant = Instant.ofEpochSecond(epochSeconds)
        val zoneId = ZoneId.of(timeZone)
        val date = LocalDateTime.ofInstant(instant, zoneId)
        val formatter =
            DateTimeFormatter.ofPattern(format, locale.systemLocale)
        return date.format(formatter)
    }

    actual override fun format(format: String, vararg args: Any): String {
        return String.format(locale.systemLocale, format, *args)
    }

    actual fun resource(path: Path): BufferedSource {
        val inputStream = object {}.javaClass.getResourceAsStream("/$path")
            ?: throw FileNotFoundException("Resource not found: $path")
        return inputStream.source().buffer()
    }

    actual override fun filesPath(): Path {
        return context.applicationContext.filesDir.absolutePath.toPath()
    }

    actual override fun modelsPath(): Path {
        val path = context.applicationContext.cacheDir.resolve("models")
        if (!path.mkdir()) {
            if (!path.exists() || !path.isDirectory) {
                throw IllegalStateException("Failed to create models directory")
            }
        }
        return path.absolutePath.toPath()
    }
}
