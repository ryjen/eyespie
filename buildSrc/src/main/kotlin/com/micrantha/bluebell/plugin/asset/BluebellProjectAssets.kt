package com.micrantha.bluebell.plugin.asset

import com.micrantha.bluebell.plugin.download.BluebellDownload
import com.micrantha.bluebell.plugin.download.BluebellDownloads
import com.micrantha.bluebell.plugin.filesystem.copyBuildAssets
import com.micrantha.bluebell.plugin.filesystem.linkBuildAssets
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project


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

internal fun Project.configureAssets(assets: BluebellAssets, downloads: BluebellDownloads) {

    val task = tasks.register("configureBluebellAssets") {
        group = "Bluebell"
        description = "Configure assets"

        copyBuildAssets(assets, downloads)
        linkBuildAssets(assets, downloads)

        generateAssetConfigs(assets, downloads)
    }

    tasks.findByName("generateBluebellConfig")?.dependsOn(task)
}

internal fun Project.generateAssetConfigs(
    assets: BluebellAssets,
    downloads: NamedDomainObjectContainer<BluebellDownload>
) {
    if (assets.manifest == null) return

    generateIosConfig(assets.manifest!!, assets.models, downloads)
    generateAndroidConfig(assets.manifest!!, assets.models, downloads)
    generateSharedConfig(assets.manifest!!, assets.models, downloads)
}
