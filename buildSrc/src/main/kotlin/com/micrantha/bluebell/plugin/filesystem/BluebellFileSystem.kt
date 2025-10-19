package com.micrantha.bluebell.plugin.filesystem

import com.micrantha.bluebell.plugin.asset.BluebellAsset
import com.micrantha.bluebell.plugin.asset.defaultAndroidDestination
import com.micrantha.bluebell.plugin.asset.defaultAssetSource
import com.micrantha.bluebell.plugin.asset.defaultIosDestination
import com.micrantha.bluebell.plugin.asset.defaultSharedDestination
import com.micrantha.bluebell.plugin.bluebell
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.io.File

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
