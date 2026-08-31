package com.micrantha.eyespie.imaging

/**
 * Rotates a captured image by a multiple of 90 degrees, returning a new
 * [CapturedImage] in the same encoded form. Used to make clue checking
 * tolerant of the angle at which a player photographs the target: the guess is
 * embedded at each canonical rotation and compared against the upright target.
 *
 * Implementations perform lossy re-encode and may return null for unreadable
 * input, in which case callers fall back to the unrotated embedding.
 */
fun interface ImageRotator {
    fun rotate(image: CapturedImage, degrees: Int): CapturedImage?
}

/** Canonical rotations (clockwise degrees) tried when matching a guess. */
val MATCH_ROTATIONS: List<Int> = listOf(0, 90, 180, 270)
