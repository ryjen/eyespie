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

internal fun Project.copyBuildAssets(assets: BluebellAssets, downloads: BluebellDownloads) {

    if (assets.copies.isEmpty()) {
        return
    }
    logger.bluebell("Copying assets")

    val srcDest = validateSrcDir() ?: return

    val downloads = assets.copies
        .filterNot { srcDest.resolve(it.name).exists() }
        .mapNotNull { downloads.findByName(it.name) }

    downloadBuildAssets(downloads, srcDest)

    val copies = forBuildAssets(assets.copies, srcDest) { from, to ->
        copyBuildAsset(from, to)
    }

    runBlocking {
        awaitAll(*copies.toTypedArray())
    }
}

internal suspend fun Project.copyBuildAsset(from: File, to: File) {
    if (from.exists().not()) {
        logger.bluebell("Asset to copy ${from.path} does not exist, skipping", logger::warn)
        return
    }

    if (to.validateParentDir().not()) {
        logger.bluebell("Asset to copy ${to.path} has invalid directory", logger::error)
        return
    }

    logger.bluebell("Copying ${to.path}", logger::debug)

    withContext(Dispatchers.IO) {
        from.copyTo(to, true)
    }
}
