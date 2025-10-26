package com.micrantha.bluebell.platform

import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSUUID

actual class BackgroundDownloader {
    companion object {
        const val BACKGROUND_SESSION_ID = "com.app.background.downloads"
    }

    private val urlSessionTasks = mutableMapOf<String, NSURLSessionDownloadTask>()

    private val backgroundSession: NSURLSession by lazy {
        val config = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(
            BACKGROUND_SESSION_ID
        )
        config.allowsCellularAccess = true
        config.discretionary = false
        config.sessionSendsLaunchEvents = true

        NSURLSession.sessionWithConfiguration(
            configuration = config,
            delegate = null,//downloadSessionDelegate,
            delegateQueue = NSOperationQueue.mainQueue
        )
    }

    actual fun startDownload(tag: String, name: String, url: String, checksum: String?): String {
        val taskId = NSUUID().UUIDString

        return taskId
    }
}
