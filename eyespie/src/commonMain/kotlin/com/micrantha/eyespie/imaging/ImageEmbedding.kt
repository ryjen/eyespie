package com.micrantha.eyespie.imaging

/**
 * Encoded camera/image bytes crossing the common boundary.
 *
 * Platform image objects deliberately do not escape into common code. The byte array is copied on
 * input/output so short-lived camera buffers cannot be mutated after capture.
 */
class CapturedImage private constructor(
    private val encoded: ByteArray,
) {
    val byteSize: Int get() = encoded.size

    fun encodedBytes(): ByteArray = encoded.copyOf()

    companion object {
        fun fromEncoded(bytes: ByteArray): CapturedImage {
            require(bytes.isNotEmpty()) { "captured image must not be empty" }
            return CapturedImage(bytes.copyOf())
        }
    }
}

/** Optional platform capture capability. UI and lifecycle ownership remain platform-specific. */
interface ImageCapture {
    suspend fun capture(): CapturedImage
}

/** Generates the canonical 1024-float target/guess representation entirely on-device. */
interface ImageEmbeddingGenerator {
    suspend fun generate(image: CapturedImage): List<Float>
}

fun canonicalImageEmbedding(values: List<Float>): List<Float> {
    require(values.size == IMAGE_EMBEDDING_DIMENSIONS) {
        "image embedding must contain exactly $IMAGE_EMBEDDING_DIMENSIONS floats"
    }
    require(values.all(Float::isFinite)) { "image embedding must contain only finite values" }
    return values.toList()
}

const val IMAGE_EMBEDDING_DIMENSIONS = 1024
const val IMAGE_EMBEDDER_MODEL_FILE = "mobilenet_v3_small_100_224_embedder.tflite"
