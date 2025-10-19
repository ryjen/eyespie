package com.micrantha.bluebell.app

interface LocalNotifier {
    fun startDownloadListener(tag: String, title: String, message: String)

    fun cancel(tag: String)
}
