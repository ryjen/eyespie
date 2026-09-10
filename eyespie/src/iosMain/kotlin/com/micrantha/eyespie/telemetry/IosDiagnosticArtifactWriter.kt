package com.micrantha.eyespie.telemetry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

class IosDiagnosticArtifactWriter(
    private val presenter: () -> UIViewController?,
) : DiagnosticArtifactWriter {
    private val operationMutex = Mutex()
    private var pendingSelection: CompletableDeferred<NSURL?>? = null
    private var activePicker: UIDocumentPickerViewController? = null
    private val pickerDelegate = IosDiagnosticPickerDelegate(::completeSelection)

    override suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): DiagnosticArtifactWriteResult {
        if (bytes.size > DiagnosticExportEnvelope.MAX_BYTES) {
            return DiagnosticArtifactWriteResult.TooLarge
        }
        if (pendingSelection != null || !operationMutex.tryLock()) {
            return DiagnosticArtifactWriteResult.Busy
        }

        val selection = CompletableDeferred<NSURL?>()
        var temporaryPath: okio.Path? = null
        return try {
            val presenter = presenter() ?: return DiagnosticArtifactWriteResult.Failed
            val path = temporaryExportPath(suggestedFileName)
            temporaryPath = path
            val wrote = withContext(Dispatchers.Default) {
                try {
                    FileSystem.SYSTEM.write(path) { write(bytes) }
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (!wrote) return DiagnosticArtifactWriteResult.Failed

            val sourceUrl = NSURL.fileURLWithPath(path.toString())
            val picker = UIDocumentPickerViewController(
                uRL = sourceUrl,
                inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService,
            )
            picker.delegate = pickerDelegate
            pendingSelection = selection
            activePicker = picker
            presenter.presentViewController(picker, animated = true, completion = null)

            if (selection.await() == null) {
                DiagnosticArtifactWriteResult.Cancelled
            } else {
                DiagnosticArtifactWriteResult.Success
            }
        } catch (cancelled: CancellationException) {
            abandonSelection(selection)
            throw cancelled
        } catch (_: Exception) {
            DiagnosticArtifactWriteResult.Failed
        } finally {
            clearSelection(selection)
            temporaryPath?.let { path ->
                withContext(NonCancellable + Dispatchers.Default) {
                    try {
                        FileSystem.SYSTEM.delete(path, mustExist = false)
                    } catch (_: Exception) {
                        // Best-effort cleanup of app-private diagnostics export material.
                    }
                }
            }
            operationMutex.unlock()
        }
    }

    private fun completeSelection(url: NSURL?) {
        pendingSelection?.complete(url)
    }

    private fun abandonSelection(selection: CompletableDeferred<NSURL?>) {
        if (pendingSelection !== selection) return
        activePicker?.delegate = null
        activePicker?.dismissViewControllerAnimated(true, completion = null)
        pendingSelection = null
        activePicker = null
    }

    private fun clearSelection(selection: CompletableDeferred<NSURL?>) {
        if (pendingSelection === selection) pendingSelection = null
        activePicker = null
    }

    private fun temporaryExportPath(suggestedFileName: String): okio.Path {
        val leaf = suggestedFileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .take(96)
            .ifBlank { DIAGNOSTIC_ARTIFACT_FILE_NAME }
        val safeLeaf = if (leaf.endsWith(".json", ignoreCase = true)) leaf else "$leaf.json"
        return "${NSTemporaryDirectory()}${NSUUID().UUIDString}-$safeLeaf".toPath()
    }
}

private class IosDiagnosticPickerDelegate(
    private val onSelection: (NSURL?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentAtURL: NSURL,
    ) {
        onSelection(didPickDocumentAtURL)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onSelection(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onSelection(null)
    }
}
