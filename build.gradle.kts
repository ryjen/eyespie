import com.android.build.api.dsl.ApplicationExtension
import org.cyclonedx.model.Component
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.apolloGraphQL) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.nativeCocoapods) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.cyclonedx)
}

val verifyFeatureBoundaries by tasks.registering {
    group = "verification"
    description = "Enforce clean feature dependencies and the two-parameter screen contract."

    val featuresRoot = rootProject.file(
        "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/features",
    )
    inputs.dir(featuresRoot)

    doLast {
        val violations = mutableListOf<String>()
        val featureDirs = featuresRoot.listFiles { file -> file.isDirectory }?.toList().orEmpty()
        val featureNames = featureDirs.map { it.name }.toSet()

        featureDirs.filter { it.name != "app" }.forEach { featureDir ->
            featureDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { source ->
                source.readLines().filter { it.startsWith("import com.micrantha.eyespie.features.") }
                    .forEach { importLine ->
                        val importedFeature = importLine
                            .removePrefix("import com.micrantha.eyespie.features.")
                            .substringBefore('.')
                        if (importedFeature in featureNames && importedFeature != featureDir.name) {
                            violations += "${source.relativeTo(rootProject.projectDir)} imports feature '$importedFeature'"
                        }
                    }
            }

            featureDir.listFiles { file -> file.isFile && file.name.endsWith("Screen.kt") }
                ?.forEach { screenFile ->
                    val source = screenFile.readText()
                    val signature = Regex(
                        "fun\\s+\\w+Screen\\s*\\((.*?)\\)\\s*\\{",
                        setOf(RegexOption.DOT_MATCHES_ALL),
                    ).find(source)?.groupValues?.get(1)
                    if (signature == null) {
                        violations += "${screenFile.relativeTo(rootProject.projectDir)} has no parseable Screen signature"
                    } else {
                        val parameters = signature.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map { it.removeSuffix(",") }
                        if (
                            parameters.size != 2 ||
                            !parameters[0].startsWith("state:") ||
                            !parameters[1].startsWith("dispatch:")
                        ) {
                            violations += "${screenFile.relativeTo(rootProject.projectDir)} must accept exactly state + dispatch"
                        }
                    }
                }
        }

        check(violations.isEmpty()) {
            "Feature architecture violations:\n${violations.joinToString("\n") { "- $it" }}"
        }
    }
}

allprojects {
    tasks.cyclonedxDirectBom {
        enabled = project.path == ":app"
    }
}

tasks.cyclonedxBom {
    enabled = false
}

project(":app") {
    tasks.cyclonedxDirectBom {
        includeConfigs = listOf(
            ".*[Mm]ain.*[Rr]esolvable.*",
            ".*[Rr]eleaseRuntimeClasspath",
        )
        skipConfigs = listOf(
            ".*[Tt]est.*",
            ".*[Dd]ebug.*",
            ".*[Bb]enchmark.*",
            ".*[Ll]int.*",
        )
        projectType.set(Component.Type.APPLICATION)
        componentGroup.set("com.micrantha")
        componentName.set("eyespie")
        componentVersion.set(
            providers.environmentVariable("SBOM_COMPONENT_VERSION").orElse("0.1.0")
        )
        includeLicenseText.set(false)
        includeBuildEnvironment = false
        includeBuildSystem = true
        jsonOutput.set(rootProject.layout.buildDirectory.file("reports/sbom/eyespie-gradle.cdx.json"))
        xmlOutput.unsetConvention()
    }

    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(rootProject.tasks.named("verifyFeatureBoundaries"))
    }

    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            assetPacks += setOf(":model-pack")
        }
    }
}
