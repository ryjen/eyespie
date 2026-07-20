import com.android.build.api.dsl.ApplicationExtension
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
}

project(":app") {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            assetPacks += setOf(":model-pack")
        }
    }
}