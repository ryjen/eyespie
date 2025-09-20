enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.repsy.io/mvn/chrynan/public")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven {
            name = "GitHubPackagesCactus"
            url = uri("https://maven.pkg.github.com/cactus-compute/cactus-kotlin")
            credentials {
                username = providers.gradleProperty("GITHUB_USER").orNull ?: throw IllegalStateException("GITHUB_USER not set")
                password = providers.gradleProperty("GITHUB_TOKEN").orNull ?: throw IllegalStateException("GITHUB_TOKEN not set")
            }
        }
    }
}

rootProject.name = "EyesPie"
include(":composeApp")
