plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlinSerialization)
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://www.jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.build.config)
    compileOnly(libs.android.build.tools)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("bluebell") {
            id = "com.micrantha.bluebell"
            implementationClass = "com.micrantha.bluebell.plugin.BluebellPlugin"
        }
    }
}
