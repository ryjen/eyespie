package com.micrantha.bluebell.platform

import com.micrantha.bluebell.app.LocalNotifier

expect class Notifications : LocalNotifier {
   override fun startDownloadListener(tag: String, title: String, message: String)

   override fun cancel(tag: String)
}
