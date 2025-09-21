package com.micrantha.bluebell

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.domainObjectContainer
import javax.inject.Inject


open class BluebellConfig {
    var packageName: String = "com.micrantha.bluebell.config"
    var className: String = "BuildConfig"
    var envFile: String = ".env.local"
    var defaultedKeys: List<String> = emptyList()
    var expectedKeys: List<String> = emptyList()
    var requiredKeys: List<String> = emptyList()

    internal var properties: Map<String, String> = emptyMap()

    override fun toString() = "($packageName)"
}

open class GraphqlConfig {
    var serviceName: String = ""
    var packagePath: String? = null
    var endpoint: String = "http://localhost:8080"
    var headers: Map<String, String> = emptyMap()
}

abstract class BluebellAssets @Inject constructor(objects: ObjectFactory) {
    var downloads = objects.domainObjectContainer(BluebellAsset::class)
    fun runtimeDownloads() = downloads.filter { it.isBundled.not() }
    fun bundledDownloads() = downloads.filter { it.isBundled }
    val copies: List<BluebellCopy> = emptyList()
}

abstract class BluebellAsset @Inject constructor(val name: String) {
    var url: String? = null
    var androidUrl: String? = null
    var iosUrl: String? = null
    var isBundled: Boolean = false
    var checksum: String? = null
}

data class BluebellDownload<T>(
    val name: String,
    val android: T? = null,
    val ios: T? = null,
)

data class BluebellCopy(
    val source: String,
    val destination: String,
    val id: String
)
