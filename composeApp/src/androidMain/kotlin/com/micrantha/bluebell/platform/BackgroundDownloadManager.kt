package com.micrantha.bluebell.platform

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.micrantha.bluebell.data.download.DownloadStatus
import com.micrantha.bluebell.data.download.DownloadTask
import com.micrantha.bluebell.data.download.DownloadTaskManager
import com.micrantha.bluebell.data.download.DownloadWorker
import com.micrantha.bluebell.data.download.KEY_BYTES
import com.micrantha.bluebell.data.download.KEY_ERROR
import com.micrantha.bluebell.data.download.KEY_FILE_NAME
import com.micrantha.bluebell.data.download.KEY_FILE_PATH
import com.micrantha.bluebell.data.download.KEY_PROGRESS
import com.micrantha.bluebell.data.download.KEY_TOTAL_BYTES
import com.micrantha.bluebell.data.download.KEY_URL
import java.util.UUID

class BackgroundDownloadManager(
    context: Context,
    private val taskManager: DownloadTaskManager
): BackgroundDownloader {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun startDownload(tag: String, url: String, fileName: String) {
        val id = enqueueDownloadTask(tag, url, fileName)
        taskManager[tag] = DownloadTask(
            id = id.toString(),
            url = url,
            fileName = fileName,
            status = DownloadStatus.PENDING
        )
        workManager.getWorkInfoByIdLiveData(id)
            .observeWorkProgress(tag)
    }

    private fun enqueueDownloadTask(tag: String, url: String, fileName: String): UUID {

        val downloadData = workDataOf(
            KEY_URL to url,
            KEY_FILE_NAME to fileName
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(downloadData)
            .setConstraints(constraints)
            .addTag(tag)
            .build()

        workManager.enqueue(downloadRequest)

        return downloadRequest.id
    }

    override fun cancelDownload(tag: String) {
        workManager.cancelAllWorkByTag(tag)
        taskManager.update(tag) {
            it.copy(status = DownloadStatus.CANCELLED)
        }
    }

    override fun pauseDownload(tag: String) {
        workManager.cancelAllWorkByTag(tag)
        taskManager.update(tag) {
            it.copy(status = DownloadStatus.PAUSED)
        }
    }

    override fun resumeDownload(tag: String, url: String, fileName: String) {
        taskManager.update(tag) {
            if (it.status == DownloadStatus.PAUSED) {
                enqueueDownloadTask(tag, url, fileName)
                it.copy(status = DownloadStatus.DOWNLOADING)
            } else {
                it
            }
        }
    }

    private fun LiveData<WorkInfo?>.observeWorkProgress(tag: String) {
        observeForever { workInfos ->
            workInfos?.let { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getFloat(KEY_PROGRESS, 0f)
                        val bytesDownloaded = workInfo.progress.getLong(KEY_BYTES, 0L)
                        val totalBytes = workInfo.progress.getLong(KEY_TOTAL_BYTES, 0L)
                        taskManager.update(tag) {
                            it.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes
                            )
                        }
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        workInfo.outputData.getString(KEY_FILE_PATH)
                        taskManager.update(tag) {
                            it.copy(
                                status = DownloadStatus.COMPLETED,
                                progress = 1.0f
                            )
                        }
                    }

                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(KEY_ERROR)
                        taskManager.update(tag) {
                            it.copy(
                                status = DownloadStatus.FAILED,
                                error = error
                            )
                        }
                    }

                    WorkInfo.State.CANCELLED -> {
                        taskManager.update(tag) {
                            it.copy(
                                status = DownloadStatus.CANCELLED
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
