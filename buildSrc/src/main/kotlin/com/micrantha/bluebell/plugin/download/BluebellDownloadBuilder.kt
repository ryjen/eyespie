package com.micrantha.bluebell.plugin.download

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory

class BluebellDownloadBuilder(
    private val container: NamedDomainObjectContainer<BluebellDownload>,
    private val objectFactory: ObjectFactory
) {
    var url: String? = null
    var checksum: String? = null

    fun ios(name: String, configure: BluebellDownloadBuilder.() -> Unit) {
        configure.invoke(this)
        val download =
            objectFactory.newInstance(BluebellDownload.IosDownload::class.java, name, url).also {
                it.checksum = checksum
            }
        container.add(download)
    }

    fun android(name: String, configure: BluebellDownloadBuilder.() -> Unit) {
        configure.invoke(this)
        val download =
            objectFactory.newInstance(BluebellDownload.AndroidDownload::class.java, name, url)
                .also {
                    it.checksum = checksum
                }
        container.add(download)
    }

    fun create(name: String, configure: BluebellDownloadBuilder.() -> Unit) {
        configure(this)
        val download =
            objectFactory.newInstance(BluebellDownload.DefaultDownload::class.java, name, url)
                .also {
                    it.checksum = this.checksum
                }
        container.add(download)
    }
}
