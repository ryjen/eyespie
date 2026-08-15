@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.sharing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import platform.Foundation.NSDate
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

class IosGameBundleDocumentGateway(
    private val presenter: () -> UIViewController,
) : GameBundleDocumentGateway {
    private val operationMutex = Mutex()
    private var activeDelegate: PickerDelegate? = null

    override suspend fun export(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameBundleDocumentWriteResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameBundleDocumentWriteResult.TooLarge
        if (!operationMutex.tryLock()) return GameBundleDocumentWriteResult.Failed

        val tempPath = buildTempPath(suggestedFileName)
        return try {
            try {
                FileSystem.SYSTEM.write(tempPath.toPath()) { write(bytes) }
            } catch (_: Exception) {
                return GameBundleDocumentWriteResult.Failed
            }

            val selected = presentPicker(
                UIDocumentPickerViewController(
                    forExportingURLs = listOf(NSURL.fileURLWithPath(tempPath)),
                    asCopy = true,
                ),
            )
            if (selected == null) GameBundleDocumentWriteResult.Cancelled
            else GameBundleDocumentWriteResult.Success
        } finally {
            runCatching { FileSystem.SYSTEM.delete(tempPath.toPath(), mustExist = false) }
            operationMutex.unlock()
        }
    }

    override suspend fun import(): GameBundleDocumentReadResult {
        if (!operationMutex.tryLock()) return GameBundleDocumentReadResult.Failed

        return try {
            val selected = presentPicker(
                UIDocumentPickerViewController(
                    documentTypes = listOf("public.data"),
                    inMode = UIDocumentPickerModeImport,
                ),
            ) ?: return GameBundleDocumentReadResult.Cancelled

            withContext(Dispatchers.Default) {
                readBounded(selected)
            }
        } finally {
            operationMutex.unlock()
        }
    }

    private suspend fun presentPicker(picker: UIDocumentPickerViewController): NSURL? {
        val deferred = CompletableDeferred<NSURL?>()
        val delegate = PickerDelegate(deferred)
        activeDelegate = delegate
        picker.delegate = delegate

        try {
            withContext(Dispatchers.Main.immediate) {
                presenter().presentViewController(picker, animated = true, completion = null)
            }
            return deferred.await()
        } finally {
            activeDelegate = null
        }
    }

    private fun readBounded(url: NSURL): GameBundleDocumentReadResult {
        val path = url.path ?: return GameBundleDocumentReadResult.Failed
        val scoped = url.startAccessingSecurityScopedResource()
        return try {
            val source = try {
                FileSystem.SYSTEM.source(path.toPath()).buffer()
            } catch (_: Exception) {
                return GameBundleDocumentReadResult.Failed
            }

            source.use {
                val output = Buffer()
                var total = 0L
                while (!source.exhausted()) {
                    val read = source.read(output, 8_192L)
                    if (read < 0L) break
                    total += read
                    if (total > GAME_BUNDLE_MAX_BYTES.toLong()) {
                        return GameBundleDocumentReadResult.TooLarge
                    }
                }
                val bytes = output.readByteArray()
                if (bytes.isEmpty()) GameBundleDocumentReadResult.Failed
                else GameBundleDocumentReadResult.Success(bytes)
            }
        } catch (_: Exception) {
            GameBundleDocumentReadResult.Failed
        } finally {
            if (scoped) url.stopAccessingSecurityScopedResource()
        }
    }

    private fun buildTempPath(suggestedFileName: String): String {
        val safeName = suggestedFileName
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
            .take(96)
            .ifBlank { "eyespie-game.$EYESPIE_FILE_EXTENSION" }
        return "${NSTemporaryDirectory()}eyespie-${NSDate().timeIntervalSince1970}-$safeName"
    }
}

private class PickerDelegate(
    private val deferred: CompletableDeferred<NSURL?>,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        deferred.complete(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        deferred.complete(null)
    }
}
