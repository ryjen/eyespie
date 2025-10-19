package com.micrantha.bluebell.plugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.io.File
import java.net.URI
import java.net.URL
import javax.inject.Inject

sealed class BluebellDownload(val name: String, val url: String) {
    var checksum: String? = null

    open class IosDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)

    open class AndroidDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)

    open class DefaultDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)
}

class BluebellDownloadBuilder(
    private val container: NamedDomainObjectContainer<BluebellDownload>,
    private val objectFactory: ObjectFactory
) {
    var url: String? = null
    var checksum: String? = null

    fun ios(name: String, configure: BluebellDownloadBuilder.() -> Unit) {
        configure.invoke(this)
        val download =
            objectFactory.newInstance(BluebellDownload.IosDownload::class.java, name, url).also {
                it.checksum = checksum
            }
        container.add(download)
    }

    fun android(name: String, configure: BluebellDownloadBuilder.() -> Unit) {
        configure.invoke(this)
        val download =
            objectFactory.newInstance(BluebellDownload.AndroidDownload::class.java, name, url)
                .also {
                    it.checksum = checksum
                }
        container.add(download)
    }

    fun create(name: String, configure: BluebellDownloadBuilder.() -> Unit) {
        configure(this)
        val download =
            objectFactory.newInstance(BluebellDownload.DefaultDownload::class.java, name, url)
                .also {
                    it.checksum = this.checksum
                }
        container.add(download)
    }
}

internal suspend fun Project.downloadBuildAsset(
    fileName: String,
    url: String,
    tempDir: File
) {
    if (!tempDir.exists()) {
        tempDir.mkdirs()
    }
    val destination = tempDir.resolve(fileName)

    if (destination.exists()) {
        logger.bluebell("Download $fileName already exists, skipping")
        return
    }

    logger.bluebell("Downloading $fileName from $url")

    val uri = buildAssetUrl(url)

    withContext(Dispatchers.IO) {
        uri.openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

internal fun Project.downloadBuildAssets(downloads: List<BluebellDownload>, destDir: File): Unit =
    runBlocking {
        awaitAll(*downloads.map { file ->
            async { downloadBuildAsset(file.name, file.url, destDir) }
        }.toTypedArray())
    }

internal fun Project.buildAssetUrl(url: String): URL {

    val uri = URI(url)

    val (userKey, passKey) = uri.userInfo.split(":", limit = 2)

    val user = System.getenv(userKey) ?: findProperty(userKey) ?: userKey
    val pass = System.getenv(passKey) ?: findProperty(passKey) ?: passKey

    return URI(
        uri.scheme,
        "$user:$pass",
        uri.host,
        uri.port,
        uri.path,
        uri.query,
        uri.fragment
    ).toURL()
}
