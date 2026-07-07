package com.micrantha.eyespie.features.scan.data

import com.micrantha.eyespie.domain.entities.Proof
import okio.Path

class FakeCaptureSyncRepository : CaptureSyncRepository {
    var queueResult: Result<Unit> = Result.success(Unit)
    var queuedCalledWith: Triple<Proof, Path, String>? = null

    override suspend fun queue(proof: Proof, imagePath: Path, playerID: String): Result<Unit> {
        queuedCalledWith = Triple(proof, imagePath, playerID)
        return queueResult
    }
}
