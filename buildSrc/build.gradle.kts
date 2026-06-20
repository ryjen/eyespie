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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

gradlePlugin {
    plugins {
        create("bluebell") {
            id = "com.micrantha.bluebell"
            implementationClass = "com.micrantha.bluebell.plugin.BluebellPlugin"
        }
    }
}
