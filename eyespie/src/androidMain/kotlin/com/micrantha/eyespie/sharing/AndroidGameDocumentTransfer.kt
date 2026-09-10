package com.micrantha.eyespie.sharing

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import java.io.ByteArrayOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

const val EYESPIE_ANDROID_MIME_TYPE = "application/vnd.micrantha.eyespie"
private const val READ_BUFFER_SIZE = 8 * 1024

@Composable
fun rememberAndroidGameDocumentTransfer(
    controller: AndroidGameDocumentTransfer,
): GameDocumentTransfer {
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EYESPIE_ANDROID_MIME_TYPE),
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

class AndroidGameDocumentTransfer(
    private val resolver: ContentResolver,
) : GameDocumentTransfer, ExternalGameDocumentSource {
    private val operationMutex = Mutex()
    private val pendingExternalState = MutableStateFlow(false)
    override val pending: StateFlow<Boolean> = pendingExternalState.asStateFlow()

    private var launchWrite: ((String) -> Unit)? = null
    private var launchRead: (() -> Unit)? = null
    private var pendingWrite: CompletableDeferred<Uri?>? = null
    private var pendingRead: CompletableDeferred<Uri?>? = null
    private var pendingExternalUri: Uri? = null

    fun attachLaunchers(
        launchWrite: (String) -> Unit,
        launchRead: () -> Unit,
    ) {
        this.launchWrite = launchWrite
        this.launchRead = launchRead
    }

    /**
     * Queue one OS-owned document handoff for the normal bounded import path.
     *
     * The URI itself remains platform-local. A second handoff is rejected while another document
     * operation is active rather than replacing or widening authority silently.
     */
    fun offerExternalDocument(uri: Uri): Boolean {
        if (pendingWrite != null || pendingRead != null || pendingExternalUri != null || operationMutex.isLocked) {
            return false
        }
        pendingExternalUri = uri
        pendingExternalState.value = true
        return true
    }

    /** Platform-only recreation token; never crosses into common navigation or MVI state. */
    fun pendingExternalDocumentState(): String? = pendingExternalUri?.toString()

    override fun acknowledgePendingDocument() {
        if (pendingExternalUri == null) return
        pendingExternalUri = null
        pendingExternalState.value = false
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
        if (pendingWrite != null || pendingRead != null || pendingExternalUri != null) {
            return GameDocumentWriteResult.Busy
        }
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
        val externalUri = pendingExternalUri
        if (externalUri != null) {
            if (!operationMutex.tryLock()) return GameDocumentReadResult.Busy
            return try {
                // Do not clear the platform handoff here. Common import preparation acknowledges it
                // only after verification reaches a stable preview or terminal result. If this
                // coroutine is cancelled, the URI remains retryable.
                readBounded(externalUri)
            } finally {
                operationMutex.unlock()
            }
        }

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
        } catch (cancelled: CancellationException) {
            throw cancelled
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GameDocumentReadResult.Failed
        }
    }
}
