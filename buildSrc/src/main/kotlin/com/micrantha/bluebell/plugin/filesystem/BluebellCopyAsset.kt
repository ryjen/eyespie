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

internal fun copyBuildAssets(
    projectDir: File,
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    manifest: String?,
    copies: List<BluebellAsset>,
    downloads: List<BluebellDownload>
) {

    if (copies.isEmpty()) {
        return
    }
    logger.bluebell("Copying assets")

    val srcDest = validateSrcDir(projectDir, logger) ?: return

    val assetDownloads = copies
        .filterNot { srcDest.resolve(it.name).exists() }
        .mapNotNull { name -> downloads.find { it.name == name.name } }

    downloadBuildAssets(logger, propertyResolver, assetDownloads, srcDest)

    val runCopies = forBuildAssets(projectDir, logger, copies, srcDest) { from, to ->
        copyBuildAsset(logger, from, to)
    }

    runBlocking {
        awaitAll(*runCopies.toTypedArray())
    }
}

internal fun Project.copyBuildAssets(assets: BluebellAssets, downloads: BluebellDownloads) =
    copyBuildAssets(projectDir, logger, ::findProperty, assets.manifest, assets.copies.toList(), downloads.toList())

internal suspend fun copyBuildAsset(logger: org.gradle.api.logging.Logger, from: File, to: File) {
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

internal suspend fun Project.copyBuildAsset(from: File, to: File) = copyBuildAsset(logger, from, to)
