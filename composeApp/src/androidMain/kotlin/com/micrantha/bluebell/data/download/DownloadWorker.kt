package com.micrantha.bluebell.data.download

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.micrantha.bluebell.app.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File

internal const val KEY_URL = "download_url"
internal const val KEY_FILE_NAME = "file_name"
internal const val KEY_TASK_ID = "task_id"
internal const val KEY_TASK_TAG = "task_tag"

const val KEY_ERROR = "error"
const val KEY_FILE_PATH = "file_path"
const val KEY_PROGRESS = "progress"
const val KEY_BYTES = "bytes"
const val KEY_TOTAL_BYTES = "total_bytes"

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val downloadService by lazy { DownloadService() }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL)
            ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME)
            ?: return@withContext Result.failure()

        try {
            downloadFile(url, fileName)
        } catch (e: Exception) {
            Log.e(tag = TAG, messageString = "Error downloading file", throwable = e)
            Result.failure(
                workDataOf("error" to (e.message ?: "Download failed"))
            )
        } finally {
            downloadService.close()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun downloadFile(
        urlString: String,
        fileName: String,
    ): Result {
        var sink: BufferedSink? = null
        return try {
            val downloadDir = File(applicationContext.filesDir, "downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val file = File(downloadDir, fileName)
            val resumePosition = if (file.exists()) file.length() else 0L

            sink = file.sink().buffer()

            downloadService.downloadFile(
                url = urlString,
                outputSink = sink,
                resumePosition = resumePosition
            ) { _, _, progress ->
                if (isStopped) return@downloadFile

                setProgress(
                    workDataOf(
                        KEY_PROGRESS to progress.toInt()
                    )
                )
            }

            Result.success(
                workDataOf(KEY_FILE_PATH to file.absolutePath)
            )

        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR to (e.message ?: "Download failed"))
            )
        } finally {
            sink?.flush()
            sink?.close()
        }
    }

    companion object {
        const val TAG = "DownloadWorker"
    }
}
