package com.micrantha.bluebell.platform

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.browser.trusted.sharing.ShareTarget.FileFormField.KEY_NAME
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import java.io.File
import java.security.MessageDigest

class BackgroundDownloadManager(
    private val context: Context,
) : BackgroundDownloader, FetchListener {


    init {
        createNotificationChannel()
    }

    private val metaData by lazy {
        mutableMapOf<Int, MetaData>()
    }

    data class MetaData(
        val name: String,
        val checksum: String?
    )

    private val notificationManager by lazy {
        NotificationManagerCompat.from(context)
    }

    private val notification by lazy {
        NotificationCompat.Builder(context.applicationContext, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup("Downloads")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, true)
    }

    private val fetch by lazy {
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .enableLogging(true)
            .build()

        Fetch.Impl.getInstance(fetchConfiguration)
    }

    private fun createDownloadFile(fileName: String): File {
        val downloadDir = File(context.filesDir, "downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return File(downloadDir, fileName)
    }

    override fun startDownload(
        tag: String,
        name: String,
        url: String,
        fileName: String,
        checksum: String?
    ) {

        val data = mutableMapOf(
            KEY_NAME to name,
        )
        checksum?.let { data[KEY_CHECKSUM] = it }

        val request = Request(
            url = url,
            file = createDownloadFile(fileName).absolutePath
        ).apply {
            this.tag = tag
            priority = Priority.HIGH
            networkType = NetworkType.UNMETERED
            extras = Extras(data)
        }

        fetch.enqueue(request)

        metaData[request.id] = MetaData(name, checksum)
    }

    override fun onAdded(download: Download) = Unit

    override fun onQueued(
        download: Download,
        waitingOnNetwork: Boolean
    ) = Unit

    override fun onWaitingNetwork(download: Download) = Unit

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCompleted(download: Download) {
        updateNotification(download)

        val expectedChecksum = download.extras.map[KEY_CHECKSUM] ?: return

        val isValid = validateChecksum(download.file, expectedChecksum)
        if (isValid) {
            Log.d("Fetch", "Checksum valid ✅")
        } else {
            Log.e("Fetch", "Checksum mismatch ❌")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onError(
        download: Download,
        error: Error,
        throwable: Throwable?
    ) {
        updateNotification(download)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) {
        updateNotification(download)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) {
        updateNotification(download)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        updateNotification(download)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onPaused(download: Download) {
        updateNotification(download)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onResumed(download: Download) {
        updateNotification(download)
    }

    override fun onCancelled(download: Download) {
        notificationManager.cancel(download.id)
    }

    override fun onRemoved(download: Download) {
        notificationManager.cancel(download.id)
    }

    override fun onDeleted(download: Download) {
        notificationManager.cancel(download.id)
    }

    private fun validateChecksum(file: String, checksum: String?): Boolean {
        if (checksum == null) return true

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)

        File(file).inputStream().use { inputStream ->
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
        }

        val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
        return actualChecksum.equals(checksum, ignoreCase = true)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_DOWNLOADS,
            "Download Progress Notifications",
            NotificationManager.IMPORTANCE_LOW
        )
        context.applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification(download: Download) {
        notification
            .setContentTitle(download.extras.getString(KEY_NAME, "File"))
            .setContentText(download.status.text)

        when (download.status) {
            Status.DOWNLOADING -> {
                notification.setProgress(100, download.progress, download.progress <= 0)
            }

            Status.ADDED, Status.QUEUED -> {
                notification.setProgress(0, 0, true)
            }

            else -> {
                notification.setProgress(0, 0, false)
            }
        }

        notificationManager.notify(download.id, notification.build())
    }

    private val Status.text: String
        get() = when (this) {
            Status.QUEUED -> "Queued"
            Status.DOWNLOADING -> "Downloading"
            Status.PAUSED -> "Paused"
            Status.COMPLETED -> "Completed"
            Status.CANCELLED -> "Cancelled"
            Status.FAILED -> "Failed"
            else -> ""
        }

    companion object {
        private const val CHANNEL_DOWNLOADS = "downloads"
        private const val KEY_CHECKSUM = "checksum"
    }
}
