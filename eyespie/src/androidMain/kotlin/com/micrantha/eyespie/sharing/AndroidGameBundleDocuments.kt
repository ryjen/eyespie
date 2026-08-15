package com.micrantha.eyespie.sharing

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@Composable
fun rememberAndroidGameBundleDocumentGateway(): GameBundleDocumentGateway {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) { AndroidGameBundleDocumentGateway(context) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EYESPIE_MIME_TYPE),
    ) { uri ->
        controller.completeExportPicker(uri)
    }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        controller.completeImportPicker(uri)
    }

    controller.bindLaunchers(createDocument, openDocument)
    return controller
}

private class AndroidGameBundleDocumentGateway(
    private val context: Context,
) : GameBundleDocumentGateway {
    private val operationMutex = Mutex()
    private var createDocumentLauncher: ManagedActivityResultLauncher<String, Uri?>? = null
    private var openDocumentLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>? = null
    private var pendingExportUri: CompletableDeferred<Uri?>? = null
    private var pendingImportUri: CompletableDeferred<Uri?>? = null

    fun bindLaunchers(
        createDocument: ManagedActivityResultLauncher<String, Uri?>,
        openDocument: ManagedActivityResultLauncher<Array<String>, Uri?>,
    ) {
        createDocumentLauncher = createDocument
        openDocumentLauncher = openDocument
    }

    fun completeExportPicker(uri: Uri?) {
        pendingExportUri?.complete(uri)
    }

    fun completeImportPicker(uri: Uri?) {
        pendingImportUri?.complete(uri)
    }

    override suspend fun export(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameBundleDocumentWriteResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameBundleDocumentWriteResult.TooLarge
        if (!operationMutex.tryLock()) return GameBundleDocumentWriteResult.Failed

        return try {
            val launcher = createDocumentLauncher ?: return GameBundleDocumentWriteResult.Failed
            val picker = CompletableDeferred<Uri?>()
            pendingExportUri = picker
            withContext(Dispatchers.Main.immediate) {
                launcher.launch(suggestedFileName)
            }
            val uri = picker.await() ?: return GameBundleDocumentWriteResult.Cancelled
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: return@withContext GameBundleDocumentWriteResult.Failed
                    GameBundleDocumentWriteResult.Success
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    GameBundleDocumentWriteResult.Failed
                }
            }
        } finally {
            pendingExportUri = null
            operationMutex.unlock()
        }
    }

    override suspend fun import(): GameBundleDocumentReadResult {
        if (!operationMutex.tryLock()) return GameBundleDocumentReadResult.Failed

        return try {
            val launcher = openDocumentLauncher ?: return GameBundleDocumentReadResult.Failed
            val picker = CompletableDeferred<Uri?>()
            pendingImportUri = picker
            withContext(Dispatchers.Main.immediate) {
                launcher.launch(arrayOf(EYESPIE_MIME_TYPE, "application/octet-stream"))
            }
            val uri = picker.await() ?: return GameBundleDocumentReadResult.Cancelled
            withContext(Dispatchers.IO) {
                readBounded(uri)
            }
        } finally {
            pendingImportUri = null
            operationMutex.unlock()
        }
    }

    private fun readBounded(uri: Uri): GameBundleDocumentReadResult {
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                val length = descriptor.length
                if (length > GAME_BUNDLE_MAX_BYTES.toLong()) {
                    return GameBundleDocumentReadResult.TooLarge
                }
            }

            val input = context.contentResolver.openInputStream(uri)
                ?: return GameBundleDocumentReadResult.Failed
            input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > GAME_BUNDLE_MAX_BYTES) {
                        return GameBundleDocumentReadResult.TooLarge
                    }
                    output.write(buffer, 0, read)
                }
                val bytes = output.toByteArray()
                if (bytes.isEmpty()) return GameBundleDocumentReadResult.Failed
                return GameBundleDocumentReadResult.Success(bytes)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return GameBundleDocumentReadResult.Failed
        }
    }
}
