package com.micrantha.eyespie.features.scan.usecase

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder.ImageEmbedderOptions
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.PlatformCameraImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

class MediaPipeImageEmbeddingGenerator(
    private val context: Context,
    private val modelAssetPath: String = ImageEmbeddingContract.androidModelAssetName,
) : ImageEmbeddingGenerator {

    private val imageEmbedder by lazy {
        val options = ImageEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelAssetPath).build())
            // The backend contract is float vector(1024). Do not request quantized bytes and then
            // attempt to recover them through reflection; both platforms must emit the same type.
            .setQuantize(false)
            .build()
        ImageEmbedder.createFromOptions(context, options)
    }

    override suspend fun generate(image: CameraImage): Embedding = withContext(Dispatchers.Default) {
        val platformImage = image as? PlatformCameraImage
            ?: throw IllegalArgumentException("unsupported Android camera image")
        val mpImage = platformImage.asMPImage()

        try {
            val result = imageEmbedder.embed(mpImage, platformImage.processingOptions)
            val embeddings = result.embeddingResult().embeddings()
            if (embeddings.size != 1) {
                throw IllegalStateException("expected exactly one image embedding head")
            }

            val floats = embeddings.first().floatEmbedding()
                ?: throw IllegalStateException("MediaPipe returned no float embedding")
            floats.toList().toCanonicalEmbedding()
        } finally {
            mpImage.close()
        }
    }
}

actual fun platformImageEmbeddingGenerator(di: DI): ImageEmbeddingGenerator =
    MediaPipeImageEmbeddingGenerator(di.direct.instance())
