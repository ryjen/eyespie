package com.micrantha.eyespie.features.scan.data.source

import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.data.PendingCapture
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.Proof
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface CaptureSyncSource {
    suspend fun queue(proof: Proof, imagePath: Path, playerID: String): Result<Unit>
    suspend fun getAll(): Result<List<PendingCapture>>
    suspend fun remove(id: String): Result<Unit>
}

internal class SqlCaptureSyncSource(
    database: EyesPieDatabase,
    private val json: Json
) : CaptureSyncSource {
    private val queries = database.eyesPieQueries

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun queue(proof: Proof, imagePath: Path, playerID: String): Result<Unit> = try {
        queries.insertPendingCapture(
            id = Uuid.random().toString(),
            image_path = imagePath.toString(),
            player_id = playerID,
            latitude = proof.location?.point?.latitude,
            longitude = proof.location?.point?.longitude,
            clues = proof.clues?.let { json.encodeToString<AiProof>(it) },
            embedding = proof.embedding.toByteArray(),
            created_at = Clock.System.now().toString()
        )
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun getAll(): Result<List<PendingCapture>> = try {
        Result.success(queries.selectAllPendingCaptures().executeAsList())
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override suspend fun remove(id: String): Result<Unit> = try {
        queries.deletePendingCapture(id)
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
