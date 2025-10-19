package com.micrantha.bluebell.plugin.asset

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory

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
