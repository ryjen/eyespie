package com.micrantha.bluebell

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
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

abstract class BluebellAssets @Inject constructor(private val objects: ObjectFactory) {
    var manifest: String? = null
    internal val downloads = objects.domainObjectContainer(BluebellDownload::class)
    internal val copies: NamedDomainObjectContainer<BluebellAsset> =
        objects.domainObjectContainer(BluebellAsset::class)
    internal val links: NamedDomainObjectContainer<BluebellAsset> =
        objects.domainObjectContainer(BluebellAsset::class)

    private val copyBuilder = BluebellAssetBuilder(copies, objects)
    private val linkBuilder = BluebellAssetBuilder(links, objects)
    private val downloadBuilder = BluebellDownloadBuilder(downloads, objects)

    fun copies(action: Action<in BluebellAssetBuilder>) {
        action.execute(copyBuilder)
    }

    fun links(action: Action<in BluebellAssetBuilder>) {
        action.execute(linkBuilder)
    }

    fun downloads(action: Action<in BluebellDownloadBuilder>) {
        action.execute(downloadBuilder)
    }
}

sealed class BluebellAsset(val name: String) {
    open class IosAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class AndroidAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class SharedAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class DefaultAsset @Inject constructor(name: String) : BluebellAsset(name)
}

class BluebellAssetBuilder(
    private val container: NamedDomainObjectContainer<BluebellAsset>,
    private val objectFactory: ObjectFactory
) {
    fun create(
        name: String,
        configure: Action<BluebellAsset.DefaultAsset>? = null
    ): BluebellAsset.DefaultAsset {
        val asset = objectFactory.newInstance(BluebellAsset.DefaultAsset::class.java, name)
        container.add(asset)
        configure?.execute(asset)
        return asset
    }

    fun ios(
        name: String,
        configure: Action<BluebellAsset.IosAsset>? = null
    ): BluebellAsset.IosAsset {
        val asset = objectFactory.newInstance(BluebellAsset.IosAsset::class.java, name)
        container.add(asset)
        configure?.execute(asset)
        return asset
    }

    fun android(
        name: String,
        configure: Action<BluebellAsset.AndroidAsset>? = null
    ): BluebellAsset.AndroidAsset {
        val asset = objectFactory.newInstance(BluebellAsset.AndroidAsset::class.java, name)
        container.add(asset)
        configure?.execute(asset)
        return asset
    }

    fun shared(
        name: String,
        configure: Action<BluebellAsset.SharedAsset>? = null
    ): BluebellAsset.SharedAsset {
        val asset = objectFactory.newInstance(BluebellAsset.SharedAsset::class.java, name)
        container.add(asset)
        configure?.execute(asset)
        return asset
    }

}
