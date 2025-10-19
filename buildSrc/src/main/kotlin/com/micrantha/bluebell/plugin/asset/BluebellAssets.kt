package com.micrantha.bluebell.plugin.asset

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.domainObjectContainer
import javax.inject.Inject

abstract class BluebellAssets @Inject constructor(private val objects: ObjectFactory) {
    var manifest: String? = null
    var assetDirectory: String = "bluebellAssets"

    internal val copies = objects.domainObjectContainer(BluebellAsset::class)
    internal val links = objects.domainObjectContainer(BluebellAsset::class)
    internal val models = objects.domainObjectContainer(BluebellAsset::class)

    private val copyBuilder = BluebellAssetBuilder(copies, objects)
    private val linkBuilder = BluebellAssetBuilder(links, objects)
    private val modelBuilder = BluebellAssetBuilder(models, objects)

    fun copies(action: Action<in BluebellAssetBuilder>) {
        action.execute(copyBuilder)
    }

    fun links(action: Action<in BluebellAssetBuilder>) {
        action.execute(linkBuilder)
    }

    fun models(action: Action<in BluebellAssetBuilder>) {
        action.execute(modelBuilder)
    }
}
