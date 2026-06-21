package com.micrantha.bluebell.plugin.config

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.micrantha.bluebell.plugin.bluebell
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
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

abstract class BluebellConfigExtensionsTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:Input
    abstract val properties: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val pName = packageName.get()
        val cName = className.get()
        val props = properties.get()

        val entries = props.entries.map { "\"${it.key}\" to $cName.${it.key}" }

        val targetDir = outputDir.dir(pName.replace(".", File.separator)).get().asFile
        targetDir.mkdirs()

        val sourceFile = File(targetDir, "${cName}Ext.kt")

        sourceFile.writeText(generatedExtensionSourceCode(pName, cName, props.isEmpty(), entries))
        logger.bluebell("Generated config extensions")
    }
}

internal fun Project.configureBuilds(config: BluebellConfig, manifestName: String?) {

    config.properties = config.loadConfigFromEnvironment(manifestName).getOrDefault(emptyMap())
    if (config.properties.isEmpty()) {
        logger.bluebell("No config properties loaded from ${config.envFile}", logger::warn)
    }

    val isProduction = gradle.startParameter.taskNames.any {
        it.contains("release", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
    } || hasProperty("production")

    val requiredKeyError = { key: String ->
        logger.bluebell("Missing '$key' in ${config.envFile}", logger::error)
        logger.error("${config.envFile} must contain the following variables:")
        config.requiredKeys.forEach { logger.error("  - $it") }
        error("missing key '$key' in ${config.envFile}")
    }

    extensions.configure(BuildConfigExtension::class.java) {
        packageName(config.packageName)
        className(config.className)
        useKotlinOutput { topLevelConstants = false }

        config.expectedKeys.forEach { key ->
            if (config.properties.containsKey(key).not()) {
                logger.bluebell("Missing expected key '$key' in ${config.envFile}", logger::warn)
            }
        }

        config.requiredKeys.forEach { key ->
            if (config.properties.containsKey(key).not()) {
                if (isProduction) {
                    requiredKeyError(key)
                } else {
                    logger.bluebell("Missing required key '$key' in ${config.envFile} (Required for production builds)", logger::warn)
                }
            }
        }

        config.properties.forEach { (key, value) ->
            buildConfigField(if (value == "null") "String?" else "String", key, value)
        }

        logger.bluebell("Generated ${config.packageName}.${config.className}")

        val generateExtensionsTask = tasks.register("generateBluebellConfigExtensions", BluebellConfigExtensionsTask::class.java) {
            group = "Bluebell"
            description = "Generates the local build config extensions"

            packageName.set(config.packageName)
            className.set(config.className)
            properties.set(config.properties)
            outputDir.set(generateTask.flatMap { it.outputDir })

            dependsOn(generateTask)
        }

        tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
            dependsOn(generateExtensionsTask)
        }

        generateTask.configure {
            finalizedBy(generateExtensionsTask)
        }
    }
}

private fun generatedExtensionSourceCode(
    packageName: String,
    className: String,
    isEmpty: Boolean,
    entries: List<String>
) = """
package $packageName
import kotlin.reflect.KProperty

${if (isEmpty) "object $className" else ""}

private val map = mapOf<String, String?>(
    ${entries.joinToString(",\n    ")}
)

internal fun $className.get(key: String): String? {
    return map[key]
}    

internal operator fun $className.getValue(thisRef: Any?, property: KProperty<*>): String =
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
