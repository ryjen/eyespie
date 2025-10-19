package com.micrantha.bluebell.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

open class BluebellPlugin : Plugin<Project> {
    private var ext: BluebellExtension? = null

    override fun apply(project: Project) = project.run {
        val bluebell = bluebellExtension().also { ext = it }

        loadPlugins()

        afterEvaluate {
            configureAssets(bluebell.assets)
            configureGraphql(bluebell.graphql, bluebell.config)
            configureBuilds(bluebell.config, bluebell.assets.manifest)
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

private fun Project.configureGraphql(graphql: GraphqlConfig, config: BluebellConfig) {

    if (graphql.endpoint.isBlank()) {
        config.properties["SUPABASE_URL"]?.let {
            graphql.endpoint = "$it/graphql/v1"
        }
    }

    config.properties["SUPABASE_KEY"]?.let { key ->
        graphql.headers = graphql.headers.toMutableMap().apply {
            put("apikey", key)
        }
    }
}
