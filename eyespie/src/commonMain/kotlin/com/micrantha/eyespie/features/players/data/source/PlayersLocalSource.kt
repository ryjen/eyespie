package com.micrantha.eyespie.features.players.data.source

import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.eyespie.features.players.data.model.PlayerResponse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal open class PlayersLocalSource(
    private val fileSystem: FileSystem,
    private val json: Json
) {
    private val playersPath = fileSystem.filesPath().resolve("players.json")

    fun getAll(): Result<List<PlayerResponse>> = try {
        val data = fileSystem.fileRead(playersPath)
        Result.success(json.decodeFromString(ListSerializer(PlayerResponse.serializer()), data.decodeToString()))
    } catch (err: Throwable) {
        Result.failure(err)
    }

    fun saveAll(players: List<PlayerResponse>): Result<Unit> = try {
        val data = json.encodeToString(ListSerializer(PlayerResponse.serializer()), players).encodeToByteArray()
        fileSystem.fileWrite(playersPath, data)
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
