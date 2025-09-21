package com.micrantha.bluebell

import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.gradle.api.Project
import java.io.File
import java.net.URI

internal val defaultSharedDestination = "src/commonMain/resources"
internal val defaultIosDestination: String = "src/iosMain/resources"
internal val defaultAndroidDestination: String = "src/androidMain/assets"

fun Project.configureAssets(assets: BluebellAssets, config: BluebellConfig) {

    val task = tasks.register("configureAssets") {
        group = "Bluebell"
        description = "Configure assets"

        copyAssets(assets)
        downloadBuildAssets(assets)
        configureRuntimeAssets(assets, config)
    }

    tasks.findByName("generateBluebellConfig")?.dependsOn(task)
}

internal fun Project.configureRuntimeAssets(assets: BluebellAssets, config: BluebellConfig) {

    val runtimeAssets = assets.runtimeDownloads()

    if (runtimeAssets.isEmpty()) return

    extensions.configure(BuildConfigExtension::class.java) {
        packageName(config.packageName)
        className(config.className)
        useKotlinOutput {
            topLevelConstants = false

            buildConfigField(type = "Int", name = "MODEL_MAX", value = runtimeAssets.size)
            runtimeAssets.forEachIndexed { index, asset ->
                buildConfigField(type = "String?", name = "MODEL_${index}_URL", value = asset.url)
                buildConfigField(type = "String?", name = "MODEL_${index}_NAME", value = asset.name)
            }
        }
    }
}

internal fun Project.downloadBuildAsset(
    fileName: String,
    url: String,
    tempDir: File
): Result<File> {
    try {
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val destination = tempDir.resolve(fileName)

        if (destination.exists()) {
            logger.lifecycle("> Download $fileName already exists, skipping")
            return Result.success(destination)
        }

        logger.lifecycle("> Downloading $fileName from $url")
        URI(url).toURL().openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return Result.success(destination)
    } catch (err: Throwable) {
        logger.error("Unable to download $fileName from $url", err)
        return Result.failure(err)
    }
}

internal fun Project.downloadBuildAssets(assets: BluebellAssets) {

    val tempDir by lazy { layout.buildDirectory.dir("tmp").get().asFile }

    val tempAssets = assets.bundledDownloads()
        .fold(mutableListOf<BluebellDownload<File>>()) { results, file ->

            file.url?.let { url ->
                downloadBuildAsset(file.name, url, tempDir).map {
                    BluebellDownload(
                        ios = it,
                        android = it,
                        name = file.name
                    )
                }.onSuccess(results::add)

                return@fold results
            }

            file.iosUrl?.let { url ->
                downloadBuildAsset(file.name, url, tempDir).map {
                    BluebellDownload(
                        ios = it,
                        android = null,
                        name = file.name,
                    )
                }.onSuccess(results::add)
            }

            file.androidUrl?.let { url ->
                downloadBuildAsset(file.name, url, tempDir).map {
                    BluebellDownload(
                        ios = null,
                        android = it,
                        name = file.name,
                    )
                }.onSuccess(results::add)
            }

            results
        }

    for (file in tempAssets) {
        val iosOutput by lazy {
            projectDir.resolve(defaultIosDestination).resolve(file.name)
        }
        val androidOutput by lazy {
            projectDir.resolve(defaultAndroidDestination).resolve(file.name)
        }

        file.ios?.copyTo(iosOutput, overwrite = true)
        file.android?.copyTo(androidOutput, overwrite = true)

        if (file.ios != null && file.android == null) {
            logger.lifecycle("> ${file.name} added to ios resources")
        } else if (file.android != null && file.ios == null) {
            logger.lifecycle("> ${file.name} added to android resources")
        } else {
            logger.lifecycle("> ${file.name} added to ios and android resources")
        }
    }
}

internal fun Project.copyAssets(assets: BluebellAssets) {

    for (file in assets.copies) {
        val from = File(projectDir.path, file.source)

        if (from.exists().not()) {
            logger.warn("Asset ${from.path} does not exist, skipping")
            continue
        }
        val to = File(projectDir.path, file.destination)

        from.copyTo(to, true)
    }
}
