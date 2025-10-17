plugins {
    alias(libs.plugins.nativeCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.apolloGraphQL)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
    id("com.micrantha.bluebell")
}

kotlin {
    jvmToolchain(21)

    cocoapods {
        version = "1.0"
        summary = "Native dependencies for ${project.name}"
        homepage = "https://github.com/hackelia-micrantha/eyespie"
        license = "GPLv3"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")

        pod("MediaPipeTasksVision")
        pod("MediaPipeTasksGenAI")
    }

    applyDefaultHierarchyTemplate()

    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "com.micrantha.eyespie")
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.components.resources)
            implementation(compose.animation)
            implementation(compose.animationGraphics)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)

            implementation(libs.kodein.di)
            implementation(libs.kodein.di.framework.compose)
            implementation(libs.kodein.di.conf)

            implementation(libs.okio)

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

            //implementation("ca.rmen:rhymer:1.2.0")

            //implementation("org.hashids:hashids:1.0.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))

            implementation(libs.permissions.test)
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
            implementation(libs.mediapipe.tasks.vision)
            implementation(libs.mediapipe.tasks.genai)
            implementation(libs.compose.ui.tooling)
            implementation(libs.compose.ui.tooling.preview)
        }

        iosMain.dependencies {
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
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    buildFeatures {
        compose = true
        buildConfig = true
        mlModelBinding = true
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
        jniLibs {
            useLegacyPackaging = false // Ensures uncompressed .so files
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
        debugImplementation("com.squareup.okio:okio-fakefilesystem:3.7.0")
    }
}

bluebell {
    config {
        packageName = "com.micrantha.eyespie.config"
        className = "EnvConfig"
        envFile = ".env.local"

        defaultedKeys = listOf(
            "LOGIN_EMAIL",
            "LOGIN_PASSWORD",
        )
        expectedKeys = listOf(
            "SUPABASE_URL",
            "SUPABASE_KEY",
        )
    }
    assets {
        files {
            create("gemma3") {
                source = "../models/gemma3-1b-it-int4.litertlm"
            }
        }
        downloads {
            create("classification_efficientnet_lite.tflite") {
                url =
                    "https://storage.googleapis.com/mediapipe-models/image_classifier/efficientnet_lite0/int8/latest/efficientnet_lite0.tflite"
                isBundled = true
            }
            create("detection_efficientnet_lite.tflite") {
                url =
                    "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/latest/efficientdet_lite0.tflite"
                isBundled = true
            }
        }
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
