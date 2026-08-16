package com.micrantha.eyespie.imaging

import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle

internal data class IosImageEmbeddingModel(
    val path: String,
    val sha256: String,
)

internal fun loadIosImageEmbeddingModel(): IosImageEmbeddingModel {
    val extension = IMAGE_EMBEDDER_MODEL_FILE.substringAfterLast('.')
    val baseName = IMAGE_EMBEDDER_MODEL_FILE.removeSuffix(".$extension")
    val path = NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
        ?: throw IllegalStateException("image embedder model resource is unavailable")
    val digest = FileSystem.SYSTEM.read(path.toPath()) {
        readByteArray().toByteString().sha256().hex()
    }
    check(digest == IMAGE_EMBEDDER_MODEL_SHA256) {
        "image embedder model resource failed integrity verification"
    }
    return IosImageEmbeddingModel(path = path, sha256 = digest)
}
