package com.micrantha.bluebell.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.micrantha.bluebell.app.LocalNotifier
import com.micrantha.bluebell.data.download.DownloadTaskManager
import com.micrantha.bluebell.data.download.KEY_TASK_ID
import com.micrantha.bluebell.data.download.ProgressListenerWorker

actual class Notifications(
    private val context: Context,
    private val taskManager: DownloadTaskManager
) : LocalNotifier {
    private val manager by lazy { NotificationManagerCompat.from(context.applicationContext) }
    private val workManager by lazy {WorkManager.getInstance(context.applicationContext)}

    init {
        createChannel(context)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_DOWNLOADS,
            "Download Progress Notifications",
            NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService(NotificationManager::class.java)
           .createNotificationChannel(channel)
    }

    actual override fun startDownloadListener(tag: String, title: String, message: String) {
        val task = taskManager[tag] ?: return
        val notifyRequest = OneTimeWorkRequestBuilder<ProgressListenerWorker>()
            .setInputData(workDataOf(
                KEY_TASK_ID to task.id,
                KEY_TITLE to title,
                KEY_MESSAGE to message
            ))
            .addTag(tag)
            .build()

        workManager.enqueueUniqueWork(
            tag, ExistingWorkPolicy.REPLACE, notifyRequest
        )
    }

    actual override fun cancel(tag: String) {
        workManager.cancelUniqueWork(tag)
        manager.cancel(tag.hashCode())
    }

    companion object {
        const val NOTIFICATION_CHANNEL_DOWNLOADS = "downloads"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
    }
}
