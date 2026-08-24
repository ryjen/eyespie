plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.nativeCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

fun xcconfigValue(path: String, name: String): String {
    val configFile = rootProject.file(path)
    return configFile
        .readLines()
        .firstOrNull { line -> line.substringBefore('=').trim() == name }
        ?.substringAfter('=')
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: error("missing $name in ${configFile.path}")
}

val appVersion = xcconfigValue("iosApp/Configuration/Version.xcconfig", "APP_VERSION")
require(Regex("[0-9]+[.][0-9]+[.][0-9]+(?:-[0-9A-Za-z.-]+)?").matches(appVersion)) {
    "APP_VERSION must be semantic-version shaped"
}
val appBuild = xcconfigValue("iosApp/Configuration/Version.xcconfig", "APP_BUILD").toIntOrNull()
    ?: error("APP_BUILD must be an integer")
require(appBuild > 0) { "APP_BUILD must be positive" }

val iosMediaPipeTasksVersion = xcconfigValue(
    "iosApp/Configuration/MediaPipe.xcconfig",
    "IOS_MEDIAPIPE_TASKS_VERSION",
)
require(Regex("[0-9]+[.][0-9]+[.][0-9]+(?:[.][0-9]+)?").matches(iosMediaPipeTasksVersion)) {
    "IOS_MEDIAPIPE_TASKS_VERSION must be numeric-version shaped"
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
            version = iosMediaPipeTasksVersion
            source = path(project.file("../iosApp/MediaPipePodspecs"))
            moduleName = "MediaPipeTasksCommon"
            packageName = "MediaPipeTasksCommon"
            extraOpts += mediaPipeInteropOpts(
                "EyespieMediaPipeTasksCommon" to "MediaPipeTasksCommon",
            )
        }
        pod("EyespieMediaPipeTasksGenAIC") {
            version = iosMediaPipeTasksVersion
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
            version = iosMediaPipeTasksVersion
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
            version = iosMediaPipeTasksVersion
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
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.junit)
                implementation(libs.androidx.ui.test.junit4)
            }
        }
    }
}

dependencies {
    debugImplementation(libs.androidx.ui.test.manifest)
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

val androidImageEmbedderFile = project.file(
    "src/androidMain/assets/mobilenet_v3_small_100_224_embedder.tflite",
)
val stageAndroidImageEmbedderModel by tasks.registering(Exec::class) {
    group = "eyespie setup"
    description = "Stage and verify the pinned Android image-embedder model for runtime packaging."
    workingDir(rootProject.projectDir)
    commandLine(
        "python3",
        "scripts/stage_image_embedder_model.py",
        "stage",
        "--target",
        "android",
    )
    inputs.file(rootProject.file("models/image-embedder.json"))
    inputs.file(rootProject.file("scripts/stage_image_embedder_model.py"))
    outputs.file(androidImageEmbedderFile)
}

val verifyAndroidRuntime by tasks.registering(Exec::class) {
    group = "verification"
    description = "Verify the staged Android runtime model offline against the pinned manifest."
    workingDir(rootProject.projectDir)
    commandLine(
        "python3",
        "scripts/stage_image_embedder_model.py",
        "verify",
        "--target",
        "android",
    )
    inputs.file(rootProject.file("models/image-embedder.json"))
    inputs.file(rootProject.file("scripts/stage_image_embedder_model.py"))
}

// Keep ordinary compile/test/assemble tasks network-independent. A signed release
// must consume an artifact that was staged explicitly by the release preparation
// workflow and then verified without download/repair at the build boundary.
val signedAndroidRelease = providers.gradleProperty("android.injected.signing.store.file")
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    if (signedAndroidRelease.isPresent) {
        dependsOn(verifyAndroidRuntime)
    }
}

android {
    namespace = "com.micrantha.eyespie"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.micrantha.eyespie"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appBuild
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "MEDIAPIPE_TASKS_VISION_VERSION",
            "\"${libs.versions.mediapipe.get()}\"",
        )
    }

    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].assets.srcDirs("src/androidMain/assets")
    sourceSets["androidTest"].assets.srcDir("src/androidInstrumentedTest/assets")

    buildFeatures {
        compose = true
        buildConfig = true
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
