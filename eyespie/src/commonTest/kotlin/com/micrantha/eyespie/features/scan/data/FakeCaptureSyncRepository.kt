package com.micrantha.eyespie.features.scan.data

import com.micrantha.eyespie.domain.entities.Proof
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path

class FakeCaptureSyncRepository : CaptureSyncRepository {
    var queueResult: Result<Unit> = Result.success(Unit)
    var queuedCalledWith: Triple<Proof, Path, String>? = null

    val countFlow = MutableStateFlow(0)
    override val pendingCount: Flow<Int> = countFlow

    override suspend fun queue(proof: Proof, imagePath: Path, playerID: String): Result<Unit> {
        queuedCalledWith = Triple(proof, imagePath, playerID)
        return queueResult
    }
}
