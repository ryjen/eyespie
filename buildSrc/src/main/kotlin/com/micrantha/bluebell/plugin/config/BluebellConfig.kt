package com.micrantha.bluebell.plugin.config

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
