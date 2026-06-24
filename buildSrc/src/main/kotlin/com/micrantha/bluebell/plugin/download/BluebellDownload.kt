package com.micrantha.bluebell.plugin.download

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

sealed class BluebellDownload(@get:Input val name: String, @get:Input val url: String) {
    @get:Input
    @get:Optional
    var checksum: String? = null

    open class IosDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)

    open class AndroidDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)

    open class DefaultDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)
}
