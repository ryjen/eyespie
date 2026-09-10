package com.micrantha.eyespie.sharing

import android.content.ClipData
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private const val SHARE_CACHE_DIRECTORY = "eyespie-shares"
private const val SHARE_PROVIDER_SUFFIX = ".eyespie-share"

class AndroidGameSharePresenter(
    private val activity: ComponentActivity,
) : GameSharePresenter {
    private val operationMutex = Mutex()

    override suspend fun present(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameSharePresentationResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameSharePresentationResult.TooLarge
        if (!operationMutex.tryLock()) return GameSharePresentationResult.Busy

        return try {
            val file = withContext(Dispatchers.IO) {
                writeShareFile(suggestedFileName, bytes)
            } ?: return GameSharePresentationResult.Failed

            withContext(Dispatchers.Main.immediate) {
                val uri = FileProvider.getUriForFile(
                    activity,
                    activity.packageName + SHARE_PROVIDER_SUFFIX,
                    file,
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = EYESPIE_ANDROID_MIME_TYPE
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(activity.contentResolver, file.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivity(Intent.createChooser(sendIntent, null))
            }
            GameSharePresentationResult.Presented
        } catch (_: Exception) {
            GameSharePresentationResult.Failed
        } finally {
            operationMutex.unlock()
        }
    }

    private fun writeShareFile(suggestedFileName: String, bytes: ByteArray): File? {
        return try {
            val directory = File(activity.cacheDir, SHARE_CACHE_DIRECTORY)
            if (!directory.exists() && !directory.mkdirs()) {
                null
            } else {
                directory.listFiles()?.forEach { previous ->
                    runCatching { previous.delete() }
                }

                val leaf = suggestedFileName
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .take(96)
                    .ifBlank { "eyespie-game.eyespie" }
                val safeLeaf = if (leaf.endsWith(".eyespie", ignoreCase = true)) leaf else "$leaf.eyespie"
                File(directory, safeLeaf).apply {
                    outputStream().use { output ->
                        output.write(bytes)
                        output.flush()
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
