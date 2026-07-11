package com.micrantha.eyespie.features.scan.usecase

import android.content.Context
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder.ImageEmbedderOptions
import com.google.mediapipe.tasks.core.BaseOptions
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.PlatformCameraImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

class MediaPipeImageEmbeddingGenerator(
    private val context: Context,
    private val modelAssetPath: String = "mobilenet_v3_small_100_224_embedder.tflite",
) : ImageEmbeddingGenerator {

    private val imageEmbedder by lazy {
        val options = ImageEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelAssetPath).build())
            .setQuantize(true)
            .build()
        ImageEmbedder.createFromOptions(context, options)
    }

    override suspend fun generate(image: CameraImage): Embedding = withContext(Dispatchers.Default) {
        val platformImage = image as PlatformCameraImage
        val mpImage = platformImage.asMPImage()
        
        val result = imageEmbedder.embed(mpImage, platformImage.processingOptions)
        
        val embedding = result.embeddingResult().embeddings().first()
        
        val floats = embedding.floatEmbedding()
        if (floats != null) {
            val bytes = ByteArray(floats.size * 4)
            floats.forEachIndexed { i, f ->
                val bits = f.toBits()
                bytes[i * 4] = (bits shr 24).toByte()
                bytes[i * 4 + 1] = (bits shr 16).toByte()
                bytes[i * 4 + 2] = (bits shr 8).toByte()
                bytes[i * 4 + 3] = bits.toByte()
            }
            return@withContext bytes.toByteString()
        }
        
        // Try byte embedding if floats are null (e.g. if quantized)
        try {
            val bytes = embedding.javaClass.getMethod("getByteEmbedding").invoke(embedding) as? ByteArray
            if (bytes != null) {
                return@withContext bytes.toByteString()
            }
        } catch (_: Throwable) {
        }

        throw IllegalStateException("No embedding found in result")
    }
}

actual fun platformImageEmbeddingGenerator(di: DI): ImageEmbeddingGenerator = 
    MediaPipeImageEmbeddingGenerator(di.direct.instance())
