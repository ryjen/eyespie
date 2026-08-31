package com.micrantha.eyespie.imaging

/**
 * Produces a small, display-only thumbnail from a captured target image.
 *
 * The thumbnail is a device-local UX cache. It is intentionally distinct from the
 * canonical [ImageEmbeddingGenerator] output, which is the sole source of matching
 * authority. The bytes produced here must never be treated as game authority or
 * serialized into a portable bundle.
 *
 * Implementations decode the [CapturedImage] bytes, downscale to at most
 * [maxDimension] on the longest edge, and re-encode as a compressed format.
 */
interface ThumbnailCodec {
    fun encode(image: CapturedImage, maxDimension: Int = 256): ByteArray?
}
