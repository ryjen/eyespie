package com.micrantha.bluebell.plugin.config

import com.micrantha.bluebell.plugin.asset.BluebellAsset
import com.micrantha.bluebell.plugin.asset.BluebellAssetConfig
import com.micrantha.bluebell.plugin.asset.BluebellModels
import com.micrantha.bluebell.plugin.asset.defaultAndroidDestination
import com.micrantha.bluebell.plugin.asset.defaultIosDestination
import com.micrantha.bluebell.plugin.asset.defaultSharedDestination
import com.micrantha.bluebell.plugin.bluebell
import com.micrantha.bluebell.plugin.download.BluebellDownload
import com.micrantha.bluebell.plugin.download.BluebellDownloads
import com.micrantha.bluebell.plugin.download.buildAssetUrl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.Project
import java.io.File
import java.util.Base64
import kotlin.reflect.KClass

internal fun <T : BluebellDownload> coreMapDownloadsToConfig(
    propertyResolver: (String) -> Any?,
    type: KClass<T>,
    downloads: List<BluebellDownload>
) = downloads
    .filterNot { type.isInstance(it) }.associate {
        it.name to BluebellAssetConfig.Download(
            url = buildAssetUrl(it.url, propertyResolver).toString(),
            checksum = it.checksum
        )
    }

internal fun <T : BluebellDownload> Project.mapDownloadsToConfig(
    type: KClass<T>,
    downloads: BluebellDownloads
) = coreMapDownloadsToConfig(::findProperty, type, downloads.toList())

internal fun <T : BluebellAsset> coreMapModelsToConfig(
    type: KClass<T>,
    models: List<BluebellAsset>
) =
    models.filterNot { type.isInstance(it) }.associate { asset ->
        asset.name to BluebellAssetConfig.Model(
            asset.filename
        )
    }

internal fun <T : BluebellAsset> Project.mapModelsToConfig(
    type: KClass<T>,
    models: BluebellModels
) = coreMapModelsToConfig(type, models.toList())

internal fun coreGenerateIosConfig(
    projectDir: File,
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    manifest: String,
    models: List<BluebellAsset>,
    downloads: List<BluebellDownload>
) {

    val destFile = projectDir.resolve(defaultIosDestination).resolve(manifest)
    val iosDownloads = coreMapDownloadsToConfig(propertyResolver, BluebellDownload.IosDownload::class, downloads)
    val configModels = coreMapModelsToConfig(BluebellAsset.IosAsset::class, models)

    coreGenerateAssetConfig(logger, manifest, destFile, iosDownloads, configModels)
}

internal fun Project.generateIosConfig(
    manifest: String,
    models: BluebellModels,
    downloads: BluebellDownloads
) = coreGenerateIosConfig(projectDir, logger, ::findProperty, manifest, models.toList(), downloads.toList())


internal fun coreGenerateAndroidConfig(
    projectDir: File,
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    manifest: String,
    models: List<BluebellAsset>,
    downloads: List<BluebellDownload>
) {

    val destFile = projectDir.resolve(defaultAndroidDestination).resolve(manifest)

    val androidDownloads = coreMapDownloadsToConfig(propertyResolver, BluebellDownload.AndroidDownload::class, downloads)

    val configModels = coreMapModelsToConfig(BluebellAsset.AndroidAsset::class, models)

    coreGenerateAssetConfig(logger, manifest, destFile, androidDownloads, configModels)
}

internal fun Project.generateAndroidConfig(
    manifest: String,
    models: BluebellModels,
    downloads: BluebellDownloads
) = coreGenerateAndroidConfig(projectDir, logger, ::findProperty, manifest, models.toList(), downloads.toList())


internal fun coreGenerateSharedConfig(
    projectDir: File,
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    manifest: String,
    models: List<BluebellAsset>,
    downloads: List<BluebellDownload>
) {

    val destFile = projectDir.resolve(defaultSharedDestination).resolve(manifest)

    val sharedDownloads = coreMapDownloadsToConfig(propertyResolver, BluebellDownload.DefaultDownload::class, downloads)

    val configModels = coreMapModelsToConfig(BluebellAsset.SharedAsset::class, models)

    coreGenerateAssetConfig(logger, manifest, destFile, sharedDownloads, configModels)
}

internal fun Project.generateSharedConfig(
    manifest: String,
    models: BluebellModels,
    downloads: BluebellDownloads
) = coreGenerateSharedConfig(projectDir, logger, ::findProperty, manifest, models.toList(), downloads.toList())

@OptIn(ExperimentalSerializationApi::class)
internal fun coreGenerateAssetConfig(
    logger: org.gradle.api.logging.Logger,
    manifest: String,
    destFile: File,
    downloads: Map<String, BluebellAssetConfig.Download>,
    models: Map<String, BluebellAssetConfig.Model>
) {

    if (manifest.isBlank()) return

    val config = BluebellAssetConfig(downloads, models)

    val json = Json {
        prettyPrint = true
    }

    Base64.getEncoder().wrap(destFile.outputStream()).use {
        json.encodeToStream(config, it)
    }

    logger.bluebell("Generated asset manifest")
}

@OptIn(ExperimentalSerializationApi::class)
internal fun Project.generateAssetConfig(
    manifest: String,
    destFile: File,
    downloads: Map<String, BluebellAssetConfig.Download>,
    models: Map<String, BluebellAssetConfig.Model>
) = coreGenerateAssetConfig(logger, manifest, destFile, downloads, models)
