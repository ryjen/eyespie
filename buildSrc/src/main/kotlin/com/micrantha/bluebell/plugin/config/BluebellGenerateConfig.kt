package com.micrantha.bluebell.plugin.asset

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

internal fun <T : BluebellDownload> Project.mapDownloadsToConfig(
    type: KClass<T>,
    downloads: BluebellDownloads
) = downloads
    .filterNot { type.isInstance(it) }.associate {
        it.name to BluebellAssetConfig.Download(
            url = buildAssetUrl(it.url).toString(),
            checksum = it.checksum
        )
    }

internal fun <T : BluebellAsset> Project.mapModelsToConfig(
    type: KClass<T>,
    models: BluebellModels
) =
    models.filterNot { type.isInstance(it) }.associate { asset ->
        asset.name to BluebellAssetConfig.Model(
            asset.filename
        )
    }

internal fun Project.generateIosConfig(
    manifest: String,
    models: BluebellModels,
    downloads: BluebellDownloads
) {

    val destFile = projectDir.resolve(defaultIosDestination).resolve(manifest)

    val iosDownloads = mapDownloadsToConfig(BluebellDownload.IosDownload::class, downloads)

    val models = mapModelsToConfig(BluebellAsset.IosAsset::class, models)

    generateAssetConfig(manifest, destFile, iosDownloads, models)
}


internal fun Project.generateAndroidConfig(
    manifest: String,
    models: BluebellModels,
    downloads: BluebellDownloads
) {

    val destFile = projectDir.resolve(defaultAndroidDestination).resolve(manifest)

    val androidDownloads = mapDownloadsToConfig(BluebellDownload.AndroidDownload::class, downloads)

    val models = mapModelsToConfig(BluebellAsset.AndroidAsset::class, models)

    generateAssetConfig(manifest, destFile, androidDownloads, models)
}


internal fun Project.generateSharedConfig(
    manifest: String,
    models: BluebellModels,
    downloads: BluebellDownloads
) {

    val destFile = projectDir.resolve(defaultSharedDestination).resolve(manifest)

    val sharedDownloads = mapDownloadsToConfig(BluebellDownload.DefaultDownload::class, downloads)

    val models = mapModelsToConfig(BluebellAsset.SharedAsset::class, models)

    generateAssetConfig(manifest, destFile, sharedDownloads, models)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun Project.generateAssetConfig(
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
