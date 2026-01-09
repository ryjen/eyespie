package com.micrantha.bluebell.platform

import com.micrantha.bluebell.app.LocalNotifier
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

class Notifications : LocalNotifier {
    init {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ -> println("iOS permission granted: $granted") }
    }

    override fun startDownloadListener(tag: String, title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
        }
        val request = UNNotificationRequest.requestWithIdentifier(tag, content, null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    override fun cancel(tag: String) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(tag))
    }
}
