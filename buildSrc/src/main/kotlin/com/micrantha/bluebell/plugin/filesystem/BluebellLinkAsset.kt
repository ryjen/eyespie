package com.micrantha.bluebell.plugin.filesystem

import com.micrantha.bluebell.plugin.asset.BluebellAssets
import com.micrantha.bluebell.plugin.bluebell
import com.micrantha.bluebell.plugin.download.BluebellDownloads
import com.micrantha.bluebell.plugin.download.downloadBuildAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files

internal fun Project.linkBuildAssets(assets: BluebellAssets, downloads: BluebellDownloads) {

    if (assets.links.isEmpty()) {
        return
    }

    logger.bluebell("Linking assets")

    val srcDest = validateSrcDir() ?: return

    val linkDownloads = assets.links
        .filterNot { srcDest.resolve(it.name).exists() }
        .mapNotNull { downloads.findByName(it.name) }

    downloadBuildAssets(linkDownloads, srcDest)

    val links = forBuildAssets(assets.links, srcDest) { from, to ->
        linkBuildAsset(from, to)
    }

    runBlocking {
        awaitAll(*links.toTypedArray())
    }
}

internal suspend fun Project.linkBuildAsset(from: File, to: File) {
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
