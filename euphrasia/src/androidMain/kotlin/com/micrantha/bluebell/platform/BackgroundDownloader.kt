package com.micrantha.bluebell.platform

import android.content.Context
import android.util.Log
import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.eyespie.BuildConfig
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.Extras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okio.source
import java.io.File

actual class BackgroundDownloader(
    private val context: Context,
    private val platform: Platform,
    private val namespace: String = "background-downloads"
) : AbstractFetchListener() {

    private val notificationManager by lazy { NotificationManager() }

    private val completed by lazy { MutableSharedFlow<DownloadData>() }

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    actual fun completed(): Flow<DownloadData> = completed

    private val fetchConfig by lazy {
        FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .setNamespace(namespace)
            .setNotificationManager(notificationManager)
            .enableLogging(BuildConfig.DEBUG)
            .build()
    }

    private val fetch by lazy {
        Fetch.getInstance(fetchConfig).apply {
            addListener(this@BackgroundDownloader)
        }
    }

    actual fun startDownload(
        tag: String,
        name: String,
        url: String,
        checksum: String?
    ): String {
        val data = mutableMapOf(
            KEY_NAME to name,
        )

        checksum?.let { data[KEY_CHECKSUM] = it }

        val fileName = sha256(url)

        val filePath = platform.filesPath().resolve(fileName)

        val request = Request(
            url = url,
            file = filePath.toString()
        ).apply {
            this.tag = tag
            priority = Priority.HIGH
            groupId = tag.hashCode()
            extras = Extras(data)
        }

        fetch.enqueue(request = request, func = {
            Log.d(TAG, "Download enqueued ✅")
        }) {
            Log.e(TAG, "Download failed $it ❌", it.throwable)
        }
        return fileName
    }

    override fun onCompleted(download: Download) {

        scope.launch {
            completed.emit(DownloadData(
                download.tag!!,
                download.extras.map[KEY_NAME]!!,
                download.file
            ))
        }

        // If configuration provided a checksum, validate it
        // using sha-256 hash.  Fetch already validates md5 if the server supports
        val expectedChecksum = download.extras.map[KEY_CHECKSUM] ?: return
        val isValid = validateChecksum(download.file, expectedChecksum)
        if (!isValid) {
            Log.e(TAG, "Checksum mismatch ❌")
            fetch.retry(download.id)
        } else {
            notificationManager.cancelNotification(download.id)
        }
    }

    private fun validateChecksum(file: String, checksum: String): Boolean {
        val file = File(file)

        if (!file.exists()) return false

        val actualChecksum = sha256(file.source())

        return actualChecksum.equals(checksum, ignoreCase = true)
    }

    private inner class NotificationManager : DefaultFetchNotificationManager(context) {

        override fun getFetchInstanceForNamespace(namespace: String): Fetch {
            if (fetchConfig.namespace != namespace) {
                throw IllegalStateException("Namespace mismatch")
            }
            return Fetch.getInstance(fetchConfig).apply {
                addListener(this@BackgroundDownloader)
            }
        }

        override fun getDownloadNotificationTitle(download: Download): String {
            return download.extras.getString(
                KEY_NAME,
                super.getDownloadNotificationTitle(download)
            )
        }
    }

    companion object {
        private const val TAG = "BackgroundDownloadManager"
        private const val KEY_CHECKSUM = "checksum"
        private const val KEY_NAME = "name"
        private const val KEY_GZIP = "gzip"
    }
}
