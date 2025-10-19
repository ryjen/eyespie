package com.micrantha.bluebell.platform

expect class BackgroundDownloader {
    fun startDownload(
        tag: String,
        name: String,
        url: String,
        checksum: String? = null
    )
}
