package com.micrantha.eyespie.features.scan.usecase

import MediaPipeTasksVision.MPPBaseOptions
import MediaPipeTasksVision.MPPEmbedding
import MediaPipeTasksVision.MPPImage
import MediaPipeTasksVision.MPPImageEmbedder
import MediaPipeTasksVision.MPPImageEmbedderOptions
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.PlatformCameraImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
class MediaPipeImageEmbeddingGenerator(
    private val modelPathProvider: () -> String = ::resolveImageEmbedderModelPath,
) : ImageEmbeddingGenerator {

    override suspend fun generate(image: CameraImage): Embedding = withContext(Dispatchers.Default) {
        autoreleasepool {
            val platformImage = image as? PlatformCameraImage
                ?: throw IllegalArgumentException("unsupported iOS camera image")

            // PlatformCameraImage owns its oriented BGRA bytes. Reuse its existing PNG boundary so
            // MediaPipe receives an owned UIImage rather than a borrowed camera CVPixelBuffer.
            val uiImage = platformImage.toByteArray().toUIImage()
            val mpImage = createMediaPipeImage(uiImage)
            val embedder = createImageEmbedder(modelPathProvider())
            val result = embedImage(embedder, mpImage)

            canonicalMediaPipeEmbedding(
                result.embeddingResult.embeddings.map { rawEmbedding ->
                    val embedding = rawEmbedding as? MPPEmbedding
                        ?: throw IllegalStateException("MediaPipe returned an invalid embedding head")
                    embedding.floatEmbedding?.map { rawValue ->
                        val number = rawValue as? NSNumber
                            ?: throw IllegalStateException("MediaPipe returned a non-numeric embedding value")
                        number.floatValue
                    }
                }
            )
        }
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

internal fun canonicalMediaPipeEmbedding(heads: List<List<Float>?>): Embedding {
    if (heads.size != 1) {
        throw IllegalStateException("expected exactly one image embedding head")
    }
    val values = heads.single()
    if (values.isNullOrEmpty()) {
        throw IllegalStateException("MediaPipe returned no float embedding")
    }
    return values.toCanonicalEmbedding()
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toUIImage(): UIImage {
    if (isEmpty()) {
        throw IllegalArgumentException("camera image produced no bytes")
    }
    val data: NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return UIImage.imageWithData(data)
        ?: throw IllegalStateException("MediaPipe image conversion failed")
}

private fun resolveImageEmbedderModelPath(): String {
    val fileName = ImageEmbeddingContract.androidModelAssetName
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
    val baseName = fileName.removeSuffix(if (extension.isEmpty()) "" else ".$extension")
    if (baseName.isEmpty() || extension.isEmpty()) {
        throw IllegalStateException("image embedder model resource name is invalid")
    }
    return NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
        ?: throw IllegalStateException("image embedder model resource is unavailable")
}

private fun mediaPipeFailure(stage: String, error: NSError?): Nothing {
    throw IllegalStateException(
        if (error == null) {
            "MediaPipe image embedding $stage failed"
        } else {
            "MediaPipe image embedding $stage failed (code=${error.code})"
        }
    )
}

actual fun platformImageEmbeddingGenerator(di: DI): ImageEmbeddingGenerator =
    MediaPipeImageEmbeddingGenerator()
