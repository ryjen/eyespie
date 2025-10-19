package com.micrantha.bluebell.plugin

import com.micrantha.bluebell.plugin.asset.BluebellAssets
import com.micrantha.bluebell.plugin.config.BluebellConfig
import com.micrantha.bluebell.plugin.config.BluebellGraphqlConfig
import com.micrantha.bluebell.plugin.download.BluebellDownload
import com.micrantha.bluebell.plugin.download.BluebellDownloadBuilder
import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.domainObjectContainer
import javax.inject.Inject

open class BluebellExtension @Inject constructor(
    objects: ObjectFactory
) {
    val config = objects.newInstance(BluebellConfig::class.java)

    val assets = objects.newInstance(BluebellAssets::class.java)

    val graphql = objects.newInstance(BluebellGraphqlConfig::class.java)

    val downloads = objects.domainObjectContainer(BluebellDownload::class)

    private val downloadBuilder = BluebellDownloadBuilder(downloads, objects)

    fun config(action: Action<BluebellConfig>) {
        action.execute(config)
    }

    fun assets(action: Action<BluebellAssets>) {
        action.execute(assets)
    }

    fun graphql(action: Action<BluebellGraphqlConfig>) {
        action.execute(graphql)
    }

    fun downloads(action: Action<in BluebellDownloadBuilder>) {
        action.execute(downloadBuilder)
    }
}

fun Logger.bluebell(message: Any, type: (String?) -> Unit = ::lifecycle) {
    type("> Bluebell: $message")
}
