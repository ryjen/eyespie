package com.micrantha.eyespie.sharing

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex

private const val EYESPIE_MIME_TYPE = "application/octet-stream"
private const val READ_BUFFER_SIZE = 8 * 1024

@Composable
fun rememberAndroidGameDocumentTransfer(): GameDocumentTransfer {
    val resolver = LocalContext.current.contentResolver
    val controller = remember(resolver) { AndroidGameDocumentTransfer(resolver) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EYESPIE_MIME_TYPE),
    ) { uri ->
        controller.completeWriteSelection(uri)
    }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        controller.completeReadSelection(uri)
    }

    SideEffect {
        controller.attachLaunchers(
            launchWrite = { suggestedName -> createDocument.launch(suggestedName) },
            launchRead = { openDocument.launch(arrayOf("*/*")) },
        )
    }
    return controller
}

private class AndroidGameDocumentTransfer(
    private val resolver: ContentResolver,
) : GameDocumentTransfer {
    private val operationMutex = Mutex()
    private var launchWrite: ((String) -> Unit)? = null
    private var launchRead: (() -> Unit)? = null
    private var pendingWrite: CompletableDeferred<Uri?>? = null
    private var pendingRead: CompletableDeferred<Uri?>? = null

    fun attachLaunchers(
        launchWrite: (String) -> Unit,
        launchRead: () -> Unit,
    ) {
        this.launchWrite = launchWrite
        this.launchRead = launchRead
    }

    fun completeWriteSelection(uri: Uri?) {
        pendingWrite?.complete(uri)
    }

    fun completeReadSelection(uri: Uri?) {
        pendingRead?.complete(uri)
    }

    override suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameDocumentWriteResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameDocumentWriteResult.TooLarge
        if (!operationMutex.tryLock()) return GameDocumentWriteResult.Busy

        return try {
            val launcher = launchWrite ?: return GameDocumentWriteResult.Failed
            val selection = CompletableDeferred<Uri?>()
            pendingWrite = selection
            launcher(suggestedFileName)
            val uri = selection.await() ?: return GameDocumentWriteResult.Cancelled

            try {
                val output = resolver.openOutputStream(uri, "wt")
                    ?: return GameDocumentWriteResult.Failed
                output.use {
                    it.write(bytes)
                    it.flush()
                }
                GameDocumentWriteResult.Success
            } catch (_: Exception) {
                GameDocumentWriteResult.Failed
            }
        } finally {
            pendingWrite = null
            operationMutex.unlock()
        }
    }

    override suspend fun read(): GameDocumentReadResult {
        if (!operationMutex.tryLock()) return GameDocumentReadResult.Busy

        return try {
            val launcher = launchRead ?: return GameDocumentReadResult.Failed
            val selection = CompletableDeferred<Uri?>()
            pendingRead = selection
            launcher()
            val uri = selection.await() ?: return GameDocumentReadResult.Cancelled
            readBounded(uri)
        } finally {
            pendingRead = null
            operationMutex.unlock()
        }
    }

    private fun readBounded(uri: Uri): GameDocumentReadResult = try {
        val input = resolver.openInputStream(uri) ?: return GameDocumentReadResult.Failed
        input.use {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(READ_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                total += count
                if (total > GAME_BUNDLE_MAX_BYTES) return GameDocumentReadResult.TooLarge
                output.write(buffer, 0, count)
            }
            GameDocumentReadResult.Success(output.toByteArray())
        }
    } catch (_: Exception) {
        GameDocumentReadResult.Failed
    }
}
