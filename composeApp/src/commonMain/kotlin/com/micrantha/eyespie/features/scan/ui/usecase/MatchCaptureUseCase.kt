package com.micrantha.eyespie.features.scan.ui.usecase

import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

class MatchCaptureUseCase(
) {
    suspend operator fun invoke(
        image: CameraImage,
        thing: Thing,
    ): Result<Boolean> =
        dispatchUseCase(coroutineContext) {
            // TODO: get embeddings from supabase or run remote function instead
            val match: Embedding = Embedding.EMPTY

            val embedding = Embedding.EMPTY

            val result = similarity(match.toByteArray(), embedding.toByteArray())

            Log.i("image match similarity: $result")

            // TODO: Game configurable
            result >= 0.70000000
        }

    private fun similarity(vectorA: ByteArray, vectorB: ByteArray): Double {
        require(vectorA.size == vectorB.size) { "Vector dimensions must be the same" }

        var dotProduct = 0L
        var normA = 0L
        var normB = 0L

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i].toLong() * vectorA[i].toLong()
            normB += vectorB[i].toLong() * vectorB[i].toLong()
        }

        val normProduct = sqrt(normA.toDouble()) * sqrt(normB.toDouble())

        return if (normProduct != 0.0) dotProduct.toDouble() / normProduct else 0.0

    }
}
