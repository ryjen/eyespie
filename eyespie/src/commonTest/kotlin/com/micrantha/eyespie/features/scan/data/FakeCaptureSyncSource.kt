package com.micrantha.eyespie.features.scan.data

import com.micrantha.eyespie.data.PendingCapture
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.features.scan.data.source.CaptureSyncSource
import okio.Path

class FakeCaptureSyncSource : CaptureSyncSource {
    val queued = mutableListOf<PendingCapture>()

    override suspend fun queue(proof: Proof, imagePath: Path, playerID: String): Result<Unit> {
        queued.add(PendingCapture(
            id = "pc${queued.size + 1}",
            image_path = imagePath.toString(),
            player_id = playerID,
            latitude = proof.location?.point?.latitude,
            longitude = proof.location?.point?.longitude,
            clues = null, // simplified for fake
            embedding = proof.embedding.toByteArray(),
            created_at = ""
        ))
        return Result.success(Unit)
    }

    override suspend fun getAll(): Result<List<PendingCapture>> = Result.success(queued)

    override suspend fun remove(id: String): Result<Unit> {
        queued.removeAll { it.id == id }
        return Result.success(Unit)
    }
}
