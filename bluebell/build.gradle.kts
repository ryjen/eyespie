plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)

    applyDefaultHierarchyTemplate()

    androidTarget()

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.components.resources)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)

            implementation(libs.kodein.di)
            implementation(libs.kodein.di.framework.compose)
            implementation(libs.kodein.di.conf)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.kodein)

            implementation(libs.okio)
            implementation(libs.kotlin.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.fetch)
            implementation(libs.mediapipe.tasks.vision)
            implementation(libs.mediapipe.tasks.genai)
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    android {
        namespace = "com.micrantha.bluebell"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        defaultConfig {
            minSdk = libs.versions.android.minSdk.get().toInt()
        }

        buildFeatures {
            buildConfig = true
        }
    }
}
