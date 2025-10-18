package com.micrantha.bluebell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File
import java.nio.file.Files

internal const val defaultAssetSource = "bluebellAssets"
internal const val defaultSharedDestination = "src/commonMain/resources"
internal const val defaultIosDestination: String = "src/iosMain/resources"
internal const val defaultAndroidDestination: String = "src/androidMain/assets"

internal fun Project.configureAssets(assets: BluebellAssets) {

    val task = tasks.register("configureBluebellAssets") {
        group = "Bluebell"
        description = "Configure assets"

        copyBuildAssets(assets)
        linkBuildAssets(assets)
        generateAssetConfig(assets)
    }

    val generate = tasks.register("generateBluebellManifestSupport", Copy::class.java) {

        val packageParts = BluebellAssetConfig::class.java.`package`.name
            .split(".", File.separator)

        val srcDir = projectDir.resolve("../buildSrc/src/main/kotlin")

        val srcFile = packageParts.fold(srcDir) { dest, dir ->
            dest.resolve(dir)
        }.resolve(BluebellAssetConfig::class.java.simpleName + ".kt")

        val targetDir = projectDir
            .resolve("src")
            .resolve("commonMain")
            .resolve("kotlin")

        logger.bluebell("Generated asset manifest support")

        if (srcFile.exists().not()) {
            logger.bluebell("${srcFile.path} does not exist", logger::error)
            return@register
        }

        if (targetDir.exists().not() || targetDir.isDirectory.not()) {
            logger.bluebell("${targetDir.path} is not a directory", logger::error)
            return@register
        }

        from(srcFile)
        into(targetDir)
    }

    tasks.findByName("generateBluebellConfig")?.dependsOn(task, generate)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun Project.generateAssetConfig(assets: BluebellAssets) {

    if (assets.manifest.isNullOrBlank()) return

    val destFile = projectDir.resolve(defaultSharedDestination).resolve(assets.manifest!!)

    val config = BluebellAssetConfig(
        assets.downloads.associate {
            it.name to when (it) {
                is BluebellDownload.IosDownload -> BluebellAssetConfig.Download(
                    iosUrl = it.url,
                    checksum = it.checksum
                )

                is BluebellDownload.AndroidDownload -> BluebellAssetConfig.Download(
                    androidUrl = it.url,
                    checksum = it.checksum
                )

                is BluebellDownload.DefaultDownload -> BluebellAssetConfig.Download(
                    url = it.url,
                    checksum = it.checksum
                )
            }
        }
    )
    val json = Json {
        prettyPrint = true
    }

    destFile.outputStream().use {
        json.encodeToStream(config, it)
    }

    logger.bluebell("Generated asset manifest file ${assets.manifest}")
}

private fun BluebellAsset.destination(baseDir: File): List<File> = when (this) {
    is BluebellAsset.IosAsset -> listOf(baseDir.resolve(defaultIosDestination))
    is BluebellAsset.AndroidAsset -> listOf(baseDir.resolve(defaultAndroidDestination))
    is BluebellAsset.SharedAsset -> listOf(baseDir.resolve(defaultSharedDestination))
    is BluebellAsset.DefaultAsset -> listOf(
        baseDir.resolve(defaultAndroidDestination),
        baseDir.resolve(defaultIosDestination)
    )
}

internal fun Project.forBuildAssets(
    assets: NamedDomainObjectContainer<BluebellAsset>,
    srcDest: File,
    action: suspend (File, File) -> Unit
) = runBlocking {
    assets.flatMap { file ->

        val srcFile = srcDest.resolve(file.name)

        if (srcFile.exists().not()) {
            logger.bluebell("No asset found for asset ${file.name}", logger::error)
            return@flatMap emptyList()
        }

        file.destination(projectDir).map {
            async { action(srcFile, it.resolve(file.name)) }
        }
    }
}

internal fun Project.linkBuildAssets(assets: BluebellAssets) {

    if (assets.links.isEmpty()) {
        return
    }

    logger.bluebell("Linking assets")

    val srcDest = validateSrcDir() ?: return

    val downloads = assets.links
        .filterNot { srcDest.resolve(it.name).exists() }
        .mapNotNull { assets.downloads.findByName(it.name) }

    downloadBuildAssets(downloads, srcDest)

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

internal fun Project.copyBuildAssets(assets: BluebellAssets) {

    if (assets.copies.isEmpty()) {
        return
    }
    logger.bluebell("Copying assets")

    val srcDest = validateSrcDir() ?: return

    val downloads = assets.copies
        .filterNot { srcDest.resolve(it.name).exists() }
        .mapNotNull { assets.downloads.findByName(it.name) }

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

internal fun File.validateParentDir(): Boolean {

    if (parentFile.exists()) {
        return !parentFile.isDirectory.not()
    } else {
        parentFile.mkdirs()
        return true
    }
}

internal fun Project.validateSrcDir(): File? {

    val srcDest = projectDir.resolve(defaultAssetSource)

    if (!srcDest.exists()) {
        logger.bluebell(
            "No build assets dir, download the assets you need and put them in ${srcDest.path}",
            logger::error
        )
        return null
    }

    return srcDest
}
