package com.micrantha.eyespie.features.scan.usecase

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder.ImageEmbedderOptions
import com.micrantha.eyespie.domain.entities.ALPHA_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.EmbeddingMetadata
import com.micrantha.eyespie.domain.entities.EmbeddingNormalization
import com.micrantha.eyespie.domain.entities.EmbeddingSimilarity
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.PlatformCameraImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

class MediaPipeImageEmbeddingGenerator(
    private val context: Context,
    private val modelAssetPath: String = "mobilenet_v3_small_100_224_embedder.tflite",
) : ImageEmbeddingGenerator {

    private val metadata = EmbeddingMetadata(
        modelId = "asset:$modelAssetPath",
        dimensions = ALPHA_EMBEDDING_DIMENSIONS,
        normalization = EmbeddingNormalization.ModelDefined,
        similarity = EmbeddingSimilarity.Cosine,
    )

    override suspend fun generate(image: CameraImage): Embedding = withContext(Dispatchers.Default) {
        val platformImage = image as? PlatformCameraImage
            ?: throw IllegalArgumentException("unsupported camera image for Android embedding")
        val mpImage = platformImage.asMPImage()
        val imageEmbedder = createImageEmbedder()

        try {
            val result = imageEmbedder.embed(mpImage, platformImage.processingOptions)
            val heads = result.embeddingResult().embeddings()
            require(heads.size == 1) { "expected exactly one image embedding head" }

            val floats = heads.single().floatEmbedding()
                ?: throw IllegalStateException("MediaPipe returned no float embedding")
            require(floats.size == ALPHA_EMBEDDING_DIMENSIONS) {
                "unexpected image embedding dimensions: ${floats.size}"
            }

            Embedding.of(metadata, floats)
        } finally {
            mpImage.close()
            imageEmbedder.close()
        }
    }

    private fun createImageEmbedder(): ImageEmbedder {
        val options = ImageEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelAssetPath).build())
            .setQuantize(false)
            .build()
        return ImageEmbedder.createFromOptions(context, options)
    }
}

actual fun platformImageEmbeddingGenerator(di: DI): ImageEmbeddingGenerator =
    MediaPipeImageEmbeddingGenerator(di.direct.instance())
