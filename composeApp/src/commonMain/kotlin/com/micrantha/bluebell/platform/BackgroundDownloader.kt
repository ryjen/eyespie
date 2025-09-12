package com.micrantha.bluebell.platform

interface BackgroundDownloader {
    fun startDownload(
        tag: String,
        name: String,
        url: String,
        fileName: String,
        checksum: String? = null
    )
}
