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
<<<<<<< Updated upstream
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
||||||| Stash base
import okio.ByteString
=======
import com.micrantha.eyespie.platform.scan.PlatformCameraImage
import okio.ByteString
import okio.ByteString.Companion.toByteString
>>>>>>> Stashed changes
import org.kodein.di.DI
<<<<<<< Updated upstream
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.UIKit.UIImage
||||||| Stash base
=======
import cocoapods.MediaPipeTasksVision.MPPImageEmbedder
import cocoapods.MediaPipeTasksVision.MPPImageEmbedderOptions
import cocoapods.MediaPipeTasksVision.MPPImage
import platform.Foundation.NSBundle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
>>>>>>> Stashed changes

<<<<<<< Updated upstream
@OptIn(ExperimentalForeignApi::class)
class MediaPipeImageEmbeddingGenerator(
    private val modelPathProvider: () -> String = ::resolveImageEmbedderModelPath,
) : ImageEmbeddingGenerator {
||||||| Stash base
class MediaPipeImageEmbeddingGenerator : ImageEmbeddingGenerator {
=======
@OptIn(ExperimentalForeignApi::class)
class MediaPipeImageEmbeddingGenerator(
    private val modelAssetPath: String = "mobilenet_v3_large.tflite"
) : ImageEmbeddingGenerator {
>>>>>>> Stashed changes

<<<<<<< Updated upstream
    override suspend fun generate(image: CameraImage): Embedding = withContext(Dispatchers.Default) {
        autoreleasepool {
            // CameraImage is the public boundary. Production PlatformCameraImage supplies an owned PNG;
            // calibration fixtures supply immutable encoded bytes through the same contract.
            val uiImage = image.toByteArray().toUIImage()
            val mpImage = createMediaPipeImage(uiImage)
            val embedder = createImageEmbedder(modelPathProvider())
            val result = embedImage(embedder, mpImage)
||||||| Stash base
    override val version: String = "deterministic-v1"
=======
    override val version: String = "mobilenet_v3_large"
>>>>>>> Stashed changes

<<<<<<< Updated upstream
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
||||||| Stash base
    override suspend fun generate(image: CameraImage): Embedding = 
        DeterministicImageEmbeddingGenerator().generate(image)
=======
    private var imageEmbedder: MPPImageEmbedder? = null
>>>>>>> Stashed changes

<<<<<<< Updated upstream
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
||||||| Stash base
    override fun close() = Unit
=======
    private fun getOrCreateModel(): MPPImageEmbedder {
        imageEmbedder?.let { return it }

        val mainBundle = NSBundle.mainBundle
        val path = mainBundle.pathForResource(modelAssetPath.substringBeforeLast("."), modelAssetPath.substringAfterLast("."))
            ?: throw IllegalStateException("Model not found in bundle: $modelAssetPath")

        val options = MPPImageEmbedderOptions()
        options.baseOptions.modelAssetPath = path
        
        // Quantize is not a direct property on iOS options in the same way, 
        // it depends on the model itself.

        imageEmbedder = MPPImageEmbedder.imageEmbedderWithOptions(options, null)
            ?: throw IllegalStateException("Failed to create ImageEmbedder")
            
        return imageEmbedder!!
    }

    override suspend fun generate(image: CameraImage): Embedding {
        val platformImage = image as PlatformCameraImage
        val cgImage = platformImage.asCGImage() ?: throw IllegalStateException("Failed to get CGImage")
        
        val mppImage = MPPImage(cgImage = cgImage, orientation = platformImage.orientation)

        val result = getOrCreateModel().embedImage(mppImage, null)
            ?: throw IllegalStateException("Embedding failed")

        val embedding = result.embeddingResult.embeddings.firstOrNull() as? cocoapods.MediaPipeTasksVision.MPPEmbedding
            ?: throw IllegalStateException("No embedding found")

        val floats = embedding.floatEmbedding
        if (floats != null) {
            val count = floats.size.toInt()
            val byteArray = ByteArray(count * 4)
            for (i in 0 until count) {
                val f = floats[i] as Float
                val bits = f.toBits()
                byteArray[i * 4] = (bits shr 24).toByte()
                byteArray[i * 4 + 1] = (bits shr 16).toByte()
                byteArray[i * 4 + 2] = (bits shr 8).toByte()
                byteArray[i * 4 + 3] = bits.toByte()
            }
            return byteArray.toByteString()
        }

        throw IllegalStateException("No float embedding found")
    }

    override fun close() {
        imageEmbedder = null
    }
>>>>>>> Stashed changes
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
