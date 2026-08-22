import com.android.build.api.dsl.ApplicationExtension
import org.cyclonedx.model.Component
import org.gradle.api.tasks.Exec
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

val verifyFeatureBoundaries by tasks.registering(Exec::class) {
    group = "verification"
    description = "Enforce clean feature dependencies and the two-parameter screen contract."

    val script = layout.projectDirectory.file("scripts/verify_feature_boundaries.py")
    val features = layout.projectDirectory.dir(
        "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/features",
    )
    inputs.file(script)
    inputs.dir(features)
    workingDir(layout.projectDirectory)
    commandLine("python3", script.asFile.absolutePath)
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
