package com.micrantha.bluebell.plugin.download

import javax.inject.Inject

sealed class BluebellDownload(val name: String, val url: String) {
    var checksum: String? = null

    open class IosDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)

    open class AndroidDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)

    open class DefaultDownload @Inject constructor(name: String, url: String) :
        BluebellDownload(name, url)
}
