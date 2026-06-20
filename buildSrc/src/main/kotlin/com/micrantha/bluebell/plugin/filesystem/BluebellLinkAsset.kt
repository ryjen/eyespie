package com.micrantha.bluebell.plugin.filesystem

import com.micrantha.bluebell.plugin.asset.BluebellAsset
import com.micrantha.bluebell.plugin.asset.BluebellAssets
import com.micrantha.bluebell.plugin.bluebell
import com.micrantha.bluebell.plugin.download.BluebellDownload
import com.micrantha.bluebell.plugin.download.BluebellDownloads
import com.micrantha.bluebell.plugin.download.downloadBuildAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files

internal fun linkBuildAssets(
    projectDir: File,
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    manifest: String?,
    links: List<BluebellAsset>,
    downloads: List<BluebellDownload>
) {

    if (links.isEmpty()) {
        return
    }

    logger.bluebell("Linking assets")

    val srcDest = validateSrcDir(projectDir, logger) ?: return

    val linkDownloads = links
        .filterNot { srcDest.resolve(it.name).exists() }
        .mapNotNull { name -> downloads.find { it.name == name.name } }

    downloadBuildAssets(logger, propertyResolver, linkDownloads, srcDest)

    val runLinks = forBuildAssets(projectDir, logger, links, srcDest) { from, to ->
        linkBuildAsset(logger, from, to)
    }

    runBlocking {
        awaitAll(*runLinks.toTypedArray())
    }
}

internal fun Project.linkBuildAssets(assets: BluebellAssets, downloads: BluebellDownloads) =
    linkBuildAssets(projectDir, logger, ::findProperty, assets.manifest, assets.links.toList(), downloads.toList())

internal suspend fun linkBuildAsset(logger: org.gradle.api.logging.Logger, from: File, to: File) {
    if (from.exists().not()) {
        logger.bluebell("Asset to link ${from.path} does not exist, skipping", logger::warn)
        return
    }

    if (to.validateParentDir().not()) {
        logger.bluebell("Asset to link ${to.path} has invalid directory", logger::error)
        return
    }

    logger.bluebell("Linking ${to.path}", logger::debug)

    withContext(Dispatchers.IO) {
        Files.deleteIfExists(to.toPath())
        Files.createSymbolicLink(to.toPath(), from.toPath())
    }
}

internal suspend fun Project.linkBuildAsset(from: File, to: File) = linkBuildAsset(logger, from, to)
