package com.micrantha.eyespie.telemetry

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@Composable
fun rememberAndroidDiagnosticArtifactWriter(
    controller: AndroidDiagnosticArtifactWriter,
): DiagnosticArtifactWriter {
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DIAGNOSTIC_ARTIFACT_MIME_TYPE),
    ) { uri ->
        controller.completeWriteSelection(uri)
    }

    SideEffect {
        controller.attachLauncher { suggestedName -> createDocument.launch(suggestedName) }
    }
    return controller
}

class AndroidDiagnosticArtifactWriter(
    private val resolver: ContentResolver,
) : DiagnosticArtifactWriter {
    private val operationMutex = Mutex()
    private var launchWrite: ((String) -> Unit)? = null
    private var pendingWrite: CompletableDeferred<Uri?>? = null

    fun attachLauncher(launchWrite: (String) -> Unit) {
        this.launchWrite = launchWrite
    }

    fun completeWriteSelection(uri: Uri?) {
        pendingWrite?.complete(uri)
    }

    override suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): DiagnosticArtifactWriteResult {
        if (bytes.size > DiagnosticExportEnvelope.MAX_BYTES) {
            return DiagnosticArtifactWriteResult.TooLarge
        }
        if (pendingWrite != null || !operationMutex.tryLock()) {
            return DiagnosticArtifactWriteResult.Busy
        }

        val selection = CompletableDeferred<Uri?>()
        return try {
            val launcher = launchWrite ?: return DiagnosticArtifactWriteResult.Failed
            pendingWrite = selection
            launcher(suggestedFileName)
            val uri = selection.await() ?: return DiagnosticArtifactWriteResult.Cancelled
            writeBounded(uri, bytes)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DiagnosticArtifactWriteResult.Failed
        } finally {
            if (pendingWrite === selection) pendingWrite = null
            operationMutex.unlock()
        }
    }

    private suspend fun writeBounded(
        uri: Uri,
        bytes: ByteArray,
    ): DiagnosticArtifactWriteResult = withContext(Dispatchers.IO) {
        try {
            val output = resolver.openOutputStream(uri, "rwt")
                ?: return@withContext DiagnosticArtifactWriteResult.Failed
            output.use {
                it.write(bytes)
                it.flush()
            }
            DiagnosticArtifactWriteResult.Success
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DiagnosticArtifactWriteResult.Failed
        }
    }
}
