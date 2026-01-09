package com.micrantha.bluebell.plugin.config

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.github.gmazzo.buildconfig.BuildConfigTask
import com.micrantha.bluebell.plugin.bluebell
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

internal fun BluebellConfig.loadConfigFromEnvironment(manifestName: String?): Result<Map<String, String>> {
    try {
        val properties = Properties()
        FileInputStream(envFile).use { fileInputStream ->
            properties.load(fileInputStream)
        }
        val config = properties.entries.associate { (key, value) -> key.toString() to "\"$value\"" }
            .toMutableMap().apply {
                manifestName?.let { this["ASSET_MANIFEST"] = "\"$it\"" }
                defaultedKeys.filterNot { containsKey(it) }.forEach {
                    this[it] = "null"
                }
            }
        return Result.success(config.toMap())
    } catch (e: Exception) {
        return Result.failure(e)
    }
}

internal fun Project.configureBuilds(config: BluebellConfig, manifestName: String?) {

    config.properties = config.loadConfigFromEnvironment(manifestName).getOrDefault(emptyMap())

    val requiredKeyError = { key: String ->
        logger.bluebell("Missing '$key' in ${config.envFile}", logger::error)
        logger.error("${config.envFile} must contain the following variables:")
        config.requiredKeys.forEach { logger.error("  - $it") }
        error("missing key '$key' in ${config.envFile}")
    }

    fun generateSource(task: BuildConfigTask) {
        val entries =
            config.properties.entries.map { "\"${it.key}\" to ${config.className}.${it.key}" }
                .toMutableList()

        val outputDir = task.outputDir.dir(
            config.packageName.replace(".", File.separator)
        ).get().also {
            it.asFile.mkdirs()
        }

        val sourceFile = outputDir.file("${config.className}Ext.kt").asFile

        // Example code generation logic
        sourceFile.writeText(generatedExtensionSourceCode(config, entries))
        logger.bluebell("Generated config extensions")
    }

    extensions.configure(BuildConfigExtension::class.java) {
        packageName(config.packageName)
        className(config.className)
        useKotlinOutput { topLevelConstants = false }

        val configureBuild = {
            config.expectedKeys.forEach { key ->
                if (config.properties.containsKey(key).not()) {
                    logger.bluebell("Missing key '$key' in ${config.envFile}", logger::warn)
                }
            }

            config.requiredKeys.forEach { key ->
                if (config.properties.containsKey(key).not()) {
                    requiredKeyError(key)
                }
            }

            config.properties.forEach { (key, value) ->
                buildConfigField(if (value == "null") "String?" else "String", key, value)
            }

            logger.bluebell("Generated ${config.packageName}.${config.className}")
        }

        val configTask = tasks.register("generateBluebellConfig") {
            group = "Bluebell"
            description = "Generates the local build config"

            configureBuild()
        }

        generateTask.get().dependsOn(configTask)

        val generateExtensionsTask = tasks.register("generateBluebellConfigExtensions") {
            group = "Bluebell"
            description = "Generates the local build config extensions"

            dependsOn(configTask)

            doLast {
                generateSource(generateTask.get())
            }
        }

        generateTask.get().dependsOn(generateExtensionsTask)
    }
}

private fun generatedExtensionSourceCode(config: BluebellConfig, entries: List<String>) = """
package ${config.packageName}
import kotlin.reflect.KProperty

${if (config.properties.isEmpty()) "object ${config.className}" else ""}

private val map = mapOf<String, String?>(
    ${entries.joinToString(",\n    ")}
)

internal fun ${config.className}.get(key: String): String? {
    return map[key]
}    

internal operator fun ${config.className}.getValue(thisRef: Any?, property: KProperty<*>): String =
    map[property.name] ?: ""
""".trimIndent()


internal fun Project.configureGraphql(graphql: BluebellGraphqlConfig, config: BluebellConfig) {

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
