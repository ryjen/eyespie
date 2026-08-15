plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.nativeCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(21)

    cocoapods {
        version = "1.0"
        name = "eyespie"
        summary = "Eyespie backendless Kotlin Multiplatform core"
        homepage = "https://github.com/ryjen/eyespie"
        license = "MPL-2.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")
        extraSpecAttributes["libraries"] = "'c++', 'sqlite3'"

        framework {
            baseName = "eyespie"
            isStatic = true
        }

        fun mediaPipeInteropOpts(vararg modules: Pair<String, String>): List<String> {
            val installedPods = project.file("../iosApp/Pods").absoluteFile
            return buildList {
                add("-compiler-option")
                add("-fmodules")
                modules.forEach { (podName, moduleName) ->
                    add("-compiler-option")
                    add("-F${installedPods}/$podName/frameworks/$moduleName.xcframework/ios-arm64_x86_64-simulator")
                    add("-compiler-option")
                    add("-F${installedPods}/$podName/frameworks/$moduleName.xcframework/ios-arm64")
                }
            }
        }

        pod("EyespieMediaPipeTasksCommon") {
            version = "0.10.26.2"
            source = path(project.file("../iosApp/MediaPipePodspecs"))
            moduleName = "MediaPipeTasksCommon"
            packageName = "MediaPipeTasksCommon"
            extraOpts += mediaPipeInteropOpts(
                "EyespieMediaPipeTasksCommon" to "MediaPipeTasksCommon",
            )
        }
        pod("EyespieMediaPipeTasksGenAIC") {
            version = "0.10.26.2"
            source = path(project.file("../iosApp/MediaPipePodspecs"))
            moduleName = "MediaPipeTasksGenAIC"
            packageName = "MediaPipeTasksGenAIC"
            extraOpts += mediaPipeInteropOpts(
                "EyespieMediaPipeTasksGenAIC" to "MediaPipeTasksGenAIC",
                "EyespieMediaPipeTasksCommon" to "MediaPipeTasksCommon",
            )
            useInteropBindingFrom("EyespieMediaPipeTasksCommon")
        }
        pod("EyespieMediaPipeTasksVision") {
            version = "0.10.26.2"
            source = path(project.file("../iosApp/MediaPipePodspecs"))
            moduleName = "MediaPipeTasksVision"
            packageName = "MediaPipeTasksVision"
            extraOpts += mediaPipeInteropOpts(
                "EyespieMediaPipeTasksVision" to "MediaPipeTasksVision",
                "EyespieMediaPipeTasksCommon" to "MediaPipeTasksCommon",
            )
            useInteropBindingFrom("EyespieMediaPipeTasksCommon")
        }
        pod("EyespieMediaPipeTasksGenAI") {
            version = "0.10.26.2"
            source = path(project.file("../iosApp/MediaPipePodspecs"))
            moduleName = "MediaPipeTasksGenAI"
            packageName = "MediaPipeTasksGenAI"
            extraOpts += mediaPipeInteropOpts(
                "EyespieMediaPipeTasksGenAI" to "MediaPipeTasksGenAI",
                "EyespieMediaPipeTasksGenAIC" to "MediaPipeTasksGenAIC",
                "EyespieMediaPipeTasksCommon" to "MediaPipeTasksCommon",
            )
            useInteropBindingFrom("EyespieMediaPipeTasksGenAIC")
        }
    }

    applyDefaultHierarchyTemplate()
    androidTarget()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "eyespie"
            isStatic = true
            binaryOption("bundleId", "com.micrantha.eyespie")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mediapipe.tasks.vision)
            implementation(libs.mediapipe.tasks.genai)
            implementation(libs.sqldelight.android.driver)
            implementation("com.google.android.play:asset-delivery:2.3.0")
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("EyesPieDatabase") {
            packageName.set("com.micrantha.eyespie.data")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

android {
    namespace = "com.micrantha.eyespie"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.micrantha.eyespie"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets["main"].res.srcDirs("src/androidMain/res")

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}
