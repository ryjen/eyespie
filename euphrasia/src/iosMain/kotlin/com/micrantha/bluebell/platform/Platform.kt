package com.micrantha.bluebell.platform

import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.eyespie.AppDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import okio.Buffer
import okio.BufferedSource
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSTimeZone
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.getBytes
import platform.Foundation.stringWithFormat
import platform.Foundation.timeZoneWithName
import platform.UIKit.UIDevice
import com.micrantha.bluebell.platform.FileSystem as BluebellFileSystem


actual class Platform(
    app: AppDelegate,
) : LocalizedRepository, BluebellFileSystem {

    actual val networkMonitor = app.networkMonitor

    actual val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    actual override fun format(
        epochSeconds: Long,
        format: String,
        timeZone: String
    ): String {
        val date = NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())
        val dateFormatter = NSDateFormatter()
        dateFormatter.timeZone = NSTimeZone.timeZoneWithName(timeZone)!!
        dateFormatter.locale = locale.systemLocale
        dateFormatter.dateFormat = format
        return dateFormatter.stringFromDate(date)
    }

    actual override fun format(format: String, vararg args: Any): String {
        return NSString.stringWithFormat(format, *args)
    }

    actual val locale by lazy { Locale() }

    actual fun resource(path: Path): BufferedSource {
        val bundle = NSBundle.mainBundle
        val url = bundle.URLForResource(
            name = path.name.substringBeforeLast("."),
            withExtension = path.name.substringAfterLast(".")
        ) ?: error("Resource not found: $path")

        val data = NSData.dataWithContentsOfURL(url)
            ?: error("Failed to read resource: $path")

        return Buffer().write(data.toByteArray())
    }

    actual override fun filesPath(): Path {
        return NSHomeDirectory().toPath().resolve("files")
    }

    actual override fun modelsPath(): Path {
        return NSHomeDirectory().toPath().resolve("models")
    }
}

// Extension to convert NSData to ByteArray
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(this.length.toInt())
    result.usePinned { pinned ->
        this.getBytes(pinned.addressOf(0), this.length)
    }
    return result
}
