package com.micrantha.bluebell.plugin.asset

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.micrantha.bluebell.plugin.config.coreGenerateAndroidConfig
import com.micrantha.bluebell.plugin.config.coreGenerateIosConfig
import com.micrantha.bluebell.plugin.config.coreGenerateSharedConfig
import com.micrantha.bluebell.plugin.download.BluebellDownload
import com.micrantha.bluebell.plugin.download.BluebellDownloads
import com.micrantha.bluebell.plugin.filesystem.copyBuildAssets
import com.micrantha.bluebell.plugin.filesystem.destination
import com.micrantha.bluebell.plugin.filesystem.linkBuildAssets
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject


/**
 * TODO:
 *
 * Assets at build time make for on-demand asset packs from app stores
 * The goal is to have AI models in asset packs
 * or as separate sister apps that deeplink for offline-inference
 * but for now - focusing on remote-config after auth
 *
 * Build Asset:
 *   - Download:
 *      - android url
 *      - checksums
 *      - ios url
 *      - common url
 *      - authentication
 *      - background/resume
 *      - saves to prebuild folder
 *   - Copy From:
 *      - uses prebuild folder
 *      - copies a file to app resources
 *   - Link From:
 *       - uses prebuild folder
 *       - uses symlinks to app resources
 *   - Build Time Storage:
 *      - ios resources
 *      - android assets
 *      - shared resources
 *      - can be any location main app or module
 *   - name / slug
 *   - fileName
 */

typealias BluebellModels = NamedDomainObjectContainer<BluebellAsset>

internal const val defaultAssetSource = "bluebellAssets"
internal const val defaultSharedDestination = "src/commonMain/resources"
internal const val defaultIosDestination: String = "src/iosMain/resources"
internal const val defaultAndroidDestination: String = "src/androidMain/assets"

abstract class ConfigureBluebellAssetsTask : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val manifest: Property<String>

    @get:Nested
    abstract val models: ListProperty<BluebellAsset>

    @get:Nested
    abstract val copies: ListProperty<BluebellAsset>

    @get:Nested
    abstract val links: ListProperty<BluebellAsset>

    @get:Nested
    abstract val downloads: ListProperty<BluebellDownload>

    @get:OutputFiles
    abstract val outputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val providers: ProviderFactory

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @TaskAction
    fun configure() {
        val manifestVal = manifest.getOrNull()
        val copiesVal = copies.get()
        val linksVal = links.get()
        val modelsVal = models.get()
        val downloadsVal = downloads.get()

        val propertyResolver: (String) -> Any? = { key -> providers.gradleProperty(key).getOrNull() }

        val projectDir = projectLayout.projectDirectory.asFile

        copyBuildAssets(projectDir, logger, propertyResolver, manifestVal, copiesVal, downloadsVal)
        linkBuildAssets(projectDir, logger, propertyResolver, manifestVal, linksVal, downloadsVal)

        generateAssetConfigs(projectDir, logger, propertyResolver, manifestVal, modelsVal, downloadsVal)
    }
<<<<<<< Updated upstream
||||||| Stash base

    private fun generateModelManifest(projectDir: File, model: BluebellAsset.ModelAsset) {
        val dest = model.destination(projectDir).firstOrNull() ?: return
        val fileName = model.filename ?: model.name
        val modelFile = dest.resolve(fileName)
        if (!modelFile.exists()) return

        val manifestFile = dest.resolve("manifest.json")
        val sha256 = modelFile.sha256()
        val size = modelFile.length()
        val version = "${model.name}-local"

        val manifestContent = """
            {
              "schemaVersion": 1,
              "modelId": "eyespie-offline-model",
              "version": "$version",
              "filename": "$fileName",
              "sizeBytes": $size,
              "sha256": "$sha256",
              "runtime": {
                "engine": "mediapipe",
                "minimumRuntimeVersion": "0.10.35",
                "minimumModelAbi": 1
              }
            }
        """.trimIndent()
        manifestFile.writeText(manifestContent)

        updateKotlinDescriptor(projectDir, version, fileName, size, sha256)
    }

    private fun updateKotlinDescriptor(
        projectDir: File,
        version: String,
        filename: String,
        size: Long,
        sha256: String
    ) {
        val moduleFile = projectDir.resolve("src/androidMain/kotlin/com/micrantha/eyespie/model/Module.android.kt")
        if (!moduleFile.exists()) return

        val content = moduleFile.readText()
        val pattern = Regex("""internal val androidSmokeModelDescriptor = ModelAssetDescriptor\(.*?\n\)""", RegexOption.DOT_MATCHES_ALL)
        
        val replacement = """
            internal val androidSmokeModelDescriptor = ModelAssetDescriptor(
                id = "eyespie-offline-model",
                version = "$version",
                filename = "$filename",
                expectedBytes = ${size}L,
                sha256 = "$sha256",
                runtime = ModelRuntimeCompatibility(
                    engine = "mediapipe",
                    minimumRuntimeVersion = "0.10.35",
                    minimumModelAbi = 1,
                ),
            )
        """.trimIndent().trim()

        val updated = content.replace(pattern, replacement)
        if (updated != content) {
            moduleFile.writeText(updated)
        }
    }
=======

    private fun generateModelManifest(projectDir: File, model: BluebellAsset.ModelAsset) {
        val dest = model.destination(projectDir).firstOrNull() ?: return
        val fileName = model.filename ?: model.name
        val modelFile = dest.resolve(fileName)
        if (!modelFile.exists()) return

        val manifestFile = dest.resolve("manifest.json")
        val sha256 = modelFile.sha256()
        val size = modelFile.length()
        val version = "${model.name}-local"

        val manifestContent = """
            {
              "schemaVersion": 1,
              "modelId": "eyespie-offline-model",
              "version": "$version",
              "filename": "$fileName",
              "sizeBytes": $size,
              "sha256": "$sha256",
              "runtime": {
                "engine": "mediapipe",
                "minimumRuntimeVersion": "0.10.35",
                "minimumModelAbi": 1
              }
            }
        """.trimIndent()
        manifestFile.writeText(manifestContent)

        updateKotlinDescriptors(projectDir, version, fileName, size, sha256)
    }

    private fun updateKotlinDescriptors(
        projectDir: File,
        version: String,
        filename: String,
        size: Long,
        sha256: String
    ) {
        updateAndroidDescriptor(projectDir, version, filename, size, sha256)
        updateIosDescriptor(projectDir, version, filename, size, sha256)
    }

    private fun updateAndroidDescriptor(
        projectDir: File,
        version: String,
        filename: String,
        size: Long,
        sha256: String
    ) {
        val moduleFile = projectDir.resolve("src/androidMain/kotlin/com/micrantha/eyespie/model/Module.android.kt")
        if (!moduleFile.exists()) return

        val content = moduleFile.readText()
        val pattern = Regex("""internal val androidSmokeModelDescriptor = ModelAssetDescriptor\(.*?\n\)""", RegexOption.DOT_MATCHES_ALL)
        
        val replacement = """
            internal val androidSmokeModelDescriptor = ModelAssetDescriptor(
                id = "eyespie-offline-model",
                version = "$version",
                filename = "$filename",
                expectedBytes = ${size}L,
                sha256 = "$sha256",
                runtime = ModelRuntimeCompatibility(
                    engine = "mediapipe",
                    minimumRuntimeVersion = "0.10.35",
                    minimumModelAbi = 1,
                ),
            )
        """.trimIndent().trim()

        val updated = content.replace(pattern, replacement)
        if (updated != content) {
            moduleFile.writeText(updated)
        }
    }

    private fun updateIosDescriptor(
        projectDir: File,
        version: String,
        filename: String,
        size: Long,
        sha256: String
    ) {
        val moduleFile = projectDir.resolve("src/iosMain/kotlin/com/micrantha/eyespie/model/Module.ios.kt")
        if (!moduleFile.exists()) return

        val content = moduleFile.readText()
        val pattern = Regex("""internal val iosSmokeModelDescriptor = ModelAssetDescriptor\(.*?\n\)""", RegexOption.DOT_MATCHES_ALL)
        
        val replacement = """
            internal val iosSmokeModelDescriptor = ModelAssetDescriptor(
                id = "eyespie-offline-model",
                version = "$version",
                filename = "$filename",
                expectedBytes = ${size}L,
                sha256 = "$sha256",
                runtime = ModelRuntimeCompatibility(
                    engine = "mediapipe",
                    minimumRuntimeVersion = "0.10.35",
                    minimumModelAbi = 1,
                ),
            )
        """.trimIndent().trim()

        val updated = if (content.contains("internal val iosSmokeModelDescriptor")) {
            content.replace(pattern, replacement)
        } else {
            // If it doesn't exist, insert it before the first function
            content.replaceFirst("internal fun", "$replacement\n\ninternal fun")
        }

        if (updated != content) {
            moduleFile.writeText(updated)
        }
    }
>>>>>>> Stashed changes
}

internal fun Project.configureAssets(assets: BluebellAssets, downloads: BluebellDownloads) {

    val task = tasks.register("configureBluebellAssets", ConfigureBluebellAssetsTask::class.java) {
        group = "Bluebell"
        description = "Configure assets"

        manifest.set(assets.manifest)
        models.set(assets.models.toList())
        copies.set(assets.copies.toList())
        links.set(assets.links.toList())
        this.downloads.set(downloads.toList())

        outputDir.set(projectDir.resolve(defaultAssetSource))

        outputFiles.from(provider {
            val m = manifest.getOrNull()
            val result = mutableListOf<File>()
            if (m != null) {
                result.add(projectDir.resolve(defaultSharedDestination).resolve(m))
                result.add(projectDir.resolve(defaultIosDestination).resolve(m))
                result.add(projectDir.resolve(defaultAndroidDestination).resolve(m))
            }

            copies.get().forEach { asset ->
                asset.destination(projectDir).forEach { result.add(it.resolve(asset.name)) }
            }
            links.get().forEach { asset ->
                asset.destination(projectDir).forEach { result.add(it.resolve(asset.name)) }
            }
            result
        })
    }

    extensions.configure(BuildConfigExtension::class.java) {
        generateTask.configure {
            dependsOn(task)
        }
    }
}

internal fun generateAssetConfigs(
    projectDir: File,
    logger: org.gradle.api.logging.Logger,
    propertyResolver: (String) -> Any?,
    manifest: String?,
    models: List<BluebellAsset>,
    downloads: List<BluebellDownload>
) {
    if (manifest == null) return

    coreGenerateIosConfig(projectDir, logger, propertyResolver, manifest, models, downloads)
    coreGenerateAndroidConfig(projectDir, logger, propertyResolver, manifest, models, downloads)
    coreGenerateSharedConfig(projectDir, logger, propertyResolver, manifest, models, downloads)
}

internal fun Project.generateAssetConfigs(
    assets: BluebellAssets,
    downloads: NamedDomainObjectContainer<BluebellDownload>
) = generateAssetConfigs(projectDir, logger, ::findProperty, assets.manifest, assets.models.toList(), downloads.toList())
