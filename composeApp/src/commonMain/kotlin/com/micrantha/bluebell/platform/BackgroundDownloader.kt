package com.micrantha.bluebell.platform

import com.benasher44.uuid.uuid4

interface BackgroundDownloader {
    fun startDownload(url: String, fileName: String): String {
        return uuid4().toString().also { tag ->
            startDownload(tag = tag, url = url, fileName = fileName)
        }
    }
    fun startDownload(tag: String, url: String, fileName: String)
    fun cancelDownload(tag: String)
    fun pauseDownload(tag: String)
    fun resumeDownload(tag: String, url: String, fileName: String)
}
