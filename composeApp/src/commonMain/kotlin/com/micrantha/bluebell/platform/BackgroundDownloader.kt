package com.micrantha.bluebell.platform

import com.micrantha.eyespie.domain.entities.UrlFile

interface BackgroundDownloader {
    fun startDownload(
        tag: String,
        name: String,
        url: UrlFile
    )
}
