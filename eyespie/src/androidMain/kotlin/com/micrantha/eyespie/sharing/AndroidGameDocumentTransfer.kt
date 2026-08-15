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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

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
        val pending = pendingWrite ?: return
        pending.complete(uri)
        if (pendingWrite === pending) pendingWrite = null
    }

    fun completeReadSelection(uri: Uri?) {
        val pending = pendingRead ?: return
        pending.complete(uri)
        if (pendingRead === pending) pendingRead = null
    }

    override suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameDocumentWriteResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameDocumentWriteResult.TooLarge
        if (pendingWrite != null || pendingRead != null) return GameDocumentWriteResult.Busy
        if (!operationMutex.tryLock()) return GameDocumentWriteResult.Busy

        val selection = CompletableDeferred<Uri?>()
        return try {
            val launcher = launchWrite ?: return GameDocumentWriteResult.Failed
            pendingWrite = selection
            launcher(suggestedFileName)
            val uri = selection.await() ?: return GameDocumentWriteResult.Cancelled
            writeBounded(uri, bytes)
        } finally {
            if (pendingWrite === selection && selection.isCompleted) pendingWrite = null
            operationMutex.unlock()
        }
    }

    override suspend fun read(): GameDocumentReadResult {
        if (pendingWrite != null || pendingRead != null) return GameDocumentReadResult.Busy
        if (!operationMutex.tryLock()) return GameDocumentReadResult.Busy

        val selection = CompletableDeferred<Uri?>()
        return try {
            val launcher = launchRead ?: return GameDocumentReadResult.Failed
            pendingRead = selection
            launcher()
            val uri = selection.await() ?: return GameDocumentReadResult.Cancelled
            readBounded(uri)
        } finally {
            if (pendingRead === selection && selection.isCompleted) pendingRead = null
            operationMutex.unlock()
        }
    }

    private suspend fun writeBounded(
        uri: Uri,
        bytes: ByteArray,
    ): GameDocumentWriteResult = withContext(Dispatchers.IO) {
        try {
            val output = resolver.openOutputStream(uri, "rwt")
                ?: return@withContext GameDocumentWriteResult.Failed
            output.use {
                it.write(bytes)
                it.flush()
            }
            GameDocumentWriteResult.Success
        } catch (_: Exception) {
            GameDocumentWriteResult.Failed
        }
    }

    private suspend fun readBounded(uri: Uri): GameDocumentReadResult = withContext(Dispatchers.IO) {
        try {
            val input = resolver.openInputStream(uri)
                ?: return@withContext GameDocumentReadResult.Failed
            input.use {
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(READ_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > GAME_BUNDLE_MAX_BYTES) {
                        return@withContext GameDocumentReadResult.TooLarge
                    }
                    output.write(buffer, 0, count)
                }
                GameDocumentReadResult.Success(output.toByteArray())
            }
        } catch (_: Exception) {
            GameDocumentReadResult.Failed
        }
    }
}
