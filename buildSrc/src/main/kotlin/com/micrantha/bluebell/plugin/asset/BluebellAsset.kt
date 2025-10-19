package com.micrantha.bluebell.plugin.asset

import javax.inject.Inject

sealed class BluebellAsset(val name: String) {
    var filename: String? = null

    open class IosAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class AndroidAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class SharedAsset @Inject constructor(name: String) : BluebellAsset(name)
    open class DefaultAsset @Inject constructor(name: String) : BluebellAsset(name)
}
