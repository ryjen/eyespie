package com.micrantha.bluebell.data.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.micrantha.bluebell.platform.Notifications.Companion.KEY_MESSAGE
import com.micrantha.bluebell.platform.Notifications.Companion.KEY_TITLE
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ProgressListenerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "progress_channel"
        const val NOTIFICATION_ID = 2001
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Downloading"
        val message = inputData.getString(KEY_MESSAGE) ?: "Download in progress"

        createNotificationChannel()
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(title)
            .setContentText(message)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        val job = CompletableDeferred<Result>()

        val observer = Observer<WorkInfo?> { info ->
            if (info == null) {
                job.complete(Result.failure())
                return@Observer
            }
            val progress = info.progress.getInt(KEY_PROGRESS, 0)
            builder.setProgress(100, progress, false)
            notificationManager.notify(NOTIFICATION_ID, builder.build())

            if (info.state.isFinished) {
                builder.setContentText("Download complete")
                    .setProgress(0, 0, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                job.complete(Result.success())
            }
        }

        withContext(Dispatchers.Main) {
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(UUID.fromString(taskId))
                .observeForever(observer)
        }

        return job.await()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Progress Notifications",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
