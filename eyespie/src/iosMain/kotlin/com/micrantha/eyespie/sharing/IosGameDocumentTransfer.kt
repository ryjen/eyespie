package com.micrantha.eyespie.sharing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

private const val IOS_DOCUMENT_READ_CHUNK_BYTES = 8 * 1024L
private const val IOS_DATA_UTI = "public.data"

class IosGameDocumentTransfer(
    private val presenter: () -> UIViewController?,
) : NSObject(), GameDocumentTransfer, UIDocumentPickerDelegateProtocol {
    private val operationMutex = Mutex()
    private var pendingSelection: CompletableDeferred<NSURL?>? = null
    private var activePicker: UIDocumentPickerViewController? = null

    override suspend fun read(): GameDocumentReadResult {
        if (pendingSelection != null) return GameDocumentReadResult.Busy
        if (!operationMutex.tryLock()) return GameDocumentReadResult.Busy

        val selection = CompletableDeferred<NSURL?>()
        return try {
            val presenter = presenter() ?: return GameDocumentReadResult.Failed
            val picker = UIDocumentPickerViewController(
                documentTypes = listOf(IOS_DATA_UTI),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeOpen,
            )
            picker.delegate = this
            pendingSelection = selection
            activePicker = picker
            presenter.presentViewController(picker, animated = true, completion = null)

            val url = selection.await() ?: return GameDocumentReadResult.Cancelled
            withContext(Dispatchers.Default) { readBounded(url) }
        } catch (cancelled: CancellationException) {
            abandonSelection(selection)
            throw cancelled
        } finally {
            clearCompletedSelection(selection)
            operationMutex.unlock()
        }
    }

    override suspend fun write(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameDocumentWriteResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameDocumentWriteResult.TooLarge
        if (pendingSelection != null) return GameDocumentWriteResult.Busy
        if (!operationMutex.tryLock()) return GameDocumentWriteResult.Busy

        val selection = CompletableDeferred<NSURL?>()
        var temporaryPath: okio.Path? = null
        return try {
            val presenter = presenter() ?: return GameDocumentWriteResult.Failed
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
            if (!wrote) return GameDocumentWriteResult.Failed

            val sourceUrl = NSURL.fileURLWithPath(path.toString())
            val picker = UIDocumentPickerViewController(
                URL = sourceUrl,
                inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService,
            )
            picker.delegate = this
            pendingSelection = selection
            activePicker = picker
            presenter.presentViewController(picker, animated = true, completion = null)

            if (selection.await() == null) {
                GameDocumentWriteResult.Cancelled
            } else {
                GameDocumentWriteResult.Success
            }
        } catch (cancelled: CancellationException) {
            abandonSelection(selection)
            throw cancelled
        } finally {
            clearCompletedSelection(selection)
            temporaryPath?.let { path ->
                withContext(NonCancellable + Dispatchers.Default) {
                    try {
                        FileSystem.SYSTEM.delete(path, mustExist = false)
                    } catch (_: Exception) {
                        // Best-effort cleanup of app-private temporary export material.
                    }
                }
            }
            operationMutex.unlock()
        }
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentAtURL: NSURL,
    ) {
        completeSelection(didPickDocumentAtURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        completeSelection(null)
    }

    private fun completeSelection(url: NSURL?) {
        val pending = pendingSelection ?: return
        pending.complete(url)
        if (pendingSelection === pending) pendingSelection = null
        activePicker = null
    }

    private fun abandonSelection(selection: CompletableDeferred<NSURL?>) {
        if (pendingSelection !== selection) return
        activePicker?.delegate = null
        activePicker?.dismissViewControllerAnimated(true, completion = null)
        pendingSelection = null
        activePicker = null
    }

    private fun clearCompletedSelection(selection: CompletableDeferred<NSURL?>) {
        if (pendingSelection === selection && selection.isCompleted) {
            pendingSelection = null
            activePicker = null
        }
    }

    private fun readBounded(url: NSURL): GameDocumentReadResult {
        val accessedSecurityScope = url.startAccessingSecurityScopedResource()
        return try {
            val rawPath = url.path ?: return GameDocumentReadResult.Failed
            val path = rawPath.toPath()
            FileSystem.SYSTEM.source(path).buffer().use { source ->
                val sink = Buffer()
                var total = 0L
                while (true) {
                    val read = source.read(sink, IOS_DOCUMENT_READ_CHUNK_BYTES)
                    if (read == -1L) break
                    total += read
                    if (total > GAME_BUNDLE_MAX_BYTES.toLong()) {
                        return GameDocumentReadResult.TooLarge
                    }
                }
                GameDocumentReadResult.Success(sink.readByteArray())
            }
        } catch (_: Exception) {
            GameDocumentReadResult.Failed
        } finally {
            if (accessedSecurityScope) url.stopAccessingSecurityScopedResource()
        }
    }

    private fun temporaryExportPath(suggestedFileName: String): okio.Path {
        val leaf = suggestedFileName.substringAfterLast('/').substringAfterLast('\\')
        val safeLeaf = if (leaf.endsWith(".eyespie")) leaf else "$leaf.eyespie"
        return "${NSTemporaryDirectory()}${NSUUID().UUIDString}-$safeLeaf".toPath()
    }
}
