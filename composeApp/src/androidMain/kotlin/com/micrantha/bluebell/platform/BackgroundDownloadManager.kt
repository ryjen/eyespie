package com.micrantha.bluebell.platform

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.browser.trusted.sharing.ShareTarget.FileFormField.KEY_NAME
import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.eyespie.BuildConfig
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import okio.source
import java.io.File

class BackgroundDownloadManager(
    private val context: Context,
    private val namespace: String = "background-downloads"
) : DefaultFetchNotificationManager(context), BackgroundDownloader, FetchListener {

    private val fetchConfig by lazy {
        FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .setNotificationManager(this)
            .enableLogging(BuildConfig.DEBUG)
    }

    private val fetch by lazy {
        fetchConfig.setNamespace(namespace)
        Fetch.getInstance(fetchConfig.build()).apply {
            addListener(this@BackgroundDownloadManager)
        }
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
    }

    override fun onAdded(download: Download) = Unit

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onQueued(
        download: Download,
        waitingOnNetwork: Boolean
    ) = Unit

    override fun onWaitingNetwork(download: Download) = Unit

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCompleted(download: Download) {
        // If configuration provided a checksum, validate it
        // using sha-256 hash.  Fetch already validates md5 if the server supports
        val expectedChecksum = download.extras.map[KEY_CHECKSUM] ?: return
        val isValid = validateChecksum(download.file, expectedChecksum)
        if (isValid) {
            Log.d("Fetch", "Checksum valid ✅")
        } else {
            Log.e("Fetch", "Checksum mismatch ❌")
            fetch.retry(download.id)
        }
    }

    override fun onError(
        download: Download,
        error: Error,
        throwable: Throwable?
    ) {
        Log.e("Fetch", "Error: ${error.name}")
    }

    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) = Unit

    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) = Unit

    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) = Unit

    override fun onPaused(download: Download) = Unit

    override fun onResumed(download: Download) = Unit

    override fun onCancelled(download: Download) = Unit

    override fun onRemoved(download: Download) = Unit

    override fun onDeleted(download: Download) = Unit

    private fun validateChecksum(file: String, checksum: String?): Boolean {
        if (checksum == null) return true

        val file = File(file)

        if (!file.exists()) return false

        val actualChecksum = sha256(file.source())

        return actualChecksum.equals(checksum, ignoreCase = true)
    }

    override fun getFetchInstanceForNamespace(namespace: String): Fetch {
        fetchConfig.setNamespace(namespace)
        return Fetch.getInstance(fetchConfig.build()).apply {
            addListener(this@BackgroundDownloadManager)
        }
    }

    override fun getDownloadNotificationTitle(download: Download): String {
        return download.extras.getString(KEY_NAME,  super.getDownloadNotificationTitle(download))
    }

    companion object {
        private const val KEY_CHECKSUM = "checksum"
    }
}
