package com.micrantha.bluebell.plugin.download

import com.micrantha.bluebell.plugin.bluebell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.io.File
import java.net.URI
import java.net.URL

typealias BluebellDownloads = NamedDomainObjectContainer<BluebellDownload>

internal suspend fun downloadBuildAsset(
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
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

    val uri = buildAssetUrl(url, propertyResolver)

    withContext(Dispatchers.IO) {
        uri.openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

internal suspend fun Project.downloadBuildAsset(
    fileName: String,
    url: String,
    tempDir: File
) = downloadBuildAsset(logger, ::findProperty, fileName, url, tempDir)

internal fun downloadBuildAssets(
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    downloads: List<BluebellDownload>,
    destDir: File
): Unit =
    runBlocking {
        awaitAll(*downloads.map { file ->
            async { downloadBuildAsset(logger, propertyResolver, file.name, file.url, destDir) }
        }.toTypedArray())
    }

internal fun Project.downloadBuildAssets(downloads: List<BluebellDownload>, destDir: File) =
    downloadBuildAssets(logger, ::findProperty, downloads, destDir)

internal fun buildAssetUrl(url: String, propertyResolver: (String) -> Any?): URL {

    val uri = URI(url)

    val (userKey, passKey) = uri.userInfo.split(":", limit = 2)

    val user = System.getenv(userKey) ?: propertyResolver(userKey) ?: userKey
    val pass = System.getenv(passKey) ?: propertyResolver(passKey) ?: passKey

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

internal fun Project.buildAssetUrl(url: String): URL = buildAssetUrl(url, ::findProperty)
