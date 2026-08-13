plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.nativeCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.apolloGraphQL)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.sqldelight)
    id("com.micrantha.bluebell")
}

compose {
    resources {
        packageOfResClass = "com.micrantha.eyespie.generated.resources"
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    cocoapods {
        version = "1.0"
        name = "eyespie"
        summary = "Native dependencies for eyespie"
        homepage = "https://github.com/ryjen/eyespie"
        license = "MPL-2.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")
        extraSpecAttributes["libraries"] = "'c++', 'sqlite3'"

        framework {
            baseName = "eyespie"
            isStatic = true
        }

        // Kotlin/Native cinterop does not inherit CocoaPods framework search paths.
        // Point Clang directly at the vendored XCFramework slices installed by
        // CocoaPods in iosApp/Pods. Include dependency modules because MediaPipe
        // umbrella headers import them while cinterop parses the public module.
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

        // Declare the complete local pod graph. Kotlin's synthetic Podfile cannot
        // discover local transitive podspecs unless each local pod is declared here.
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

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "eyespie"
            isStatic = true
            binaryOption("bundleId", "com.micrantha.eyespie")
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(project(":bluebell"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.animation)
            implementation(libs.compose.animationGraphics)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)

            implementation(libs.kodein.di)
            implementation(libs.kodein.di.framework.compose)
            implementation(libs.kodein.di.conf)

            implementation(libs.okio)
            implementation(libs.kotlin.logging)

            implementation(libs.cache4k)

            implementation(libs.kotlinx.io)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.kodein)

            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.cio)

            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.apollo.graphql)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)

            implementation(libs.permissions.compose)
            implementation(libs.permissions.camera)
            implementation(libs.permissions.location)
            implementation(libs.permissions.notifications)
            implementation(libs.permissions.storage)

            implementation(libs.geo.compose)
            implementation(libs.kamel.image)
            implementation(libs.moko.media)

            implementation(libs.datastore)
            implementation(libs.datastore.preferences)

            implementation(libs.sqldelight.coroutines)

            //implementation("ca.rmen:rhymer:1.2.0")
            //implementation("org.hashids:hashids:1.0.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))

            implementation(libs.permissions.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.turbine)
            implementation(libs.okio.fakefilesystem)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment.ktx)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.palette.ktx)

            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.video)
            implementation(libs.androidx.camera.view)
            implementation(libs.androidx.camera.extensions)

            implementation(libs.androidx.exifinterface)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.fetch)
            implementation("com.google.android.play:asset-delivery:2.3.0")

            implementation(libs.mediapipe.tasks.vision)
            implementation(libs.mediapipe.tasks.genai)

            implementation(libs.compose.ui.tooling)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.robolectric)
                implementation(libs.androidx.ui.test.junit4)
                implementation(libs.androidx.ui.test.manifest)
                implementation(libs.roborazzi)
                implementation(libs.roborazzi.compose)
                implementation(libs.roborazzi.junit)
            }
        }

        appleTest {
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
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        versionCode = 10
        versionName = "1.0.0"
    }
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    buildFeatures {
        compose = true
        buildConfig = true
        mlModelBinding = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
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
        jniLibs {
            useLegacyPackaging = false
        }
    }
    signingConfigs {
        create("release") {
            System.getenv("ANDROID_STORE_FILE")?.let { storeFile = file(it) }
            storePassword = System.getenv("ANDROID_STORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    sqldelight {
        databases {
            create("EyesPieDatabase") {
                packageName.set("com.micrantha.eyespie.data")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    dependencies {
        debugImplementation(libs.okio.fakefilesystem)
    }
}

bluebell {
    config {
        packageName = "com.micrantha.eyespie.config"
        className = "EnvConfig"
        envFile = ".env.local"

        defaultedKeys = listOf(
            "SUPABASE_URL",
            "SUPABASE_KEY",
            "LOGIN_EMAIL",
            "LOGIN_PASSWORD",
        )
        requiredKeys = listOf(
            "SUPABASE_URL",
            "SUPABASE_KEY",
        )
    }
    graphql {
        serviceName = "eyespie"
        packagePath = "com.micrantha.eyespie.graphql"
    }

    afterEvaluate {
        apollo {
            service(graphql.serviceName) {
                packageNamesFromFilePaths(graphql.packagePath)
                introspection {
                    endpointUrl = graphql.endpoint
                    headers.putAll(graphql.headers)
                }
            }
        }
    }
}
