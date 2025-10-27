package com.micrantha.bluebell.platform

import android.content.Context
import android.util.Log
import com.micrantha.bluebell.data.DownloadState
import com.micrantha.eyespie.BuildConfig
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Path

actual class BackgroundDownloader(
    private val context: Context,
    private val platform: Platform,
    private val namespace: String = "background-downloads"
) : AbstractFetchListener() {

    private val notificationManager by lazy { NotificationManager() }

    private val fetchConfig by lazy {
        FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .setNamespace(namespace)
            .enableFileExistChecks(true)
            .enableHashCheck(true)
            .setNotificationManager(notificationManager)
            .enableLogging(BuildConfig.DEBUG)
            .build()
    }

    private val fetch by lazy {
        Fetch.getInstance(fetchConfig).apply {
            addListener(this@BackgroundDownloader)
        }
    }

    actual suspend fun startDownload(
        id: Long,
        name: String?,
        url: String,
        filePath: Path,
        tag: String?,
    ): Result<Unit> {

        val data = mutableMapOf(
            KEY_FILE to filePath.toString(),
        )

        name?.let { data[KEY_NAME] = it }

        val request = Request(
            url = url,
            file = filePath.toString(),
        ).apply {
            this.tag = tag
            this.identifier = id
            priority = Priority.HIGH
            extras = Extras(data)

            if (name == null) {
                addHeader("Fetch-Notification", "false")
            }
        }

        fetch.addListener(this)

        return suspendCancellableCoroutine { cont ->
            fetch.enqueue(request = request, func = {
                Log.d(TAG, "Download enqueued ✅")
                cont.resume(Result.success(Unit)) { cause, _, _ ->
                    cont.cancel(cause)
                }
            }) {
                Log.e(TAG, "Download failed $it ❌", it.throwable)
                cont.cancel(it.throwable)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    actual fun observe(): Flow<DownloadState> = callbackFlow {
        val listener = object : AbstractFetchListener() {
            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                trySend(DownloadState.Queued(download.identifier))
            }

            override fun onStarted(
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int
            ) {
                trySend(DownloadState.Started(download.identifier))
            }

            override fun onCompleted(download: Download) {
                trySend(DownloadState.Completed(download.identifier))
            }

            override fun onProgress(
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long
            ) {
                trySend(DownloadState.Progress(
                    download.identifier,
                    download.progress,
                    download.downloaded,
                    download.total,
                    etaInMilliSeconds,
                    downloadedBytesPerSecond
                ))
            }

            override fun onError(download: Download, error: com.tonyodev.fetch2.Error, throwable: Throwable?) {
                trySend(DownloadState.Failed(download.identifier, Error(error.name, error.throwable), throwable))
            }

            override fun onPaused(download: Download) {
                trySend(DownloadState.Paused(download.identifier))
            }

            override fun onResumed(download: Download) {
                trySend(DownloadState.Resumed(download.identifier))
            }

            override fun onCancelled(download: Download) {
                trySend(DownloadState.Cancelled(download.identifier))
            }

            override fun onRemoved(download: Download) {
                trySend(DownloadState.Removed(download.identifier))
            }

            override fun onWaitingNetwork(download: Download) {
                trySend(DownloadState.WaitingNetwork(download.identifier))
            }

            override fun onDeleted(download: Download) {
                trySend(DownloadState.Deleted(download.identifier))
            }

            override fun onAdded(download: Download) {
                trySend(DownloadState.Added(download.identifier))
            }
        }
        fetch.addListener(listener)

        awaitClose {
            fetch.removeListener(listener)
        }
    }.shareIn(
        scope = GlobalScope,
        started = SharingStarted.Lazily,
        replay = 0
    )

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
        private const val KEY_VALID = "valid"
        private const val KEY_NAME = "name"
        private const val KEY_FILE = "file"
    }
}
