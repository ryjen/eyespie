package com.micrantha.eyespie.imaging

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder

class MediaPipeImageEmbeddingGenerator(
    context: Context,
    modelAssetPath: String = IMAGE_EMBEDDER_MODEL_FILE,
) : ImageEmbeddingGenerator {
    private val embedder: ImageEmbedder by lazy {
        val options = ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(modelAssetPath)
                    .build(),
            )
            .setQuantize(false)
            .build()
        ImageEmbedder.createFromOptions(context.applicationContext, options)
    }

    override suspend fun generate(image: CapturedImage): List<Float> {
        val encoded = image.encodedBytes()
        val bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
            ?: throw IllegalArgumentException("captured image bytes are not a supported Android image")
        val mpImage = BitmapImageBuilder(bitmap).build()

        return try {
            val embeddings = embedder.embed(mpImage).embeddingResult().embeddings()
            if (embeddings.size != 1) {
                throw IllegalStateException("expected exactly one image embedding head")
            }
            val floats = embeddings.single().floatEmbedding()
                ?: throw IllegalStateException("MediaPipe returned no float image embedding")
            canonicalImageEmbedding(floats.toList())
        } finally {
            mpImage.close()
            bitmap.recycle()
        }
    }
}
