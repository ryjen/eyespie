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
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.build.config)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}
