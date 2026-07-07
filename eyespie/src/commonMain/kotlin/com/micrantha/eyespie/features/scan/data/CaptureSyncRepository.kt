package com.micrantha.eyespie.features.scan.data

import com.micrantha.bluebell.platform.ConnectivityStatus
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.features.scan.data.source.CaptureSyncSource
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

interface CaptureSyncRepository {
    suspend fun queue(proof: Proof, imagePath: okio.Path, playerID: String): Result<Unit>
}

class CaptureSyncRepositoryImpl(
    private val source: CaptureSyncSource,
    private val uploadUseCase: UploadCaptureUseCase,
    private val connectivity: ConnectivityStatus,
    private val json: Json,
    private val scope: CoroutineScope
) : CaptureSyncRepository {
    init {
        connectivity.connected
            .onEach { isConnected ->
                if (isConnected) {
                    sync()
                }
            }.launchIn(scope)
    }

    override suspend fun queue(proof: Proof, imagePath: okio.Path, playerID: String): Result<Unit> {
        return source.queue(proof, imagePath, playerID).onSuccess {
            if (connectivity.isConnected) {
                sync()
            }
        }
    }

    private suspend fun sync() {
        source.getAll().onSuccess { pending ->
            pending.forEach { item ->
                val proof = Proof(
                    clues = item.clues?.let { json.decodeFromString<AiProof>(it) },
                    location = if (item.latitude != null && item.longitude != null) {
                        Location(point = Location.Point(item.latitude, item.longitude))
                    } else null
                )
                uploadUseCase(proof, item.image_path.toPath()).onSuccess {
                    source.remove(item.id)
                }
            }
        }
    }
}
