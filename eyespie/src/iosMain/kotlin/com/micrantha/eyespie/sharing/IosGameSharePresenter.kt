package com.micrantha.eyespie.sharing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController

class IosGameSharePresenter(
    private val presenter: () -> UIViewController?,
) : GameSharePresenter {
    private var activeController: UIActivityViewController? = null

    override suspend fun present(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameSharePresentationResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) return GameSharePresentationResult.TooLarge
        if (activeController != null) return GameSharePresentationResult.Busy

        val host = presenter() ?: return GameSharePresentationResult.Failed
        if (host.presentedViewController != null) return GameSharePresentationResult.Busy

        val path = temporaryExportPath(suggestedFileName)
        val wrote = try {
            withContext(Dispatchers.Default) {
                FileSystem.SYSTEM.write(path) { write(bytes) }
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (!wrote) return GameSharePresentationResult.Failed

        val fileUrl = NSURL.fileURLWithPath(path.toString())
        val controller = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null,
        )
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = host.view
            popover.sourceRect = host.view.bounds
        }
        controller.completionWithItemsHandler = { _, _, _, _ ->
            complete(controller, path)
        }

        activeController = controller
        host.presentViewController(controller, animated = true, completion = null)
        return GameSharePresentationResult.Presented
    }

    private fun complete(
        controller: UIActivityViewController,
        path: Path,
    ) {
        if (activeController !== controller) return
        controller.completionWithItemsHandler = null
        activeController = null
        try {
            FileSystem.SYSTEM.delete(path, mustExist = false)
        } catch (_: Exception) {
            // Best-effort cleanup after UIKit has finished using the app-private artifact.
        }
    }

    private fun temporaryExportPath(suggestedFileName: String): Path {
        val leaf = suggestedFileName.substringAfterLast('/').substringAfterLast('\\')
        val safeLeaf = if (leaf.endsWith(".eyespie")) leaf else "$leaf.eyespie"
        return "${NSTemporaryDirectory()}${NSUUID().UUIDString}-$safeLeaf".toPath()
    }
}
