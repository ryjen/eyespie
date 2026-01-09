package com.micrantha.bluebell.platform

import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.bluebell.i18n.repository.LocalizedRepository
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

actual class Platform(
    actual val networkMonitor: NetworkMonitor = object : NetworkMonitor {
        override fun startMonitoring(onUpdate: (Boolean) -> Unit) = Unit
        override fun stopMonitoring() = Unit
    }
) : LocalizedRepository, com.micrantha.bluebell.platform.FileSystem {

    actual val name: String = "JVM ${System.getProperty("java.version")}"

    actual val locale: Locale = Locale()

    actual override fun format(
        epochSeconds: Long,
        format: String,
        timeZone: String
    ): String {
        val instant = Instant.ofEpochSecond(epochSeconds)
        val zoneId = ZoneId.of(timeZone)
        val date = LocalDateTime.ofInstant(instant, zoneId)
        val formatter = DateTimeFormatter.ofPattern(format, java.util.Locale.getDefault())
        return date.format(formatter)
    }

    actual override fun format(format: String, vararg args: Any): String {
        return String.format(java.util.Locale.getDefault(), format, *args)
    }

    actual fun resource(path: Path): BufferedSource {
        val inputStream = object {}.javaClass.getResourceAsStream("/$path")
            ?: throw FileNotFoundException("Resource not found: $path")
        return inputStream.source().buffer()
    }

    actual fun asset(path: Path): BufferedSource = resource(path)

    actual fun checksum(path: Path): String? {
        return runCatching {
            val source = FileSystem.SYSTEM.source(path)
            sha256(source)
        }.getOrNull()
    }

    override fun fileWrite(path: Path, data: ByteArray) {
        FileSystem.SYSTEM.sink(path).use { sink ->
            sink.buffer().use { buf ->
                buf.write(data)
                buf.flush()
            }
        }
    }

    override fun fileRead(path: Path): ByteArray {
        return FileSystem.SYSTEM.source(path).use { src ->
            src.buffer().use { buf ->
                buf.readByteArray()
            }
        }
    }

    actual override fun filesPath(): Path {
        return System.getProperty("user.home").toPath().resolve("files")
    }

    actual override fun sharedFilesPath(): Path {
        return System.getProperty("user.home").toPath().resolve("shared")
    }
}
