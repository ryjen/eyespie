package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import okio.Path

class FakeUploadCaptureUseCase : UploadCaptureUseCase {
    var result: Result<Thing> = Result.failure(Exception("Not initialized"))
    var invokedWith: Pair<Proof, Path>? = null

    override suspend fun invoke(proof: Proof, image: Path): Result<Thing> {
        invokedWith = proof to image
        return result
    }
}
