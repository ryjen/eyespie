@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.imaging

import MediaPipeTasksVision.MPPBaseOptions
import MediaPipeTasksVision.MPPEmbedding
import MediaPipeTasksVision.MPPImage
import MediaPipeTasksVision.MPPImageEmbedder
import MediaPipeTasksVision.MPPImageEmbedderOptions
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.UIKit.UIImage

class MediaPipeImageEmbeddingGenerator(
    private val modelPathProvider: () -> String = ::resolveImageEmbedderModelPath,
) : ImageEmbeddingGenerator {
    override suspend fun generate(image: CapturedImage): List<Float> = autoreleasepool {
        val uiImage = image.encodedBytes().toUIImage()
        val mpImage = createMediaPipeImage(uiImage)
        val embedder = createImageEmbedder(modelPathProvider())
        val result = embedImage(embedder, mpImage)

        val heads = result.embeddingResult.embeddings.map { rawEmbedding ->
            val embedding = rawEmbedding as? MPPEmbedding
                ?: throw IllegalStateException("MediaPipe returned an invalid embedding head")
            embedding.floatEmbedding?.map { rawValue ->
                val number = rawValue as? NSNumber
                    ?: throw IllegalStateException("MediaPipe returned a non-numeric embedding value")
                number.floatValue
            }
        }
        if (heads.size != 1) {
            throw IllegalStateException("expected exactly one image embedding head")
        }
        val values = heads.single()
        if (values.isNullOrEmpty()) {
            throw IllegalStateException("MediaPipe returned no float image embedding")
        }
        canonicalImageEmbedding(values)
    }

    private fun createImageEmbedder(modelPath: String): MPPImageEmbedder = memScoped {
        val options = MPPImageEmbedderOptions().apply {
            baseOptions = MPPBaseOptions().apply {
                modelAssetPath = modelPath
            }
            quantize = false
            l2Normalize = false
        }
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        MPPImageEmbedder(options = options, error = error.ptr)
            ?: mediaPipeFailure("initialization", error.value)
    }

    private fun createMediaPipeImage(image: UIImage): MPPImage = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        MPPImage(uIImage = image, error = error.ptr)
            ?: mediaPipeFailure("image conversion", error.value)
    }

    private fun embedImage(embedder: MPPImageEmbedder, image: MPPImage) = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        embedder.embedImage(image = image, error = error.ptr)
            ?: mediaPipeFailure("inference", error.value)
    }
}

private fun ByteArray.toUIImage(): UIImage {
    val data: NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return UIImage.imageWithData(data)
        ?: throw IllegalArgumentException("captured image bytes are not a supported iOS image")
}

private fun resolveImageEmbedderModelPath(): String {
    val extension = IMAGE_EMBEDDER_MODEL_FILE.substringAfterLast('.')
    val baseName = IMAGE_EMBEDDER_MODEL_FILE.removeSuffix(".$extension")
    return NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
        ?: throw IllegalStateException("image embedder model resource is unavailable")
}

private fun mediaPipeFailure(stage: String, error: NSError?): Nothing {
    throw IllegalStateException(
        if (error == null) {
            "MediaPipe image embedding $stage failed"
        } else {
            "MediaPipe image embedding $stage failed (code=${error.code})"
        },
    )
}
