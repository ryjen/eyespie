package com.micrantha.bluebell.plugin

import com.micrantha.bluebell.plugin.asset.configureAssets
import com.micrantha.bluebell.plugin.config.configureBuilds
import com.micrantha.bluebell.plugin.config.configureGraphql
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BluebellPlugin : Plugin<Project> {
    private var ext: BluebellExtension? = null

    override fun apply(project: Project) = project.run {
        bluebellExtension().also { ext = it }.run {

            loadPlugins()

            afterEvaluate {
                configureAssets(assets, downloads)
                configureGraphql(graphql, config)
                configureBuilds(config, assets.manifest)
            }
        }
    }
}

fun Project.bluebellExtension(): BluebellExtension = extensions.create(
    "bluebell", BluebellExtension::class.java
)

private fun Project.loadPlugins() {
    pluginManager.apply("com.github.gmazzo.buildconfig")

    pluginManager.withPlugin("com.android.application") {
        logger.bluebell("Android application found")
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        logger.bluebell("KMP found")
    }
}
