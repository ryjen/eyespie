package com.micrantha.bluebell.plugin.asset

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

sealed class BluebellAsset(@get:Input val name: String) {
    @get:Input
    @get:Optional
    var filename: String? = null

    open class IosAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class AndroidAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class SharedAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class DefaultAsset @Inject constructor(name: String) : BluebellAsset(name)
}
